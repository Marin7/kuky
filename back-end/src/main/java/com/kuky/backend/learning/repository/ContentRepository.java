package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkLevel;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.PastClass;
import com.kuky.backend.learning.model.PresentationBlock;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
        a.setDueOn(rs.getObject("due_on", LocalDate.class));
        a.setPublished(rs.getBoolean("published"));
        a.setSortOrder(rs.getInt("sort_order"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        String type = rs.getString("homework_type");
        if (type != null) a.setHomeworkType(HomeworkType.valueOf(type));
        String level = rs.getString("level");
        if (level != null) a.setLevel(HomeworkLevel.valueOf(level));
        String format = rs.getString("format");
        if (format != null) a.setFormat(HomeworkFormat.valueOf(format));
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

    public UUID insertAssignment(String title, String instructions, LocalDate dueOn,
                                  HomeworkType homeworkType, HomeworkLevel level, HomeworkFormat format) {
        UUID id = UUID.randomUUID();
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("instructions", instructions);
        params.put("dueOn", dueOn == null ? null : java.sql.Date.valueOf(dueOn));
        params.put("homeworkType", homeworkType == null ? null : homeworkType.name());
        params.put("level", level == null ? null : level.name());
        params.put("format", (format == null ? HomeworkFormat.MANUAL : format).name());
        jdbc.update("""
                INSERT INTO homework_assignments (id, title, instructions, due_on, homework_type, level, format, published, sort_order)
                VALUES (:id, :title, :instructions, :dueOn, :homeworkType, :level, :format, true, 0)
                """, params);
        return id;
    }

    public int updateAssignment(UUID id, String title, String instructions, LocalDate dueOn,
                                HomeworkType homeworkType, HomeworkLevel level, HomeworkFormat format) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("instructions", instructions);
        params.put("dueOn", dueOn == null ? null : java.sql.Date.valueOf(dueOn));
        params.put("homeworkType", homeworkType == null ? null : homeworkType.name());
        params.put("level", level == null ? null : level.name());
        params.put("format", (format == null ? HomeworkFormat.MANUAL : format).name());
        return jdbc.update("""
                UPDATE homework_assignments
                SET title = :title, instructions = :instructions, due_on = :dueOn,
                    homework_type = :homeworkType, level = :level, format = :format
                WHERE id = :id
                """, params);
    }

    public int deleteAssignment(UUID id) {
        return jdbc.update("DELETE FROM homework_assignments WHERE id = :id", Map.of("id", id));
    }
}
