package com.kuky.backend.presentations.repository;

import com.kuky.backend.presentations.model.Presentation;
import com.kuky.backend.presentations.model.PresentationFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PresentationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PresentationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Summary(UUID id, String title, boolean hasFile, String originalFileName,
                          long sharedWith, Instant updatedAt) {}
    public record SharedUser(UUID userId, String email) {}

    // --- presentations -------------------------------------------------------

    public List<Summary> listSummaries() {
        String sql = """
                SELECT p.id, p.title, p.updated_at,
                       pf.original_name,
                       (SELECT COUNT(*) FROM presentation_shares sh WHERE sh.presentation_id = p.id) AS shared_count
                FROM presentations p
                LEFT JOIN presentation_files pf ON pf.presentation_id = p.id
                ORDER BY p.updated_at DESC
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> new Summary(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("original_name") != null,
                rs.getString("original_name"),
                rs.getLong("shared_count"),
                rs.getTimestamp("updated_at").toInstant()));
    }

    public UUID create(String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO presentations (id, title) VALUES (:id, :title)",
                Map.of("id", id, "title", title));
        return id;
    }

    public Optional<Presentation> findById(UUID id) {
        return jdbc.query("SELECT * FROM presentations WHERE id = :id", Map.of("id", id), (rs, n) -> {
            Presentation p = new Presentation();
            p.setId(rs.getObject("id", UUID.class));
            p.setTitle(rs.getString("title"));
            p.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            p.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return p;
        }).stream().findFirst();
    }

    public int rename(UUID id, String title) {
        return jdbc.update("UPDATE presentations SET title = :title, updated_at = NOW() WHERE id = :id",
                Map.of("id", id, "title", title));
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM presentations WHERE id = :id", Map.of("id", id));
    }

    public void touch(UUID id) {
        jdbc.update("UPDATE presentations SET updated_at = NOW() WHERE id = :id", Map.of("id", id));
    }

    // --- files ---------------------------------------------------------------

    /** Full record including bytes — for serving a download. */
    public Optional<PresentationFile> findFile(UUID presentationId) {
        return jdbc.query(
                "SELECT * FROM presentation_files WHERE presentation_id = :pid",
                Map.of("pid", presentationId),
                (rs, n) -> new PresentationFile(
                        rs.getObject("presentation_id", UUID.class),
                        rs.getString("original_name"),
                        rs.getString("content_type"),
                        rs.getInt("byte_size"),
                        rs.getBytes("data")))
                .stream().findFirst();
    }

    /** Metadata only (no bytes) — for building summary/detail responses. */
    public Optional<String> findOriginalFileName(UUID presentationId) {
        return jdbc.query(
                "SELECT original_name FROM presentation_files WHERE presentation_id = :pid",
                Map.of("pid", presentationId),
                (rs, n) -> rs.getString("original_name"))
                .stream().findFirst();
    }

    public void upsertFile(UUID presentationId, String originalName,
                           String contentType, int byteSize, byte[] data) {
        jdbc.update("""
                INSERT INTO presentation_files (presentation_id, original_name, content_type, byte_size, data)
                VALUES (:pid, :name, :ct, :size, :data)
                ON CONFLICT (presentation_id) DO UPDATE SET
                    original_name = EXCLUDED.original_name,
                    content_type  = EXCLUDED.content_type,
                    byte_size     = EXCLUDED.byte_size,
                    data          = EXCLUDED.data,
                    created_at    = NOW()
                """,
                new MapSqlParameterSource()
                        .addValue("pid", presentationId)
                        .addValue("name", originalName)
                        .addValue("ct", contentType)
                        .addValue("size", byteSize)
                        .addValue("data", data));
    }

    public void deleteFile(UUID presentationId) {
        jdbc.update("DELETE FROM presentation_files WHERE presentation_id = :pid",
                Map.of("pid", presentationId));
    }

    // --- shares --------------------------------------------------------------

    @Transactional
    public void replaceShares(UUID presentationId, List<UUID> userIds) {
        jdbc.update("DELETE FROM presentation_shares WHERE presentation_id = :pid",
                Map.of("pid", presentationId));
        for (UUID userId : userIds) {
            jdbc.update("""
                    INSERT INTO presentation_shares (id, presentation_id, user_id)
                    VALUES (:id, :pid, :uid)
                    ON CONFLICT (presentation_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("pid", presentationId)
                    .addValue("uid", userId));
        }
    }

    // --- slides --------------------------------------------------------------

    public List<UUID> findSlideIds(UUID presentationId) {
        return jdbc.query("""
                SELECT id FROM presentation_slides
                WHERE presentation_id = :pid
                ORDER BY sort_order, created_at
                """, Map.of("pid", presentationId),
                (rs, n) -> rs.getObject("id", UUID.class));
    }

    public List<com.kuky.backend.admin.dto.SlideDto> findSlides(UUID presentationId) {
        return jdbc.query("""
                SELECT id, heading, body, image_id, sort_order
                FROM presentation_slides
                WHERE presentation_id = :pid
                ORDER BY sort_order, created_at
                """, Map.of("pid", presentationId),
                (rs, n) -> new com.kuky.backend.admin.dto.SlideDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("heading"),
                        rs.getString("body"),
                        rs.getObject("image_id", UUID.class),
                        rs.getInt("sort_order")));
    }

    public void updateSortOrders(UUID presentationId, List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            jdbc.update("""
                    UPDATE presentation_slides SET sort_order = :order
                    WHERE id = :id AND presentation_id = :pid
                    """, new MapSqlParameterSource()
                    .addValue("order", i)
                    .addValue("id", orderedIds.get(i))
                    .addValue("pid", presentationId));
        }
    }

    public UUID insertSlide(UUID presentationId, String heading, String body, UUID imageId, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO presentation_slides (id, presentation_id, heading, body, image_id, sort_order)
                VALUES (:id, :pid, :heading, :body, :imageId, :sortOrder)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("pid", presentationId)
                .addValue("heading", heading)
                .addValue("body", body)
                .addValue("imageId", imageId)
                .addValue("sortOrder", sortOrder));
        return id;
    }

    public List<SharedUser> findSharedUsers(UUID presentationId) {
        return jdbc.query("""
                SELECT u.id AS user_id, u.email AS email
                FROM presentation_shares sh JOIN users u ON u.id = sh.user_id
                WHERE sh.presentation_id = :pid ORDER BY u.email
                """, Map.of("pid", presentationId),
                (rs, n) -> new SharedUser(rs.getObject("user_id", UUID.class), rs.getString("email")));
    }

    public boolean isSharedWith(UUID presentationId, UUID userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(1) FROM presentation_shares
                WHERE presentation_id = :pid AND user_id = :uid
                """, Map.of("pid", presentationId, "uid", userId), Integer.class);
        return count != null && count > 0;
    }

    public List<Summary> findSharedSummariesForUser(UUID userId) {
        String sql = """
                SELECT p.id, p.title, p.updated_at,
                       pf.original_name,
                       0 AS shared_count
                FROM presentations p
                JOIN presentation_shares sh ON sh.presentation_id = p.id
                LEFT JOIN presentation_files pf ON pf.presentation_id = p.id
                WHERE sh.user_id = :uid
                ORDER BY p.updated_at DESC
                """;
        return jdbc.query(sql, Map.of("uid", userId), (rs, n) -> new Summary(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("original_name") != null,
                rs.getString("original_name"),
                0L,
                rs.getTimestamp("updated_at").toInstant()));
    }
}
