package com.kuky.backend.units.repository;

import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.PresentationSummary;
import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.units.dto.UnitContentItem;
import com.kuky.backend.units.dto.UnitContentRef;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.dto.UnitSummary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UnitRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UnitRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- CRUD ----------------------------------------------------------------

    public List<UnitSummary> listSummaries() {
        String sql = """
                SELECT u.id, u.level, u.subject, u.position,
                       COUNT(DISTINCT p.id)  AS presentation_count,
                       COUNT(DISTINCT ha.id) AS homework_count,
                       COALESCE(ARRAY_AGG(DISTINCT ua.user_id::text) FILTER (WHERE ua.user_id IS NOT NULL), '{}') AS assigned_student_ids
                FROM units u
                LEFT JOIN presentations p ON p.unit_id = u.id
                LEFT JOIN homework_assignments ha ON ha.unit_id = u.id
                LEFT JOIN unit_assignments ua ON ua.unit_id = u.id
                GROUP BY u.id, u.level, u.subject, u.position
                ORDER BY u.level, u.position
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> {
            java.sql.Array arr = rs.getArray("assigned_student_ids");
            List<String> ids = arr == null ? List.of()
                    : Arrays.stream((Object[]) arr.getArray())
                            .map(Object::toString)
                            .toList();
            return new UnitSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("level"),
                    rs.getString("subject"),
                    rs.getInt("position"),
                    rs.getInt("presentation_count"),
                    rs.getInt("homework_count"),
                    ids);
        });
    }

    public Optional<Unit> findById(UUID id) {
        return jdbc.query("SELECT * FROM units WHERE id = :id", Map.of("id", id), (rs, n) -> {
            Unit u = new Unit();
            u.setId(rs.getObject("id", UUID.class));
            u.setLevel(rs.getString("level"));
            u.setSubject(rs.getString("subject"));
            u.setPosition(rs.getInt("position"));
            u.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            u.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return u;
        }).stream().findFirst();
    }

    public UUID create(String level, String subject) {
        UUID id = UUID.randomUUID();
        int nextPos = nextPosition(level);
        jdbc.update("""
                INSERT INTO units (id, level, subject, position)
                VALUES (:id, :level, :subject, :position)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("level", level)
                .addValue("subject", subject)
                .addValue("position", nextPos));
        return id;
    }

    public void updateLevelSubject(UUID id, String level, String subject) {
        jdbc.update("""
                UPDATE units SET level = :level, subject = :subject, updated_at = NOW()
                WHERE id = :id
                """, Map.of("id", id, "level", level, "subject", subject));
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM units WHERE id = :id", Map.of("id", id));
    }

    public void touch(UUID id) {
        jdbc.update("UPDATE units SET updated_at = NOW() WHERE id = :id", Map.of("id", id));
    }

    public int nextPosition(String level) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(position), -1) FROM units WHERE level = :level",
                Map.of("level", level), Integer.class);
        return (max == null ? -1 : max) + 1;
    }

    // --- Reorder -------------------------------------------------------------

    public List<UUID> findIdsByLevel(String level) {
        return jdbc.query("SELECT id FROM units WHERE level = :level ORDER BY position",
                Map.of("level", level), (rs, n) -> rs.getObject("id", UUID.class));
    }

    @Transactional
    public void reorder(String level, List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            jdbc.update("""
                    UPDATE units SET position = :pos, updated_at = NOW()
                    WHERE id = :id AND level = :level
                    """, new MapSqlParameterSource()
                    .addValue("pos", i)
                    .addValue("id", orderedIds.get(i))
                    .addValue("level", level));
        }
    }

    // --- Content membership --------------------------------------------------

    /** Ordered mixed content refs for a unit (presentations + homeworks by unit_position). */
    public record ContentMember(String type, UUID id, int unitPosition) {}

    public List<ContentMember> findContentMembers(UUID unitId) {
        String sql = """
                SELECT type, id, unit_position FROM (
                    SELECT 'PRESENTATION' AS type, id, unit_position
                    FROM presentations WHERE unit_id = :uid
                    UNION ALL
                    SELECT 'HOMEWORK' AS type, id, unit_position
                    FROM homework_assignments WHERE unit_id = :uid
                ) c
                ORDER BY unit_position, type, id
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> new ContentMember(
                rs.getString("type"),
                rs.getObject("id", UUID.class),
                rs.getInt("unit_position")));
    }

    @Transactional
    public void reorderContents(UUID unitId, List<UnitContentRef> items) {
        for (int i = 0; i < items.size(); i++) {
            UnitContentRef ref = items.get(i);
            String type = ref.type() == null ? "" : ref.type().toUpperCase(java.util.Locale.ROOT);
            if (UnitContentItem.PRESENTATION.equals(type)) {
                jdbc.update("""
                        UPDATE presentations SET unit_position = :pos, updated_at = NOW()
                        WHERE id = :id AND unit_id = :uid
                        """, new MapSqlParameterSource()
                        .addValue("pos", i)
                        .addValue("id", ref.id())
                        .addValue("uid", unitId));
            } else if (UnitContentItem.HOMEWORK.equals(type)) {
                jdbc.update("""
                        UPDATE homework_assignments SET unit_position = :pos
                        WHERE id = :id AND unit_id = :uid
                        """, new MapSqlParameterSource()
                        .addValue("pos", i)
                        .addValue("id", ref.id())
                        .addValue("uid", unitId));
            }
        }
        touch(unitId);
    }

    private void rewritePositions(UUID unitId, List<ContentMember> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ContentMember m = ordered.get(i);
            if (UnitContentItem.PRESENTATION.equals(m.type())) {
                jdbc.update("""
                        UPDATE presentations SET unit_position = :pos, updated_at = NOW()
                        WHERE id = :id AND unit_id = :uid
                        """, new MapSqlParameterSource()
                        .addValue("pos", i)
                        .addValue("id", m.id())
                        .addValue("uid", unitId));
            } else {
                jdbc.update("""
                        UPDATE homework_assignments SET unit_position = :pos
                        WHERE id = :id AND unit_id = :uid
                        """, new MapSqlParameterSource()
                        .addValue("pos", i)
                        .addValue("id", m.id())
                        .addValue("uid", unitId));
            }
        }
    }

    @Transactional
    public void setPresentations(UUID unitId, List<UUID> presentationIds) {
        applyMembership(unitId, UnitContentItem.PRESENTATION, presentationIds);
    }

    @Transactional
    public void setHomeworks(UUID unitId, List<UUID> homeworkIds) {
        applyMembership(unitId, UnitContentItem.HOMEWORK, homeworkIds == null ? List.of() : homeworkIds);
    }

    private void applyMembership(UUID unitId, String memberType, List<UUID> desiredIds) {
        List<UUID> desired = desiredIds == null ? List.of() : desiredIds;
        java.util.LinkedHashSet<UUID> desiredSet = new java.util.LinkedHashSet<>(desired);

        List<ContentMember> current = findContentMembers(unitId);

        for (ContentMember m : current) {
            if (memberType.equals(m.type()) && !desiredSet.contains(m.id())) {
                detachMember(m);
            }
        }

        for (UUID id : desired) {
            compactAfterMoveFromOtherUnit(memberType, id, unitId);
        }

        List<ContentMember> next = new java.util.ArrayList<>();
        java.util.HashSet<UUID> retained = new java.util.HashSet<>();
        for (ContentMember m : current) {
            if (!memberType.equals(m.type())) {
                next.add(m);
            } else if (desiredSet.contains(m.id())) {
                next.add(m);
                retained.add(m.id());
            }
        }
        for (UUID id : desired) {
            if (!retained.contains(id)) {
                attachMember(memberType, id, unitId);
                next.add(new ContentMember(memberType, id, next.size()));
            }
        }

        for (UUID id : desired) {
            if (retained.contains(id)) {
                attachMember(memberType, id, unitId);
            }
        }

        rewritePositions(unitId, next);
        touch(unitId);
    }

    private void detachMember(ContentMember m) {
        if (UnitContentItem.PRESENTATION.equals(m.type())) {
            jdbc.update("""
                    UPDATE presentations SET unit_id = NULL, unit_position = 0, updated_at = NOW()
                    WHERE id = :id
                    """, Map.of("id", m.id()));
        } else {
            jdbc.update("""
                    UPDATE homework_assignments SET unit_id = NULL, unit_position = 0
                    WHERE id = :id
                    """, Map.of("id", m.id()));
        }
    }

    /** If the item belongs to another unit, detach it and compact that unit's remaining sequence. */
    private void compactAfterMoveFromOtherUnit(String type, UUID id, UUID keepUnitId) {
        UUID previousUnitId = null;
        if (UnitContentItem.PRESENTATION.equals(type)) {
            previousUnitId = jdbc.query("""
                    SELECT unit_id FROM presentations
                    WHERE id = :id AND unit_id IS NOT NULL AND unit_id <> :uid
                    """, Map.of("id", id, "uid", keepUnitId),
                    (rs, n) -> rs.getObject("unit_id", UUID.class)).stream().findFirst().orElse(null);
            jdbc.update("""
                    UPDATE presentations SET unit_id = NULL, unit_position = 0, updated_at = NOW()
                    WHERE id = :id AND unit_id IS NOT NULL AND unit_id <> :uid
                    """, Map.of("id", id, "uid", keepUnitId));
        } else {
            previousUnitId = jdbc.query("""
                    SELECT unit_id FROM homework_assignments
                    WHERE id = :id AND unit_id IS NOT NULL AND unit_id <> :uid
                    """, Map.of("id", id, "uid", keepUnitId),
                    (rs, n) -> rs.getObject("unit_id", UUID.class)).stream().findFirst().orElse(null);
            jdbc.update("""
                    UPDATE homework_assignments SET unit_id = NULL, unit_position = 0
                    WHERE id = :id AND unit_id IS NOT NULL AND unit_id <> :uid
                    """, Map.of("id", id, "uid", keepUnitId));
        }
        if (previousUnitId != null) {
            rewritePositions(previousUnitId, findContentMembers(previousUnitId));
            touch(previousUnitId);
        }
    }

    private void attachMember(String type, UUID id, UUID unitId) {
        if (UnitContentItem.PRESENTATION.equals(type)) {
            jdbc.update("""
                    UPDATE presentations SET unit_id = :uid, updated_at = NOW()
                    WHERE id = :id
                    """, Map.of("uid", unitId, "id", id));
        } else {
            jdbc.update("""
                    UPDATE homework_assignments SET unit_id = :uid
                    WHERE id = :id
                    """, Map.of("uid", unitId, "id", id));
        }
    }

    // --- Detail loaders ------------------------------------------------------

    public List<PresentationSummary> findPresentations(UUID unitId) {
        String sql = """
                SELECT p.id, p.title, p.level, p.updated_at, p.unit_position,
                       COALESCE(ARRAY_AGG(sh.user_id::text) FILTER (WHERE sh.user_id IS NOT NULL), '{}') AS shared_with_ids
                FROM presentations p
                LEFT JOIN presentation_shares sh ON sh.presentation_id = p.id
                WHERE p.unit_id = :uid
                GROUP BY p.id, p.title, p.level, p.updated_at, p.unit_position
                ORDER BY p.unit_position
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> {
            java.sql.Array arr = rs.getArray("shared_with_ids");
            List<String> ids = arr == null ? List.of()
                    : Arrays.stream((Object[]) arr.getArray()).map(Object::toString).toList();
            return new PresentationSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("level"),
                    List.of(),
                    ids,
                    rs.getTimestamp("updated_at").toInstant());
        });
    }

    public List<HomeworkAdminItem> findHomeworks(UUID unitId) {
        String sql = """
                SELECT ha.id, ha.title, ha.instructions, ha.due_on, ha.homework_type,
                       ha.level, ha.format, ha.audio_url, ha.audio_file_id, ha.unit_position
                FROM homework_assignments ha
                WHERE ha.unit_id = :uid
                ORDER BY ha.unit_position
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> new HomeworkAdminItem(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("instructions"),
                rs.getObject("due_on", LocalDate.class),
                rs.getString("homework_type"),
                rs.getString("level"),
                rs.getString("format"),
                List.of(),
                rs.getString("audio_url"),
                rs.getObject("audio_file_id", UUID.class),
                null,
                List.of()));
    }

    public List<StudentResponse> findAssignedStudents(UUID unitId) {
        return jdbc.query("""
                SELECT u.id, u.email, u.first_name, u.last_name, u.username
                FROM unit_assignments ua JOIN users u ON u.id = ua.user_id
                WHERE ua.unit_id = :uid ORDER BY u.email
                """, Map.of("uid", unitId),
                (rs, n) -> new StudentResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("username")));
    }

    // --- Assignees -----------------------------------------------------------

    public List<UUID> findAssigneeIds(UUID unitId) {
        return jdbc.query("""
                SELECT user_id FROM unit_assignments WHERE unit_id = :uid ORDER BY user_id
                """, Map.of("uid", unitId), (rs, n) -> rs.getObject("user_id", UUID.class));
    }

    public List<UUID> findHomeworkIds(UUID unitId) {
        return jdbc.query("""
                SELECT id FROM homework_assignments WHERE unit_id = :uid ORDER BY unit_position, id
                """, Map.of("uid", unitId), (rs, n) -> rs.getObject("id", UUID.class));
    }

    public java.util.Optional<UUID> findUnitIdForHomework(UUID homeworkId) {
        return jdbc.query("""
                SELECT unit_id FROM homework_assignments WHERE id = :id AND unit_id IS NOT NULL
                """, Map.of("id", homeworkId), (rs, n) -> rs.getObject("unit_id", UUID.class))
                .stream().findFirst();
    }

    @Transactional
    public void replaceAssignees(UUID unitId, List<UUID> studentIds) {
        jdbc.update("DELETE FROM unit_assignments WHERE unit_id = :uid", Map.of("uid", unitId));
        for (UUID userId : studentIds) {
            jdbc.update("""
                    INSERT INTO unit_assignments (id, unit_id, user_id)
                    VALUES (:id, :uid, :userId)
                    ON CONFLICT (unit_id, user_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("uid", unitId)
                    .addValue("userId", userId));
        }
        touch(unitId);
    }

    // --- Student progress ------------------------------------------------------

    /**
     * Homework totals for a student on an assigned unit: homeworks in the unit that
     * are targeted at the student ({@code homework_targets}). Unit assignment now
     * auto-targets unit homeworks; targets remain the access source of truth.
     */
    public record UnitProgressView(UUID unitId, String subject, String level,
                                   int totalHomeworks, int completedHomeworks) {}

    public List<UnitProgressView> findProgressForStudent(UUID studentId) {
        String sql = """
                SELECT u.id AS unit_id, u.subject, u.level,
                       COUNT(t.id) AS total_homeworks,
                       COUNT(t.id) FILTER (WHERE COALESCE(s.status, 'PENDING') IN ('REVIEWED', 'GRADED')) AS completed_homeworks
                FROM unit_assignments ua
                JOIN units u ON u.id = ua.unit_id
                LEFT JOIN homework_assignments ha ON ha.unit_id = u.id
                LEFT JOIN homework_targets t ON t.assignment_id = ha.id AND t.user_id = ua.user_id
                LEFT JOIN homework_submissions s ON s.assignment_id = ha.id AND s.user_id = ua.user_id
                WHERE ua.user_id = :studentId
                GROUP BY u.id, u.subject, u.level, u.position
                ORDER BY u.position
                """;
        return jdbc.query(sql, Map.of("studentId", studentId), (rs, n) -> new UnitProgressView(
                rs.getObject("unit_id", UUID.class),
                rs.getString("subject"),
                rs.getString("level"),
                rs.getInt("total_homeworks"),
                rs.getInt("completed_homeworks")));
    }

    // --- Unit-derived presentation access (for LearningService) --------------

    public boolean isAccessibleByUser(UUID presentationId, UUID userId) {
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

    public record UnitInfo(String level, String subject, int position) {}

    public List<PresentationWithUnit> findAccessiblePresentationsForUser(UUID userId) {
        String sql = """
                SELECT p.id, p.title, p.level AS p_level, p.updated_at,
                       EXISTS (SELECT 1 FROM presentation_files pf WHERE pf.presentation_id = p.id) AS has_file,
                       u.level AS unit_level, u.subject AS unit_subject, u.position AS unit_position
                FROM presentations p
                LEFT JOIN units u ON u.id = p.unit_id
                WHERE
                    EXISTS (SELECT 1 FROM presentation_shares s WHERE s.presentation_id = p.id AND s.user_id = :uid)
                    OR
                    EXISTS (SELECT 1 FROM unit_assignments ua WHERE ua.unit_id = p.unit_id AND ua.user_id = :uid)
                ORDER BY COALESCE(u.level, 'ZZ'), COALESCE(u.position, 999), p.updated_at DESC
                """;
        return jdbc.query(sql, Map.of("uid", userId), (rs, n) -> {
            String unitLevel = rs.getString("unit_level");
            UnitInfo unitInfo = unitLevel == null ? null
                    : new UnitInfo(unitLevel, rs.getString("unit_subject"), rs.getInt("unit_position"));
            return new PresentationWithUnit(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getBoolean("has_file"),
                    unitInfo);
        });
    }

    public record PresentationWithUnit(UUID id, String title, boolean hasFile, UnitInfo unit) {}
}
