package com.kuky.backend.units.repository;

import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.units.dto.UnitSummary;
import com.kuky.backend.units.model.Unit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
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
                       (SELECT COUNT(*) FROM quizzes q WHERE q.unit_id = u.id) AS quiz_count,
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
                    rs.getInt("quiz_count"),
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

    // --- Quizzes at the end of the unit ----------------------------------------

    public record UnitQuiz(UUID id, String title, int questionCount) {}

    /** The unit's quizzes in the teacher's order. */
    public List<UnitQuiz> findQuizzes(UUID unitId) {
        return jdbc.query("""
                SELECT q.id, q.title,
                       (SELECT COUNT(*) FROM quiz_questions qq
                        WHERE qq.quiz_id = q.id AND NOT qq.retired) AS question_count
                FROM quizzes q
                WHERE q.unit_id = :uid
                ORDER BY q.unit_position, q.title
                """, Map.of("uid", unitId), (rs, n) -> new UnitQuiz(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getInt("question_count")));
    }

    /** The unit (if any) a quiz currently belongs to. */
    public Optional<UUID> findUnitIdForQuiz(UUID quizId) {
        return jdbc.query("SELECT unit_id FROM quizzes WHERE id = :id AND unit_id IS NOT NULL",
                Map.of("id", quizId), (rs, n) -> rs.getObject("unit_id", UUID.class)).stream().findFirst();
    }

    /** Makes {@code quizIds} (in that order) the unit's quizzes; quizzes no longer listed are unlinked. */
    @Transactional
    public void setQuizzes(UUID unitId, List<UUID> quizIds) {
        if (quizIds.isEmpty()) {
            jdbc.update("UPDATE quizzes SET unit_id = NULL, unit_position = 0 WHERE unit_id = :uid",
                    Map.of("uid", unitId));
        } else {
            jdbc.update("""
                    UPDATE quizzes SET unit_id = NULL, unit_position = 0
                    WHERE unit_id = :uid AND id NOT IN (:ids)
                    """, Map.of("uid", unitId, "ids", quizIds));
        }
        for (int i = 0; i < quizIds.size(); i++) {
            jdbc.update("UPDATE quizzes SET unit_id = :uid, unit_position = :pos WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("uid", unitId)
                            .addValue("pos", i)
                            .addValue("id", quizIds.get(i)));
        }
        touch(unitId);
    }

    // --- Assignees -----------------------------------------------------------

    public List<UUID> findAssigneeIds(UUID unitId) {
        return jdbc.query("SELECT user_id FROM unit_assignments WHERE unit_id = :uid",
                Map.of("uid", unitId), (rs, n) -> rs.getObject("user_id", UUID.class));
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

}
