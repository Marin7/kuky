package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.HomeworkAssignment;
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
        return a;
    };

    public List<PresentationBlock> findPublishedPresentation() {
        String sql = "SELECT * FROM learning_presentation WHERE published = true ORDER BY sort_order";
        return jdbc.query(sql, Map.of(), PRESENTATION_MAPPER);
    }

    public List<PastClass> findPublishedPastClasses() {
        String sql = "SELECT * FROM past_classes WHERE published = true ORDER BY held_on DESC";
        return jdbc.query(sql, Map.of(), PAST_CLASS_MAPPER);
    }

    public List<HomeworkAssignment> findPublishedAssignments() {
        String sql = "SELECT * FROM homework_assignments WHERE published = true ORDER BY sort_order";
        return jdbc.query(sql, Map.of(), ASSIGNMENT_MAPPER);
    }

    public Optional<HomeworkAssignment> findPublishedAssignmentById(UUID id) {
        String sql = "SELECT * FROM homework_assignments WHERE id = :id AND published = true";
        return jdbc.query(sql, Map.of("id", id), ASSIGNMENT_MAPPER).stream().findFirst();
    }
}
