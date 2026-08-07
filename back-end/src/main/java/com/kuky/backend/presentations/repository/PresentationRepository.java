package com.kuky.backend.presentations.repository;

import com.kuky.backend.admin.dto.PresentationFileSummary;
import com.kuky.backend.presentations.model.Presentation;
import com.kuky.backend.presentations.model.PresentationFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    public record Summary(UUID id, String title, String level,
                          List<UUID> sharedWithIds, Instant updatedAt) {}
    public record SharedUser(UUID userId, String email, String firstName, String lastName, String username) {}

    // --- presentations -------------------------------------------------------

    public List<Summary> listSummaries() {
        String sql = """
                SELECT p.id, p.title, p.level, p.updated_at,
                       COALESCE(ARRAY_AGG(sh.user_id::text) FILTER (WHERE sh.user_id IS NOT NULL), '{}') AS shared_with_ids
                FROM presentations p
                LEFT JOIN presentation_shares sh ON sh.presentation_id = p.id
                GROUP BY p.id, p.title, p.level, p.updated_at
                ORDER BY p.updated_at DESC
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> {
            java.sql.Array arr = rs.getArray("shared_with_ids");
            List<UUID> ids = arr == null ? List.of()
                    : Arrays.stream((Object[]) arr.getArray())
                            .map(o -> UUID.fromString(o.toString()))
                            .toList();
            return new Summary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("level"),
                    ids,
                    rs.getTimestamp("updated_at").toInstant());
        });
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
            p.setLevel(rs.getString("level"));
            p.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            p.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return p;
        }).stream().findFirst();
    }

    public int updateLevel(UUID id, String level) {
        return jdbc.update("UPDATE presentations SET level = :level, updated_at = NOW() WHERE id = :id",
                Map.of("id", id, "level", level));
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

    public List<PresentationFileSummary> listFiles(UUID presentationId) {
        return jdbc.query("""
                SELECT id, original_name, display_name, content_type, byte_size, created_at
                FROM presentation_files
                WHERE presentation_id = :pid
                ORDER BY created_at ASC, id ASC
                """, Map.of("pid", presentationId), this::mapFileSummary);
    }

    /** All file rows for migration / batch mapping, oldest-first per presentation. */
    public List<PresentationFile> listAllFileRows() {
        return jdbc.query("""
                SELECT id, presentation_id, original_name, display_name, content_type, byte_size, created_at
                FROM presentation_files
                ORDER BY presentation_id, created_at ASC, id ASC
                """, Map.of(), (rs, n) -> new PresentationFile(
                rs.getObject("id", UUID.class),
                rs.getObject("presentation_id", UUID.class),
                rs.getString("original_name"),
                rs.getString("display_name"),
                rs.getString("content_type"),
                rs.getInt("byte_size"),
                rs.getTimestamp("created_at").toInstant(),
                null));
    }

    public Map<UUID, List<PresentationFileSummary>> listFilesGrouped(List<UUID> presentationIds) {
        Map<UUID, List<PresentationFileSummary>> result = new HashMap<>();
        if (presentationIds == null || presentationIds.isEmpty()) {
            return result;
        }
        for (UUID id : presentationIds) {
            result.put(id, new ArrayList<>());
        }
        jdbc.query("""
                SELECT id, presentation_id, original_name, display_name, content_type, byte_size, created_at
                FROM presentation_files
                WHERE presentation_id IN (:pids)
                ORDER BY created_at ASC, id ASC
                """, Map.of("pids", presentationIds), (rs, n) -> {
            UUID pid = rs.getObject("presentation_id", UUID.class);
            result.computeIfAbsent(pid, k -> new ArrayList<>()).add(mapFileSummary(rs, n));
            return null;
        });
        return result;
    }

    public int countFiles(UUID presentationId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM presentation_files WHERE presentation_id = :pid",
                Map.of("pid", presentationId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<String> listDisplayNames(UUID presentationId) {
        return jdbc.query("""
                SELECT display_name FROM presentation_files
                WHERE presentation_id = :pid
                """, Map.of("pid", presentationId),
                (rs, n) -> rs.getString("display_name"));
    }

    public Optional<PresentationFile> findFile(UUID presentationId, UUID fileId) {
        return jdbc.query("""
                SELECT id, presentation_id, original_name, display_name, content_type, byte_size, created_at
                FROM presentation_files
                WHERE presentation_id = :pid AND id = :fid
                """, Map.of("pid", presentationId, "fid", fileId),
                (rs, n) -> new PresentationFile(
                        rs.getObject("id", UUID.class),
                        rs.getObject("presentation_id", UUID.class),
                        rs.getString("original_name"),
                        rs.getString("display_name"),
                        rs.getString("content_type"),
                        rs.getInt("byte_size"),
                        rs.getTimestamp("created_at").toInstant(),
                        null))
                .stream().findFirst();
    }

    public List<UUID> listFileIds(UUID presentationId) {
        return jdbc.query("""
                SELECT id FROM presentation_files WHERE presentation_id = :pid
                """, Map.of("pid", presentationId),
                (rs, n) -> rs.getObject("id", UUID.class));
    }

    public void insertFile(UUID fileId, UUID presentationId, String originalName,
                           String displayName, String contentType, int byteSize) {
        jdbc.update("""
                INSERT INTO presentation_files
                    (id, presentation_id, original_name, display_name, content_type, byte_size)
                VALUES (:id, :pid, :name, :display, :ct, :size)
                """,
                new MapSqlParameterSource()
                        .addValue("id", fileId)
                        .addValue("pid", presentationId)
                        .addValue("name", originalName)
                        .addValue("display", displayName)
                        .addValue("ct", contentType)
                        .addValue("size", byteSize));
    }

    public int deleteFile(UUID presentationId, UUID fileId) {
        return jdbc.update(
                "DELETE FROM presentation_files WHERE presentation_id = :pid AND id = :fid",
                Map.of("pid", presentationId, "fid", fileId));
    }

    private PresentationFileSummary mapFileSummary(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new PresentationFileSummary(
                rs.getObject("id", UUID.class),
                rs.getString("display_name"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getInt("byte_size"),
                rs.getTimestamp("created_at").toInstant());
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
                SELECT u.id AS user_id, u.email, u.first_name, u.last_name, u.username
                FROM presentation_shares sh JOIN users u ON u.id = sh.user_id
                WHERE sh.presentation_id = :pid ORDER BY u.email
                """, Map.of("pid", presentationId),
                (rs, n) -> new SharedUser(rs.getObject("user_id", UUID.class), rs.getString("email"),
                        rs.getString("first_name"), rs.getString("last_name"), rs.getString("username")));
    }

    // --- access UNION: direct share OR unit assignment -----------------------

    public boolean isSharedWith(UUID presentationId, UUID userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(1) FROM presentations p
                WHERE p.id = :pid
                  AND (
                    EXISTS (SELECT 1 FROM presentation_shares s WHERE s.presentation_id = p.id AND s.user_id = :uid)
                    OR
                    EXISTS (SELECT 1 FROM unit_assignments ua WHERE ua.unit_id = p.unit_id AND ua.user_id = :uid)
                  )
                """, Map.of("pid", presentationId, "uid", userId), Integer.class);
        return count != null && count > 0;
    }

    public record UnitRef(UUID id, String level, String subject, int position) {}
    public record SharedSummaryWithUnit(UUID id, String title, String level, UnitRef unit) {}

    public List<SharedSummaryWithUnit> findSharedSummariesForUser(UUID userId) {
        String sql = """
                SELECT p.id, p.title, p.level, p.updated_at,
                       u.id AS unit_id, u.level AS unit_level, u.subject AS unit_subject,
                       u.position AS unit_position
                FROM presentations p
                LEFT JOIN units u ON u.id = p.unit_id
                WHERE
                    EXISTS (SELECT 1 FROM presentation_shares s WHERE s.presentation_id = p.id AND s.user_id = :uid)
                    OR
                    EXISTS (SELECT 1 FROM unit_assignments ua WHERE ua.unit_id = p.unit_id AND ua.user_id = :uid)
                ORDER BY COALESCE(u.level, 'ZZ'), COALESCE(u.position, 999), p.updated_at DESC
                """;
        return jdbc.query(sql, Map.of("uid", userId), (rs, n) -> {
            UUID unitId = rs.getObject("unit_id", UUID.class);
            UnitRef unitRef = unitId == null ? null
                    : new UnitRef(unitId, rs.getString("unit_level"), rs.getString("unit_subject"),
                            rs.getInt("unit_position"));
            return new SharedSummaryWithUnit(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("level"),
                    unitRef);
        });
    }
}
