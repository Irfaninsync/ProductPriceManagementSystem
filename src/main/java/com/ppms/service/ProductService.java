// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.ProductRepository;
import com.ppms.util.IdGenerator;
import com.ppms.validation.ProductValidator;
import com.ppms.validation.ValidationResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing product catalog lifecycle, validation, updates, search, and sorting.
 */
public class ProductService {

    private final ProductRepository productRepository;
    private final IdGenerator productIdGenerator;

    public ProductService(ProductRepository productRepository, IdGenerator productIdGenerator) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.productIdGenerator = Objects.requireNonNull(productIdGenerator, "IdGenerator cannot be null");
    }

    /**
     * Creates and persists a new product with an automatically generated unique ID.
     */
    public Product addProduct(String name,
                              String category,
                              String description,
                              BigDecimal costPrice,
                              BigDecimal sellingPrice,
                              int quantity) {
        String newId = productIdGenerator.nextId();
        return addProductWithId(newId, name, category, description, costPrice, sellingPrice, quantity);
    }

    /**
     * Creates and persists a new product with a specific user-provided ID.
     */
    public Product addProductWithId(String productId,
                                    String name,
                                    String category,
                                    String description,
                                    BigDecimal costPrice,
                                    BigDecimal sellingPrice,
                                    int quantity) {
        Product product = new Product(productId, name, category, description, costPrice, sellingPrice, quantity);
        ValidationResult validation = ProductValidator.validate(product);

        if (productRepository.existsById(product.getProductId())) {
            validation.addError("A product with ID '" + product.getProductId() + "' already exists.");
        }

        if (validation.hasErrors()) {
            throw new IllegalArgumentException("Cannot create product due to validation errors:\n"
                    + validation.getFormattedErrors());
        }

        return productRepository.save(product);
    }

    public Optional<Product> getProductById(String productId) {
        return productRepository.findById(productId);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findActive();
    }

    public List<Product> getArchivedProducts() {
        return productRepository.findArchived();
    }

    /**
     * Updates non-price product details (name, category, description).
     * Does not perform partial updates if validation fails.
     */
    public Product updateProductDetails(String productId, String newName, String newCategory, String newDescription) {
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Create defensive copy to test validation safely
        Product draft = new Product(existing);
        draft.updateDetails(newName, newCategory, newDescription);

        ValidationResult validation = ProductValidator.validate(draft);
        if (validation.hasErrors()) {
            throw new IllegalArgumentException("Cannot update product details:\n" + validation.getFormattedErrors());
        }

        return productRepository.save(draft);
    }

    /**
     * Updates cost price with validation.
     */
    public Product updateCostPrice(String productId, BigDecimal newCostPrice) {
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        ValidationResult validation = new ValidationResult();
        ProductValidator.validateCostPrice(newCostPrice, validation);
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getFormattedErrors());
        }

        Product draft = new Product(existing);
        draft.setCostPrice(newCostPrice);
        return productRepository.save(draft);
    }

    /**
     * Updates inventory quantity.
     */
    public Product updateQuantity(String productId, int newQuantity) {
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        ValidationResult validation = new ValidationResult();
        ProductValidator.validateQuantity(newQuantity, validation);
        if (validation.hasErrors()) {
            throw new IllegalArgumentException(validation.getFormattedErrors());
        }

        Product draft = new Product(existing);
        draft.setQuantity(newQuantity);
        return productRepository.save(draft);
    }

    /**
     * Archives a product (soft retirement).
     */
    public Product archiveProduct(String productId) {
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (existing.isArchived()) {
            throw new IllegalStateException("Product " + productId + " is already archived.");
        }

        Product draft = new Product(existing);
        draft.setStatus(ProductStatus.ARCHIVED);
        return productRepository.save(draft);
    }

    /**
     * Restores an archived product to active status.
     */
    public Product restoreProduct(String productId) {
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (existing.isActive()) {
            throw new IllegalStateException("Product " + productId + " is already active.");
        }

        Product draft = new Product(existing);
        draft.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(draft);
    }

    /**
     * Searches products across ID, name, category, and description.
     */
    public List<Product> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getActiveProducts();
        }
        String clean = query.trim().toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(p -> p.getProductId().toLowerCase(Locale.ROOT).contains(clean)
                        || p.getName().toLowerCase(Locale.ROOT).contains(clean)
                        || p.getCategory().toLowerCase(Locale.ROOT).contains(clean)
                        || (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(clean)))
                .collect(Collectors.toList());
    }

    /**
     * Multi-criteria filtering.
     */
    public List<Product> filter(String category,
                                ProductStatus status,
                                BigDecimal minSellingPrice,
                                BigDecimal maxSellingPrice,
                                Integer minQuantity,
                                Integer maxQuantity,
                                Boolean belowCostOnly,
                                Boolean lowStockOnly,
                                int lowStockThreshold) {

        if (minSellingPrice != null && maxSellingPrice != null && minSellingPrice.compareTo(maxSellingPrice) > 0) {
            throw new IllegalArgumentException("Minimum price (" + minSellingPrice + ") cannot be greater than maximum price (" + maxSellingPrice + ").");
        }
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
            throw new IllegalArgumentException("Minimum quantity (" + minQuantity + ") cannot be greater than maximum quantity (" + maxQuantity + ").");
        }

        return productRepository.findAll().stream()
                .filter(p -> (category == null || category.trim().isEmpty() || p.getCategory().equalsIgnoreCase(category.trim())))
                .filter(p -> (status == null || p.getStatus() == status))
                .filter(p -> (minSellingPrice == null || p.getSellingPrice().compareTo(minSellingPrice) >= 0))
                .filter(p -> (maxSellingPrice == null || p.getSellingPrice().compareTo(maxSellingPrice) <= 0))
                .filter(p -> (minQuantity == null || p.getQuantity() >= minQuantity))
                .filter(p -> (maxQuantity == null || p.getQuantity() <= maxQuantity))
                .filter(p -> (belowCostOnly == null || !belowCostOnly || p.isBelowCost()))
                .filter(p -> (lowStockOnly == null || !lowStockOnly || p.isLowStock(lowStockThreshold)))
                .collect(Collectors.toList());
    }

    /**
     * Deterministic sorting by criteria.
     */
    public List<Product> sort(List<Product> products, String sortBy, boolean ascending) {
        if (products == null) return new ArrayList<>();
        List<Product> copy = new ArrayList<>(products);

        Comparator<Product> comparator;
        String field = sortBy != null ? sortBy.trim().toLowerCase(Locale.ROOT) : "name";

        switch (field) {
            case "id":
            case "productid":
                comparator = Comparator.comparing(Product::getProductId, String.CASE_INSENSITIVE_ORDER);
                break;
            case "category":
                comparator = Comparator.comparing(Product::getCategory, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "price":
            case "sellingprice":
                comparator = Comparator.comparing(Product::getSellingPrice)
                        .thenComparing(Product::getProductId);
                break;
            case "cost":
            case "costprice":
                comparator = Comparator.comparing(Product::getCostPrice)
                        .thenComparing(Product::getProductId);
                break;
            case "quantity":
            case "qty":
                comparator = Comparator.comparingInt(Product::getQuantity)
                        .thenComparing(Product::getProductId);
                break;
            case "updated":
            case "updatedat":
                comparator = Comparator.comparing(Product::getUpdatedAt)
                        .thenComparing(Product::getProductId);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Product::getProductId);
                break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        copy.sort(comparator);
        return copy;
    }

    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

