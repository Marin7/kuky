package com.kuky.backend.units.repository;

import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.units.dto.UnitActivityProgressRow;
import com.kuky.backend.units.dto.UnitContentItem;
import com.kuky.backend.units.dto.UnitContentRef;
import com.kuky.backend.units.dto.UnitSummary;
import com.kuky.backend.units.model.Unit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UnitRepository {

    /** Activity work from a student who still holds the unit and that the teacher has not opened yet. */
    private static final String UNSEEN_SUBMISSION = """
            (s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED') AND s.teacher_seen_at IS NULL
             AND EXISTS (SELECT 1 FROM unit_assignments ua2
                         WHERE ua2.unit_id = p.unit_id AND ua2.user_id = s.user_id))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public UnitRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- CRUD ----------------------------------------------------------------

    public List<UnitSummary> listSummaries() {
        String sql = """
                SELECT u.id, u.level, u.subject, u.position,
                       (SELECT f.page_count FROM presentations p
                        JOIN presentation_files f ON f.presentation_id = p.id
                        WHERE p.unit_id = u.id
                        ORDER BY f.created_at DESC LIMIT 1) AS page_count,
                       (SELECT COUNT(*) FROM presentations p
                        JOIN activities a ON a.presentation_id = p.id
                        WHERE p.unit_id = u.id) AS activity_count,
                       (SELECT COUNT(*) FROM homework_assignments ha WHERE ha.unit_id = u.id) AS homework_count,
                       (SELECT COALESCE(ARRAY_AGG(ua.user_id::text), '{}') FROM unit_assignments ua
                        WHERE ua.unit_id = u.id) AS assigned_student_ids,
                       EXISTS (SELECT 1 FROM presentations p
                               JOIN activities a ON a.presentation_id = p.id
                               JOIN activity_submissions s ON s.activity_id = a.id
                               WHERE p.unit_id = u.id AND """ + UNSEEN_SUBMISSION + """
                       ) AS has_unseen
                FROM units u
                ORDER BY u.level, u.position
                """;
        return jdbc.query(sql, Map.of(), (rs, n) -> {
            java.sql.Array arr = rs.getArray("assigned_student_ids");
            List<String> ids = arr == null ? List.of()
                    : Arrays.stream((Object[]) arr.getArray())
                            .map(Object::toString)
                            .toList();
            Integer pageCount = rs.getObject("page_count", Integer.class);
            return new UnitSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("level"),
                    rs.getString("subject"),
                    rs.getInt("position"),
                    pageCount != null,
                    pageCount,
                    rs.getInt("activity_count"),
                    rs.getInt("homework_count"),
                    ids,
                    rs.getBoolean("has_unseen"));
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

    /**
     * Ordered mixed content of a unit: its own PDF (the owned presentation, type {@code PDF})
     * and its homeworks, by {@code unit_position}.
     */
    public record ContentMember(String type, UUID id, int unitPosition) {}

    public List<ContentMember> findContentMembers(UUID unitId) {
        String sql = """
                SELECT type, id, unit_position FROM (
                    SELECT 'PDF' AS type, id, unit_position
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

    /** Next free slot at the end of the unit's sequence. */
    public int nextContentPosition(UUID unitId) {
        List<ContentMember> members = findContentMembers(unitId);
        return members.isEmpty() ? 0 : members.get(members.size() - 1).unitPosition() + 1;
    }

    @Transactional
    public void reorderContents(UUID unitId, List<UnitContentRef> items) {
        List<ContentMember> ordered = new ArrayList<>();
        for (UnitContentRef ref : items) {
            String type = ref.type() == null ? "" : ref.type().toUpperCase(Locale.ROOT);
            ordered.add(new ContentMember(type, ref.id(), ordered.size()));
        }
        rewritePositions(unitId, ordered);
        touch(unitId);
    }

    private void rewritePositions(UUID unitId, List<ContentMember> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ContentMember m = ordered.get(i);
            String table = UnitContentItem.PDF.equals(m.type()) ? "presentations" : "homework_assignments";
            jdbc.update("UPDATE " + table + " SET unit_position = :pos WHERE id = :id AND unit_id = :uid",
                    new MapSqlParameterSource()
                            .addValue("pos", i)
                            .addValue("id", m.id())
                            .addValue("uid", unitId));
        }
    }

    /** Replaces the unit's homeworks, keeping the PDF and retained homeworks in their order. */
    @Transactional
    public void setHomeworks(UUID unitId, List<UUID> homeworkIds) {
        List<UUID> desired = homeworkIds == null ? List.of() : homeworkIds;
        LinkedHashSet<UUID> desiredSet = new LinkedHashSet<>(desired);
        List<ContentMember> current = findContentMembers(unitId);

        for (ContentMember m : current) {
            if (UnitContentItem.HOMEWORK.equals(m.type()) && !desiredSet.contains(m.id())) {
                jdbc.update("UPDATE homework_assignments SET unit_id = NULL, unit_position = 0 WHERE id = :id",
                        Map.of("id", m.id()));
            }
        }
        for (UUID id : desired) {
            detachFromOtherUnit(id, unitId);
        }

        List<ContentMember> next = new ArrayList<>();
        HashSet<UUID> retained = new HashSet<>();
        for (ContentMember m : current) {
            if (!UnitContentItem.HOMEWORK.equals(m.type())) {
                next.add(m);
            } else if (desiredSet.contains(m.id())) {
                next.add(m);
                retained.add(m.id());
            }
        }
        for (UUID id : desired) {
            if (!retained.contains(id)) {
                jdbc.update("UPDATE homework_assignments SET unit_id = :uid WHERE id = :id",
                        Map.of("uid", unitId, "id", id));
                next.add(new ContentMember(UnitContentItem.HOMEWORK, id, next.size()));
            }
        }

        rewritePositions(unitId, next);
        touch(unitId);
    }

    /** If the homework belongs to another unit, detach it and compact that unit's remaining sequence. */
    private void detachFromOtherUnit(UUID homeworkId, UUID keepUnitId) {
        UUID previousUnitId = jdbc.query("""
                SELECT unit_id FROM homework_assignments
                WHERE id = :id AND unit_id IS NOT NULL AND unit_id <> :uid
                """, Map.of("id", homeworkId, "uid", keepUnitId),
                (rs, n) -> rs.getObject("unit_id", UUID.class)).stream().findFirst().orElse(null);
        if (previousUnitId == null) {
            return;
        }
        jdbc.update("UPDATE homework_assignments SET unit_id = NULL, unit_position = 0 WHERE id = :id",
                Map.of("id", homeworkId));
        rewritePositions(previousUnitId, findContentMembers(previousUnitId));
        touch(previousUnitId);
    }

    // --- Detail loaders ------------------------------------------------------

    public List<HomeworkAdminItem> findHomeworks(UUID unitId) {
        String sql = """
                SELECT ha.id, ha.title, ha.instructions, ha.homework_type,
                       ha.level, ha.format, ha.audio_url, ha.audio_file_id, ha.media_source_kind,
                       ha.labels, ha.unit_position
                FROM homework_assignments ha
                WHERE ha.unit_id = :uid
                ORDER BY ha.unit_position
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> {
            String homeworkType = rs.getString("homework_type");
            String format = rs.getString("format");
            return new HomeworkAdminItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("instructions"),
                    homeworkType,
                    rs.getString("level"),
                    format,
                    compositionFrom(homeworkType, format),
                    List.of(),
                    rs.getString("audio_url"),
                    rs.getObject("audio_file_id", UUID.class),
                    null,
                    rs.getString("media_source_kind"),
                    readLabels(rs),
                    List.of(),
                    false);
        });
    }

    private static String compositionFrom(String homeworkType, String format) {
        if ("WRITE".equals(homeworkType)) return "WRITE";
        if ("MIXED".equals(format)) return "MIXED";
        if ("EXERCISE".equals(format)) return "ALL_AUTO";
        return "ALL_MANUAL";
    }

    private static List<String> readLabels(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.sql.Array arr = rs.getArray("labels");
        if (arr == null) {
            return List.of();
        }
        Object raw = arr.getArray();
        if (raw instanceof String[] strings) {
            return List.of(strings);
        }
        return Arrays.stream((Object[]) raw).map(Object::toString).toList();
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

    // --- Page activities -------------------------------------------------------

    /** Per-activity submission counts (assigned students only) for the unit's PDF, in page order. */
    public record ActivityCounts(UUID id, int page, String title, String format,
                                 int submitted, int awaitingCorrection, int graded, boolean hasUnseen) {}

    public List<ActivityCounts> findActivityCounts(UUID unitId) {
        String sql = """
                SELECT a.id, a.page, a.title, a.format,
                       COUNT(ua.user_id) FILTER (WHERE s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED')) AS submitted,
                       COUNT(ua.user_id) FILTER (WHERE s.status = 'SUBMITTED') AS awaiting,
                       COUNT(ua.user_id) FILTER (WHERE s.status IN ('REVIEWED', 'GRADED')) AS graded,
                       COALESCE(BOOL_OR(ua.user_id IS NOT NULL
                                        AND s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
                                        AND s.teacher_seen_at IS NULL), false) AS has_unseen
                FROM presentations p
                JOIN activities a ON a.presentation_id = p.id
                LEFT JOIN activity_submissions s ON s.activity_id = a.id
                LEFT JOIN unit_assignments ua ON ua.unit_id = p.unit_id AND ua.user_id = s.user_id
                WHERE p.unit_id = :uid
                GROUP BY a.id, a.page, a.title, a.format
                ORDER BY a.page
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> new ActivityCounts(
                rs.getObject("id", UUID.class),
                rs.getInt("page"),
                rs.getString("title"),
                rs.getString("format"),
                rs.getInt("submitted"),
                rs.getInt("awaiting"),
                rs.getInt("graded"),
                rs.getBoolean("has_unseen")));
    }

    /** One row per (activity × assigned student); a missing submission reads as PENDING. */
    public List<UnitActivityProgressRow> findActivityProgress(UUID unitId) {
        String sql = """
                SELECT a.id AS activity_id, a.page, ua.user_id AS student_id,
                       COALESCE(s.status, 'PENDING') AS status, s.score_percent,
                       s.id AS submission_id, s.submitted_at,
                       COALESCE(s.status IN ('SUBMITTED', 'REVIEWED', 'GRADED')
                                AND s.teacher_seen_at IS NULL, false) AS unseen
                FROM presentations p
                JOIN activities a ON a.presentation_id = p.id
                JOIN unit_assignments ua ON ua.unit_id = p.unit_id
                LEFT JOIN activity_submissions s ON s.activity_id = a.id AND s.user_id = ua.user_id
                WHERE p.unit_id = :uid
                ORDER BY a.page, ua.user_id
                """;
        return jdbc.query(sql, Map.of("uid", unitId), (rs, n) -> {
            var submittedAt = rs.getTimestamp("submitted_at");
            return new UnitActivityProgressRow(
                    rs.getObject("activity_id", UUID.class),
                    rs.getInt("page"),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("status"),
                    rs.getObject("score_percent", Integer.class),
                    rs.getObject("submission_id", UUID.class),
                    submittedAt == null ? null : submittedAt.toInstant(),
                    rs.getBoolean("unseen"));
        });
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

    public Optional<UUID> findUnitIdForHomework(UUID homeworkId) {
        return jdbc.query("""
                SELECT unit_id FROM homework_assignments WHERE id = :id AND unit_id IS NOT NULL
                """, Map.of("id", homeworkId), (rs, n) -> rs.getObject("unit_id", UUID.class))
                .stream().findFirst();
    }

    @Transactional
    public void replaceAssignees(UUID unitId, List<UUID> studentIds) {
        List<UUID> ids = studentIds == null ? List.of() : studentIds;
        if (ids.isEmpty()) {
            jdbc.update("DELETE FROM unit_assignments WHERE unit_id = :uid", Map.of("uid", unitId));
        } else {
            jdbc.update("""
                    DELETE FROM unit_assignments
                    WHERE unit_id = :uid AND user_id NOT IN (:uids)
                    """, Map.of("uid", unitId, "uids", ids));
            for (UUID userId : ids) {
                jdbc.update("""
                        INSERT INTO unit_assignments (id, unit_id, user_id)
                        VALUES (:id, :uid, :userId)
                        ON CONFLICT (unit_id, user_id) DO NOTHING
                        """, new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("uid", unitId)
                        .addValue("userId", userId));
            }
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
}
