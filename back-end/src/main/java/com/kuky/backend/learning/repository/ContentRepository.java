package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkLevel;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.PastClass;
import com.kuky.backend.learning.model.PresentationBlock;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the shared, seeded learning content: presentation blocks, past classes,
 * and homework assignment definitions. Per-student state lives in
 * {@link HomeworkSubmissionRepository}.
 */
@Repository
public class ContentRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ContentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PresentationBlock> PRESENTATION_MAPPER = (rs, rowNum) -> {
        PresentationBlock p = new PresentationBlock();
        p.setId(rs.getObject("id", UUID.class));
        p.setHeading(rs.getString("heading"));
        p.setBody(rs.getString("body"));
        p.setPublished(rs.getBoolean("published"));
        p.setSortOrder(rs.getInt("sort_order"));
        p.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return p;
    };

    private static final RowMapper<PastClass> PAST_CLASS_MAPPER = (rs, rowNum) -> {
        PastClass c = new PastClass();
        c.setId(rs.getObject("id", UUID.class));
        c.setTitle(rs.getString("title"));
        c.setHeldOn(rs.getObject("held_on", LocalDate.class));
        c.setTeacherNote(rs.getString("teacher_note"));
        c.setPublished(rs.getBoolean("published"));
        c.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return c;
    };

    private static final RowMapper<HomeworkAssignment> ASSIGNMENT_MAPPER = (rs, rowNum) -> {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(rs.getObject("id", UUID.class));
        a.setTitle(rs.getString("title"));
        a.setInstructions(rs.getString("instructions"));
        a.setPublished(rs.getBoolean("published"));
        a.setSortOrder(rs.getInt("sort_order"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        String type = rs.getString("homework_type");
        if (type != null) a.setHomeworkType(HomeworkType.valueOf(type));
        String level = rs.getString("level");
        if (level != null) a.setLevel(HomeworkLevel.valueOf(level));
        String format = rs.getString("format");
        if (format != null) a.setFormat(HomeworkFormat.valueOf(format));
        a.setAudioUrl(rs.getString("audio_url"));
        a.setAudioFileId(rs.getObject("audio_file_id", UUID.class));
        String mediaKind = rs.getString("media_source_kind");
        if (mediaKind != null) a.setMediaSourceKind(MediaSourceKind.valueOf(mediaKind));
        java.sql.Timestamp revised = rs.getTimestamp("content_revised_at");
        if (revised != null) a.setContentRevisedAt(revised.toInstant());
        a.setLabels(readLabels(rs));
        return a;
    };

    public List<PresentationBlock> findPublishedPresentation() {
        String sql = "SELECT * FROM learning_presentation WHERE published = true ORDER BY sort_order";
        return jdbc.query(sql, Map.of(), PRESENTATION_MAPPER);
    }

    public List<PastClass> findPublishedPastClassesSince(LocalDate enrolledOn) {
        String sql = "SELECT * FROM past_classes WHERE published = true AND held_on >= :enrolledOn ORDER BY held_on DESC";
        return jdbc.query(sql, Map.of("enrolledOn", enrolledOn), PAST_CLASS_MAPPER);
    }

    /** Homework visible to a student: only assignments targeted at them (per-student model). */
    public List<HomeworkAssignment> findAssignmentsForUser(UUID userId) {
        String sql = """
                SELECT a.* FROM homework_assignments a
                JOIN homework_targets t ON t.assignment_id = a.id
                WHERE t.user_id = :userId
                ORDER BY a.sort_order, a.created_at
                """;
        return jdbc.query(sql, Map.of("userId", userId), ASSIGNMENT_MAPPER);
    }

    /** Maps each of the student's assigned homeworks to its owning unit (only those in a unit). */
    public record AssignmentUnit(UUID assignmentId, UUID unitId, String level, String subject,
                                 int position, int unitPosition) {}

    public List<AssignmentUnit> findAssignmentUnitsForUser(UUID userId) {
        String sql = """
                SELECT a.id AS assignment_id, u.id AS unit_id, u.level, u.subject, u.position,
                       a.unit_position AS content_position
                FROM homework_assignments a
                JOIN homework_targets t ON t.assignment_id = a.id
                JOIN units u ON u.id = a.unit_id
                WHERE t.user_id = :userId
                """;
        return jdbc.query(sql, Map.of("userId", userId), (rs, n) -> new AssignmentUnit(
                rs.getObject("assignment_id", UUID.class),
                rs.getObject("unit_id", UUID.class),
                rs.getString("level"),
                rs.getString("subject"),
                rs.getInt("position"),
                rs.getInt("content_position")));
    }

    public Optional<HomeworkAssignment> findPublishedAssignmentById(UUID id) {
        String sql = "SELECT * FROM homework_assignments WHERE id = :id AND published = true";
        return jdbc.query(sql, Map.of("id", id), ASSIGNMENT_MAPPER).stream().findFirst();
    }

    // --- admin (teacher backoffice) writes ----------------------------------

    public List<HomeworkAssignment> findAllAssignments() {
        return jdbc.query("SELECT * FROM homework_assignments ORDER BY created_at DESC",
                Map.of(), ASSIGNMENT_MAPPER);
    }

    public Optional<HomeworkAssignment> findAssignmentById(UUID id) {
        return jdbc.query("SELECT * FROM homework_assignments WHERE id = :id",
                Map.of("id", id), ASSIGNMENT_MAPPER).stream().findFirst();
    }

    public UUID insertAssignment(String title, String instructions,
                                  HomeworkType homeworkType, HomeworkLevel level, HomeworkFormat format,
                                  String audioUrl, UUID audioFileId, MediaSourceKind mediaSourceKind,
                                  List<String> labels) {
        UUID id = UUID.randomUUID();
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("instructions", instructions);
        params.put("homeworkType", homeworkType == null ? null : homeworkType.name());
        params.put("level", level == null ? null : level.name());
        params.put("format", (format == null ? HomeworkFormat.MANUAL : format).name());
        params.put("audioUrl", audioUrl);
        params.put("audioFileId", audioFileId);
        params.put("mediaSourceKind", mediaSourceKind == null ? null : mediaSourceKind.name());
        params.put("labels", textArrayValue(labels));
        jdbc.update("""
                INSERT INTO homework_assignments (id, title, instructions, homework_type, level, format,
                                                  audio_url, audio_file_id, media_source_kind, labels, published, sort_order)
                VALUES (:id, :title, :instructions, :homeworkType, :level, :format,
                        :audioUrl, :audioFileId, :mediaSourceKind, :labels, true, 0)
                """, params);
        return id;
    }

    public int updateAssignment(UUID id, String title, String instructions,
                                HomeworkType homeworkType, HomeworkLevel level, HomeworkFormat format,
                                String audioUrl, UUID audioFileId, MediaSourceKind mediaSourceKind,
                                List<String> labels) {
        return updateAssignment(id, title, instructions, homeworkType, level, format,
                audioUrl, audioFileId, mediaSourceKind, labels, null);
    }

    public int updateAssignment(UUID id, String title, String instructions,
                                HomeworkType homeworkType, HomeworkLevel level, HomeworkFormat format,
                                String audioUrl, UUID audioFileId, MediaSourceKind mediaSourceKind,
                                List<String> labels, java.time.Instant contentRevisedAt) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("instructions", instructions);
        params.put("homeworkType", homeworkType == null ? null : homeworkType.name());
        params.put("level", level == null ? null : level.name());
        params.put("format", (format == null ? HomeworkFormat.MANUAL : format).name());
        params.put("audioUrl", audioUrl);
        params.put("audioFileId", audioFileId);
        params.put("mediaSourceKind", mediaSourceKind == null ? null : mediaSourceKind.name());
        params.put("labels", textArrayValue(labels));
        params.put("contentRevisedAt", contentRevisedAt == null ? null : java.sql.Timestamp.from(contentRevisedAt));
        if (contentRevisedAt == null) {
            return jdbc.update("""
                    UPDATE homework_assignments
                    SET title = :title, instructions = :instructions,
                        homework_type = :homeworkType, level = :level, format = :format,
                        audio_url = :audioUrl, audio_file_id = :audioFileId,
                        media_source_kind = :mediaSourceKind, labels = :labels
                    WHERE id = :id
                    """, params);
        }
        return jdbc.update("""
                UPDATE homework_assignments
                SET title = :title, instructions = :instructions,
                    homework_type = :homeworkType, level = :level, format = :format,
                    audio_url = :audioUrl, audio_file_id = :audioFileId,
                    media_source_kind = :mediaSourceKind, labels = :labels,
                    content_revised_at = :contentRevisedAt
                WHERE id = :id
                """, params);
    }

    /** Locks the assignment row for a submit vs teacher-edit race. */
    public Optional<HomeworkAssignment> lockAssignment(UUID id) {
        return jdbc.query(
                "SELECT * FROM homework_assignments WHERE id = :id FOR UPDATE",
                Map.of("id", id), ASSIGNMENT_MAPPER).stream().findFirst();
    }

    public int deleteAssignment(UUID id) {
        return jdbc.update("DELETE FROM homework_assignments WHERE id = :id", Map.of("id", id));
    }

    /** Teacher-only metadata; does not bump {@code content_revised_at}. */
    public int updateLabels(UUID id, List<String> labels) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        params.put("labels", textArrayValue(labels));
        return jdbc.update(
                "UPDATE homework_assignments SET labels = :labels WHERE id = :id",
                params);
    }

    private static List<String> readLabels(java.sql.ResultSet rs) throws SQLException {
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

    private static Object textArrayValue(List<String> labels) {
        String[] values = labels == null || labels.isEmpty() ? new String[0] : labels.toArray(String[]::new);
        return new AbstractSqlTypeValue() {
            @Override
            protected Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
                return con.createArrayOf("text", values);
            }
        };
    }
}
