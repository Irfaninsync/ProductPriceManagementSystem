// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a product category with unique ID and audit timestamps.
 */
public class Category implements Comparable<Category> {

    private final String categoryId;
    private String name;
    private String description;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Category(String categoryId,
                    String name,
                    String description,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.categoryId = Objects.requireNonNull(categoryId, "Category ID cannot be null").trim();
        this.name = Objects.requireNonNull(name, "Category name cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public Category(String categoryId, String name, String description) {
        this(categoryId, name, description, LocalDateTime.now(), LocalDateTime.now());
    }

    public Category(Category other) {
        Objects.requireNonNull(other, "Source category cannot be null");
        this.categoryId = other.categoryId;
        this.name = other.name;
        this.description = other.description;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String name, String description) {
        this.name = Objects.requireNonNull(name, "Category name cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return categoryId.equalsIgnoreCase(category.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId.toUpperCase());
    }

    @Override
    public int compareTo(Category other) {
        if (other == null) return 1;
        return this.name.compareToIgnoreCase(other.name);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
