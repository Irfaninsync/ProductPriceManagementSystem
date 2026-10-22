package com.ppms.repository;

import com.ppms.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface defining catalog persistence operations.
 * Decouples domain and service logic from underlying CSV storage details.
 */
public interface ProductRepository {

    /**
     * Persists or updates a product in storage.
     *
     * @param product the entity to save
     * @return the saved entity
     */
    Product save(Product product);

    /**
     * Finds a product by its unique product ID (case-insensitive).
     */
    Optional<Product> findById(String productId);

    /**
     * Checks if a product exists with the given ID.
     */
    boolean existsById(String productId);

    /**
     * Returns all registered products (both active and archived).
     */
    List<Product> findAll();

    /**
     * Returns all active products.
     */
    List<Product> findActive();

    /**
     * Returns all archived products.
     */
    List<Product> findArchived();

    /**
     * Returns total count of all products.
     */
    int count();

    /**
     * Forces a fresh reload from the underlying storage.
     */
    void reload();
}
