// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.Category;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.ProductRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite-backed implementation of ProductRepository.
 * Preserves exact Product interface semantics while linking to normalized categories table.
 */
public class SqliteProductRepository implements ProductRepository {

    private static final String BASE_SELECT = """
            SELECT p.product_id, p.name, c.name AS category_name, p.description,
                   p.cost_price_minor, p.selling_price_minor, p.quantity, p.status,
                   p.created_at, p.updated_at
            FROM products p
            INNER JOIN categories c ON p.category_id = c.category_id
            """;

    private final SqliteDatabaseManager db;
    private final SqliteCategoryRepository categoryRepository;
    private final IdGenerator productIdGenerator;

    public SqliteProductRepository(SqliteDatabaseManager db,
                                   SqliteCategoryRepository categoryRepository,
                                   IdGenerator productIdGenerator) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "CategoryRepository cannot be null");
        this.productIdGenerator = Objects.requireNonNull(productIdGenerator, "Product IdGenerator cannot be null");
        reload();
    }

    @Override
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Cannot save null product");
        }
        return db.inTransaction(() -> {
            Category category = categoryRepository.ensureCategory(product.getCategory());
            db.execute(conn -> {
                String sql = """
                        INSERT INTO products (
                            product_id, name, category_id, description,
                            cost_price_minor, selling_price_minor, quantity,
                            status, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(product_id) DO UPDATE SET
                            name = excluded.name,
                            category_id = excluded.category_id,
                            description = excluded.description,
                            cost_price_minor = excluded.cost_price_minor,
                            selling_price_minor = excluded.selling_price_minor,
                            quantity = excluded.quantity,
                            status = excluded.status,
                            updated_at = excluded.updated_at
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, product.getProductId());
                    ps.setString(2, product.getName());
                    ps.setString(3, category.getCategoryId());
                    ps.setString(4, product.getDescription() != null ? product.getDescription() : "");
                    ps.setLong(5, SqliteDatabaseManager.toMinorUnits(product.getCostPrice()));
                    ps.setLong(6, SqliteDatabaseManager.toMinorUnits(product.getSellingPrice()));
                    ps.setInt(7, product.getQuantity());
                    ps.setString(8, product.getStatus().name());
                    ps.setString(9, DateTimeUtil.formatIso(product.getCreatedAt()));
                    ps.setString(10, DateTimeUtil.formatIso(product.getUpdatedAt()));
                    ps.executeUpdate();
                }
            });
            productIdGenerator.observeId(product.getProductId());
            return findById(product.getProductId()).orElseGet(() -> new Product(product));
        });
    }

    @Override
    public Optional<Product> findById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Optional.empty();
        }
        return db.query(conn -> {
            String sql = BASE_SELECT + " WHERE LOWER(p.product_id) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productId.trim());
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
    public boolean existsById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM products WHERE LOWER(product_id) = LOWER(?)")) {
                ps.setString(1, productId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public List<Product> findAll() {
        return fetchList(BASE_SELECT + " ORDER BY p.created_at DESC, p.product_id ASC");
    }

    @Override
    public List<Product> findActive() {
        return fetchList(BASE_SELECT + " WHERE p.status = 'ACTIVE' ORDER BY p.created_at DESC, p.product_id ASC");
    }

    @Override
    public List<Product> findArchived() {
        return fetchList(BASE_SELECT + " WHERE p.status = 'ARCHIVED' ORDER BY p.updated_at DESC, p.product_id ASC");
    }

    @Override
    public int count() {
        return db.query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    @Override
    public boolean deleteById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE LOWER(product_id) = LOWER(?)")) {
                ps.setString(1, productId.trim());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public void reload() {
        db.syncIdGenerators(productIdGenerator, null, null);
    }

    private List<Product> fetchList(String sql) {
        return db.query(conn -> {
            List<Product> list = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return Collections.unmodifiableList(list);
        });
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("product_id"),
                rs.getString("name"),
                rs.getString("category_name"),
                rs.getString("description"),
                SqliteDatabaseManager.fromMinorUnits(rs.getLong("cost_price_minor")),
                SqliteDatabaseManager.fromMinorUnits(rs.getLong("selling_price_minor")),
                rs.getInt("quantity"),
                ProductStatus.fromString(rs.getString("status")),
                DateTimeUtil.parseIso(rs.getString("created_at")),
                DateTimeUtil.parseIso(rs.getString("updated_at"))
        );
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
