package com.kuky.backend.resources.repository;

import com.kuky.backend.resources.model.Purchase;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class PurchaseRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // Sequence counter for receipt references (per-process; resets on restart, unique via DB constraint)
    private final AtomicLong receiptSeq = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    public PurchaseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Purchase> PURCHASE_MAPPER = (rs, rowNum) -> {
        Purchase p = new Purchase();
        p.setId(rs.getObject("id", UUID.class));
        p.setUserId(rs.getObject("user_id", UUID.class));
        p.setItemType(Purchase.ItemType.valueOf(rs.getString("item_type")));
        p.setResourceId(rs.getObject("resource_id", UUID.class));
        p.setBundleId(rs.getObject("bundle_id", UUID.class));
        p.setAmountCents(rs.getInt("amount_cents"));
        p.setCurrency(rs.getString("currency"));
        p.setReceiptReference(rs.getString("receipt_reference"));
        p.setPaymentProvider(rs.getString("payment_provider"));
        p.setPaymentReference(rs.getString("payment_reference"));
        Timestamp ts = rs.getTimestamp("purchased_at");
        if (ts != null) p.setPurchasedAt(ts.toInstant());
        return p;
    };

    public Purchase insertPurchase(Purchase purchase) {
        UUID id = UUID.randomUUID();
        String receiptRef = generateReceiptReference();
        Instant now = Instant.now();

        String sql = """
                INSERT INTO purchases (id, user_id, item_type, resource_id, bundle_id,
                    amount_cents, currency, receipt_reference, payment_provider, payment_reference, purchased_at)
                VALUES (:id, :userId, :itemType, :resourceId, :bundleId,
                    :amountCents, :currency, :receiptReference, :paymentProvider, :paymentReference, :purchasedAt)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", purchase.getUserId())
                .addValue("itemType", purchase.getItemType().name())
                .addValue("resourceId", purchase.getResourceId())
                .addValue("bundleId", purchase.getBundleId())
                .addValue("amountCents", purchase.getAmountCents())
                .addValue("currency", purchase.getCurrency() != null ? purchase.getCurrency() : "EUR")
                .addValue("receiptReference", receiptRef)
                .addValue("paymentProvider", purchase.getPaymentProvider())
                .addValue("paymentReference", purchase.getPaymentReference())
                .addValue("purchasedAt", Timestamp.from(now));

        jdbc.update(sql, params);
        purchase.setId(id);
        purchase.setReceiptReference(receiptRef);
        purchase.setPurchasedAt(now);
        return purchase;
    }

    public void insertEntitlements(UUID userId, List<UUID> resourceIds, UUID purchaseId) {
        if (resourceIds.isEmpty()) return;
        String sql = """
                INSERT INTO entitlements (id, user_id, resource_id, source_purchase_id)
                VALUES (:id, :userId, :resourceId, :purchaseId)
                ON CONFLICT (user_id, resource_id) DO NOTHING
                """;
        for (UUID resourceId : resourceIds) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("userId", userId)
                    .addValue("resourceId", resourceId)
                    .addValue("purchaseId", purchaseId);
            jdbc.update(sql, params);
        }
    }

    public List<Purchase> findByUserId(UUID userId) {
        String sql = "SELECT * FROM purchases WHERE user_id = :userId ORDER BY purchased_at DESC";
        return jdbc.query(sql, Map.of("userId", userId), PURCHASE_MAPPER);
    }

    public Optional<Purchase> findByIdAndUserId(UUID id, UUID userId) {
        String sql = "SELECT * FROM purchases WHERE id = :id AND user_id = :userId";
        List<Purchase> results = jdbc.query(sql, Map.of("id", id, "userId", userId), PURCHASE_MAPPER);
        return results.stream().findFirst();
    }

    public List<Purchase> findByUserIdWithTitles(UUID userId) {
        String sql = """
                SELECT p.*,
                    COALESCE(r.slug, b.slug) AS item_slug,
                    COALESCE(r.title, b.title) AS item_title
                FROM purchases p
                LEFT JOIN resources r ON p.resource_id = r.id
                LEFT JOIN bundles b ON p.bundle_id = b.id
                WHERE p.user_id = :userId
                ORDER BY p.purchased_at DESC
                """;
        return jdbc.query(sql, Map.of("userId", userId), (rs, rowNum) -> {
            Purchase p = PURCHASE_MAPPER.mapRow(rs, rowNum);
            p.setGrantedResourceSlugs(List.of(rs.getString("item_slug") != null ? rs.getString("item_slug") : ""));
            return p;
        });
    }

    private String generateReceiptReference() {
        String year = String.valueOf(java.time.Year.now().getValue());
        long seq = receiptSeq.incrementAndGet();
        return String.format("REC-%s-%06d", year, seq % 1_000_000);
    }
}
