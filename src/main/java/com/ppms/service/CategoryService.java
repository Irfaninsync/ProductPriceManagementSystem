// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.model.Category;
import com.ppms.model.CategorySummary;
import com.ppms.model.Product;
import com.ppms.repository.ActivityRepository;
import com.ppms.repository.CategoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.repository.TransactionManager;
import com.ppms.util.IdGenerator;
import com.ppms.validation.CategoryValidator;
import com.ppms.validation.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing category lifecycle, referential integrity, and aggregated category analytics.
 */
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final IdGenerator categoryIdGenerator;
    private final TransactionManager transactionManager;
    private final ActivityRepository activityRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           IdGenerator categoryIdGenerator,
                           TransactionManager transactionManager,
                           ActivityRepository activityRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "CategoryRepository cannot be null");
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.categoryIdGenerator = Objects.requireNonNull(categoryIdGenerator, "Category IdGenerator cannot be null");
        this.transactionManager = transactionManager != null ? transactionManager : TransactionManager.direct();
        this.activityRepository = activityRepository;
    }

    public Category createCategory(String name, String description) {
        String cleanName = name != null ? name.trim() : "";
        ValidationResult validation = new ValidationResult();
        CategoryValidator.validateName(cleanName, validation);
        CategoryValidator.validateDescription(description, validation);

        if (validation.isValid() && categoryRepository.existsByNameIgnoreCase(cleanName)) {
            validation.addError("A category named '" + cleanName + "' already exists.");
        }
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getFormattedErrors());
        }

        return transactionManager.inTransaction(() -> {
            Category category = new Category(categoryIdGenerator.nextId(), cleanName, description);
            Category saved = categoryRepository.save(category);
            if (activityRepository != null) {
                activityRepository.record("CATEGORY_CREATED", "CATEGORY", saved.getCategoryId(),
                        "Created category '" + saved.getName() + "'");
            }
            return saved;
        });
    }

    public Category updateCategory(String categoryId, String newName, String newDescription) {
        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        String cleanName = newName != null ? newName.trim() : "";
        ValidationResult validation = new ValidationResult();
        CategoryValidator.validateName(cleanName, validation);
        CategoryValidator.validateDescription(newDescription, validation);

        Optional<Category> duplicate = categoryRepository.findByNameIgnoreCase(cleanName);
        if (duplicate.isPresent() && !duplicate.get().getCategoryId().equalsIgnoreCase(existing.getCategoryId())) {
            validation.addError("Another category already uses the name '" + cleanName + "'.");
        }
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getFormattedErrors());
        }

        String oldName = existing.getName();
        return transactionManager.inTransaction(() -> {
            Category draft = new Category(existing);
            draft.updateDetails(cleanName, newDescription);
            Category saved = categoryRepository.save(draft);
            if (activityRepository != null) {
                String summary = oldName.equals(saved.getName())
                        ? "Updated category '" + saved.getName() + "'"
                        : "Renamed category '" + oldName + "' to '" + saved.getName() + "'";
                activityRepository.record("CATEGORY_UPDATED", "CATEGORY", saved.getCategoryId(), summary);
            }
            return saved;
        });
    }

    /**
     * Deletes a category. If products reference it and targetCategoryId is provided,
     * reassigns those products atomically before deleting; otherwise blocks deletion.
     */
    public void deleteCategory(String categoryId, String reassignTargetCategoryId) {
        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        int attachedProducts = categoryRepository.countProductsInCategory(existing.getCategoryId());
        if (attachedProducts > 0) {
            if (reassignTargetCategoryId == null || reassignTargetCategoryId.trim().isEmpty()) {
                throw new IllegalStateException("Cannot delete category '" + existing.getName()
                        + "' because " + attachedProducts + " product(s) are assigned to it. Reassign them first.");
            }
            Category target = categoryRepository.findById(reassignTargetCategoryId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Replacement category not found: " + reassignTargetCategoryId));
            if (target.getCategoryId().equalsIgnoreCase(existing.getCategoryId())) {
                throw new IllegalArgumentException("Replacement category must be different from the deleted category.");
            }

            transactionManager.inTransactionVoid(() -> {
                int moved = categoryRepository.reassignProducts(existing.getCategoryId(), target.getCategoryId());
                categoryRepository.deleteById(existing.getCategoryId());
                if (activityRepository != null) {
                    activityRepository.record("CATEGORY_DELETED", "CATEGORY", existing.getCategoryId(),
                            "Deleted category '" + existing.getName() + "' and moved " + moved + " product(s) to '" + target.getName() + "'");
                }
            });
            return;
        }

        transactionManager.inTransactionVoid(() -> {
            categoryRepository.deleteById(existing.getCategoryId());
            if (activityRepository != null) {
                activityRepository.record("CATEGORY_DELETED", "CATEGORY", existing.getCategoryId(),
                        "Deleted empty category '" + existing.getName() + "'");
            }
        });
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId);
    }

    public List<CategorySummary> getCategorySummaries(String searchQuery) {
        List<Category> categories = categoryRepository.findAll();
        List<Product> allProducts = productRepository.findAll();

        Map<String, List<Product>> byCategoryName = allProducts.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().trim().toLowerCase(Locale.ROOT)));

        String cleanQuery = (searchQuery != null) ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";

        List<CategorySummary> summaries = new ArrayList<>();
        for (Category cat : categories) {
            if (!cleanQuery.isEmpty()) {
                boolean matches = cat.getName().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || cat.getCategoryId().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || (cat.getDescription() != null && cat.getDescription().toLowerCase(Locale.ROOT).contains(cleanQuery));
                if (!matches) {
                    continue;
                }
            }

            List<Product> group = byCategoryName.getOrDefault(cat.getName().trim().toLowerCase(Locale.ROOT), Collections.emptyList());
            List<Product> active = group.stream().filter(Product::isActive).toList();
            int archivedCount = group.size() - active.size();
            int totalUnits = active.stream().mapToInt(Product::getQuantity).sum();

            BigDecimal costSum = active.stream()
                    .map(Product::getInventoryCostValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellingSum = active.stream()
                    .map(Product::getInventorySellingValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal marginSum = sellingSum.subtract(costSum).setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgSelling = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (!active.isEmpty()) {
                BigDecimal unitPriceSum = active.stream()
                        .map(Product::getSellingPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                avgSelling = unitPriceSum.divide(BigDecimal.valueOf(active.size()), 2, RoundingMode.HALF_UP);
            }

            summaries.add(new CategorySummary(
                    cat.getCategoryId(),
                    cat.getName(),
                    cat.getDescription(),
                    group.size(),
                    active.size(),
                    archivedCount,
                    totalUnits,
                    costSum,
                    sellingSum,
                    marginSum,
                    avgSelling,
                    cat.getCreatedAt(),
                    cat.getUpdatedAt()
            ));
        }

        return Collections.unmodifiableList(summaries);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
