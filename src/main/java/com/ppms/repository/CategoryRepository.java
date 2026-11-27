// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.repository;

import com.ppms.model.Category;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for category lifecycle persistence.
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(String categoryId);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsById(String categoryId);

    boolean existsByNameIgnoreCase(String name);

    List<Category> findAll();

    int countProductsInCategory(String categoryId);

    int reassignProducts(String sourceCategoryId, String targetCategoryId);

    boolean deleteById(String categoryId);

    int count();

    void reload();
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
