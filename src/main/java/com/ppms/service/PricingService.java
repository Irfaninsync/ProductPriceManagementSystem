// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.util.IdGenerator;
import com.ppms.validation.ProductValidator;
import com.ppms.validation.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centralized service governing all pricing modifications, previews, and historical auditing.
 * Guarantees that manual and bulk price updates undergo identical validation and audit trails.
 */
public class PricingService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IdGenerator historyIdGenerator;

    public PricingService(ProductRepository productRepository,
                          PriceHistoryRepository priceHistoryRepository,
                          IdGenerator historyIdGenerator) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.priceHistoryRepository = Objects.requireNonNull(priceHistoryRepository, "PriceHistoryRepository cannot be null");
        this.historyIdGenerator = Objects.requireNonNull(historyIdGenerator, "History IdGenerator cannot be null");
    }

    /**
     * Builds a proposed price change preview without altering any persistent or in-memory state.
     */
    public PriceProposal createProposal(String productId, BigDecimal proposedPrice, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        BigDecimal normalizedProposed = Product.normalizePrice(proposedPrice);
        ValidationResult validation = ProductValidator.validatePriceChange(product.getSellingPrice(), normalizedProposed, reason);

        return new PriceProposal(product, normalizedProposed, reason, validation);
    }

    /**
     * Executes an approved price proposal atomically across product and audit history.
     */
    public synchronized PriceHistory applyPriceChange(PriceProposal proposal) {
        Objects.requireNonNull(proposal, "Proposal cannot be null");
        if (!proposal.isValid()) {
            throw new IllegalArgumentException("Cannot apply invalid price proposal:\n"
                    + proposal.getValidationResult().getFormattedErrors());
        }

        Product currentProduct = productRepository.findById(proposal.getProduct().getProductId())
                .orElseThrow(() -> new IllegalStateException("Product has been removed: " + proposal.getProduct().getProductId()));

        BigDecimal oldPrice = currentProduct.getSellingPrice();
        BigDecimal newPrice = proposal.getProposedPrice();

        if (oldPrice.compareTo(newPrice) == 0) {
            throw new IllegalArgumentException("New price is identical to current price. No change recorded.");
        }

        // 1. Update product selling price
        Product updatedProduct = new Product(currentProduct);
        updatedProduct.setSellingPrice(newPrice);
        productRepository.save(updatedProduct);

        // 2. Generate permanent history record
        String historyId = historyIdGenerator.nextId();
        PriceHistory historyRecord = new PriceHistory(
                historyId,
                updatedProduct.getProductId(),
                oldPrice,
                newPrice,
                LocalDateTime.now(),
                proposal.getReason()
        );

        return priceHistoryRepository.save(historyRecord);
    }

    /**
     * Direct execution of a single price change with immediate validation and audit logging.
     */
    public PriceHistory changePrice(String productId, BigDecimal newPrice, String reason) {
        PriceProposal proposal = createProposal(productId, newPrice, reason);
        return applyPriceChange(proposal);
    }

    /**
     * Previews a bulk percentage price adjustment across target products.
     *
     * @param targetProducts products to evaluate
     * @param percentageChange positive for increase, negative for decrease
     * @param reason business reason
     * @return list of proposals for user preview
     */
    public List<PriceProposal> previewBulkAdjustment(List<Product> targetProducts,
                                                     BigDecimal percentageChange,
                                                     String reason) {
        List<PriceProposal> proposals = new ArrayList<>();
        if (targetProducts == null || percentageChange == null) {
            return proposals;
        }

        BigDecimal multiplier = BigDecimal.ONE.add(percentageChange.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        for (Product p : targetProducts) {
            BigDecimal proposed = p.getSellingPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            proposals.add(createProposal(p.getProductId(), proposed, reason));
        }

        return proposals;
    }

    /**
     * Applies a list of verified proposals in a bulk transaction.
     */
    public synchronized List<PriceHistory> applyBulkAdjustments(List<PriceProposal> proposals) {
        List<PriceHistory> executed = new ArrayList<>();
        for (PriceProposal proposal : proposals) {
            if (proposal.isValid()) {
                executed.add(applyPriceChange(proposal));
            }
        }
        return executed;
    }

    // --- Querying History ---

    public List<PriceHistory> getHistoryForProduct(String productId) {
        return priceHistoryRepository.findByProductId(productId);
    }

    public List<PriceHistory> getAllHistory() {
        return priceHistoryRepository.findAll();
    }

    public List<PriceHistory> getHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return priceHistoryRepository.findByDateRange(start, end);
    }

    public List<PriceHistory> getHistoryByReason(String reasonSubstring) {
        if (reasonSubstring == null || reasonSubstring.trim().isEmpty()) {
            return getAllHistory();
        }
        String clean = reasonSubstring.trim().toLowerCase(Locale.ROOT);
        return priceHistoryRepository.findAll().stream()
                .filter(h -> h.getReason().toLowerCase(Locale.ROOT).contains(clean))
                .collect(Collectors.toList());
    }

    /**
     * Finds the price history record representing the largest single price increase.
     */
    public Optional<PriceHistory> getLargestPriceIncrease() {
        return priceHistoryRepository.findAll().stream()
                .filter(PriceHistory::isPriceIncrease)
                .max(Comparator.comparing(PriceHistory::getPriceDifference));
    }

    /**
     * Finds the price history record representing the largest single price decrease.
     */
    public Optional<PriceHistory> getLargestPriceDecrease() {
        return priceHistoryRepository.findAll().stream()
                .filter(PriceHistory::isPriceDecrease)
                .min(Comparator.comparing(PriceHistory::getPriceDifference));
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

