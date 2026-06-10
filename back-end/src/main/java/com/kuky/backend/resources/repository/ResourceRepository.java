package com.kuky.backend.resources.repository;

import com.kuky.backend.resources.model.Bundle;
import com.kuky.backend.resources.model.Resource;
import com.kuky.backend.resources.model.ResourceAsset;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ResourceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ResourceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Resource> RESOURCE_MAPPER = (rs, rowNum) -> {
        Resource r = new Resource();
        r.setId(rs.getObject("id", UUID.class));
        r.setSlug(rs.getString("slug"));
        r.setTitle(rs.getString("title"));
        r.setDescription(rs.getString("description"));
        r.setLevel(rs.getString("level"));
        r.setCategory(rs.getString("category"));
        r.setPricing(rs.getString("pricing"));
        int price = rs.getInt("price_cents");
        r.setPriceCents(rs.wasNull() ? null : price);
        r.setPreviewText(rs.getString("preview_text"));
        r.setRelatedResourceId(rs.getObject("related_resource_id", UUID.class));
        r.setPublished(rs.getBoolean("published"));
        r.setSortOrder(rs.getInt("sort_order"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toInstant());
        return r;
    };

    private static final RowMapper<ResourceAsset> ASSET_MAPPER = (rs, rowNum) -> {
        ResourceAsset a = new ResourceAsset();
        a.setId(rs.getObject("id", UUID.class));
        a.setResourceId(rs.getObject("resource_id", UUID.class));
        a.setAssetType(ResourceAsset.AssetType.valueOf(rs.getString("asset_type")));
        a.setLabel(rs.getString("label"));
        a.setLocator(rs.getString("locator"));
        a.setSortOrder(rs.getInt("sort_order"));
        return a;
    };

    private static final RowMapper<Bundle> BUNDLE_MAPPER = (rs, rowNum) -> {
        Bundle b = new Bundle();
        b.setId(rs.getObject("id", UUID.class));
        b.setSlug(rs.getString("slug"));
        b.setTitle(rs.getString("title"));
        b.setDescription(rs.getString("description"));
        b.setPriceCents(rs.getInt("price_cents"));
        b.setPublished(rs.getBoolean("published"));
        b.setSortOrder(rs.getInt("sort_order"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) b.setCreatedAt(createdAt.toInstant());
        return b;
    };

    public List<Resource> findPublishedResources() {
        String sql = "SELECT * FROM resources WHERE published = true ORDER BY sort_order, created_at";
        return jdbc.query(sql, Map.of(), RESOURCE_MAPPER);
    }

    public Optional<Resource> findResourceBySlug(String slug) {
        String sql = "SELECT * FROM resources WHERE slug = :slug AND published = true";
        List<Resource> results = jdbc.query(sql, Map.of("slug", slug), RESOURCE_MAPPER);
        return results.stream().findFirst();
    }

    public Optional<Resource> findResourceById(UUID id) {
        String sql = "SELECT * FROM resources WHERE id = :id AND published = true";
        List<Resource> results = jdbc.query(sql, Map.of("id", id), RESOURCE_MAPPER);
        return results.stream().findFirst();
    }

    public List<ResourceAsset> findAssetsByResourceId(UUID resourceId) {
        String sql = "SELECT * FROM resource_assets WHERE resource_id = :resourceId ORDER BY sort_order";
        return jdbc.query(sql, Map.of("resourceId", resourceId), ASSET_MAPPER);
    }

    public List<Bundle> findPublishedBundles() {
        String sql = "SELECT * FROM bundles WHERE published = true ORDER BY sort_order, created_at";
        return jdbc.query(sql, Map.of(), BUNDLE_MAPPER);
    }

    public Optional<Bundle> findBundleBySlug(String slug) {
        String sql = "SELECT * FROM bundles WHERE slug = :slug AND published = true";
        List<Bundle> results = jdbc.query(sql, Map.of("slug", slug), BUNDLE_MAPPER);
        return results.stream().findFirst();
    }

    public List<String> findBundleMemberSlugs(UUID bundleId) {
        String sql = """
                SELECT r.slug FROM resources r
                JOIN bundle_resources br ON br.resource_id = r.id
                WHERE br.bundle_id = :bundleId
                ORDER BY r.sort_order
                """;
        return jdbc.query(sql, Map.of("bundleId", bundleId), (rs, rowNum) -> rs.getString("slug"));
    }

    public List<UUID> findBundleMemberIds(UUID bundleId) {
        String sql = "SELECT resource_id FROM bundle_resources WHERE bundle_id = :bundleId";
        return jdbc.query(sql, Map.of("bundleId", bundleId), (rs, rowNum) -> rs.getObject("resource_id", UUID.class));
    }

    public Set<UUID> findOwnedResourceIds(UUID userId) {
        String sql = "SELECT resource_id FROM entitlements WHERE user_id = :userId";
        return jdbc.query(sql, Map.of("userId", userId), (rs, rowNum) -> rs.getObject("resource_id", UUID.class))
                .stream().collect(Collectors.toSet());
    }

    public boolean hasEntitlement(UUID userId, UUID resourceId) {
        String sql = "SELECT COUNT(*) FROM entitlements WHERE user_id = :userId AND resource_id = :resourceId";
        Integer count = jdbc.queryForObject(sql, Map.of("userId", userId, "resourceId", resourceId), Integer.class);
        return count != null && count > 0;
    }

    public Optional<Bundle> findBundleById(UUID id) {
        String sql = "SELECT * FROM bundles WHERE id = :id AND published = true";
        List<Bundle> results = jdbc.query(sql, Map.of("id", id), BUNDLE_MAPPER);
        return results.stream().findFirst();
    }

    public Optional<String> findRelatedResourceSlugById(UUID resourceId) {
        if (resourceId == null) return Optional.empty();
        String sql = "SELECT slug FROM resources WHERE id = :id AND published = true";
        List<String> results = jdbc.query(sql, Map.of("id", resourceId), (rs, rowNum) -> rs.getString("slug"));
        return results.stream().findFirst();
    }
}
