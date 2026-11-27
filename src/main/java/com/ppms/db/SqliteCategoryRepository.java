// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.Category;
import com.ppms.repository.CategoryRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite-backed CategoryRepository maintaining strict category uniqueness and product referential integrity.
 */
public class SqliteCategoryRepository implements CategoryRepository {

    private final SqliteDatabaseManager db;
    private final IdGenerator categoryIdGenerator;

    public SqliteCategoryRepository(SqliteDatabaseManager db, IdGenerator categoryIdGenerator) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
        this.categoryIdGenerator = Objects.requireNonNull(categoryIdGenerator, "Category IdGenerator cannot be null");
    }

    @Override
    public Category save(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");
        db.execute(conn -> {
            String sql = """
                    INSERT INTO categories (category_id, name, description, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(category_id) DO UPDATE SET
                        name = excluded.name,
                        description = excluded.description,
                        updated_at = excluded.updated_at
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, category.getCategoryId());
                ps.setString(2, category.getName());
                ps.setString(3, category.getDescription());
                ps.setString(4, DateTimeUtil.formatIso(category.getCreatedAt()));
                ps.setString(5, DateTimeUtil.formatIso(category.getUpdatedAt()));
                ps.executeUpdate();
            }
        });
        categoryIdGenerator.observeId(category.getCategoryId());
        return new Category(category);
    }

    /**
     * Resolves an existing category by case-insensitive name, or creates one atomically if absent.
     */
    public Category ensureCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        String clean = categoryName.trim();
        Optional<Category> existing = findByNameIgnoreCase(clean);
        if (existing.isPresent()) {
            return existing.get();
        }
        Category created = new Category(categoryIdGenerator.nextId(), clean, "");
        return save(created);
    }

    @Override
    public Optional<Category> findById(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return Optional.empty();
        }
        return db.query(conn -> {
            String sql = "SELECT category_id, name, description, created_at, updated_at FROM categories WHERE LOWER(category_id) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, categoryId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return db.query(conn -> {
            String sql = "SELECT category_id, name, description, created_at, updated_at FROM categories WHERE LOWER(name) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    @Override
    public boolean existsById(String categoryId) {
        return findById(categoryId).isPresent();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return findByNameIgnoreCase(name).isPresent();
    }

    @Override
    public List<Category> findAll() {
        return db.query(conn -> {
            List<Category> list = new ArrayList<>();
            String sql = "SELECT category_id, name, description, created_at, updated_at FROM categories ORDER BY name COLLATE NOCASE ASC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return Collections.unmodifiableList(list);
        });
    }

    @Override
    public int countProductsInCategory(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return 0;
        }
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE LOWER(category_id) = LOWER(?)")) {
                ps.setString(1, categoryId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public int reassignProducts(String sourceCategoryId, String targetCategoryId) {
        return db.query(conn -> {
            String now = DateTimeUtil.formatIso(LocalDateTime.now());
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET category_id = ?, updated_at = ? WHERE LOWER(category_id) = LOWER(?)")) {
                ps.setString(1, targetCategoryId);
                ps.setString(2, now);
                ps.setString(3, sourceCategoryId);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public boolean deleteById(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return false;
        }
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE LOWER(category_id) = LOWER(?)")) {
                ps.setString(1, categoryId.trim());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public int count() {
        return db.query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    @Override
    public void reload() {
        db.syncIdGenerators(null, null, categoryIdGenerator);
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
                rs.getString("category_id"),
                rs.getString("name"),
                rs.getString("description"),
                DateTimeUtil.parseIso(rs.getString("created_at")),
                DateTimeUtil.parseIso(rs.getString("updated_at"))
        );
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
