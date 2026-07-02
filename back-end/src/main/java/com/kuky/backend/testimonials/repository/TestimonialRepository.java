package com.kuky.backend.testimonials.repository;

import com.kuky.backend.testimonials.model.Testimonial;
import com.kuky.backend.testimonials.model.TestimonialStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TestimonialRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TestimonialRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Testimonial> TESTIMONIAL_MAPPER = (rs, rowNum) -> {
        Testimonial t = new Testimonial();
        t.setId(rs.getObject("id", UUID.class));
        t.setUserId(rs.getObject("user_id", UUID.class));
        t.setStudentName(rs.getString("student_name"));
        t.setText(rs.getString("text"));
        t.setStatus(TestimonialStatus.valueOf(rs.getString("status")));
        t.setDisplayOrder(rs.getInt("display_order"));
        t.setSubmittedAt(rs.getTimestamp("submitted_at").toInstant());
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        t.setReviewedAt(reviewedAt == null ? null : reviewedAt.toInstant());
        return t;
    };

    public Optional<Testimonial> findById(UUID id) {
        return jdbc.query("SELECT * FROM testimonials WHERE id = :id", Map.of("id", id), TESTIMONIAL_MAPPER)
                .stream().findFirst();
    }

    public List<Testimonial> findApproved() {
        return jdbc.query(
                "SELECT * FROM testimonials WHERE status = 'APPROVED' ORDER BY display_order",
                Map.of(), TESTIMONIAL_MAPPER);
    }

    public Optional<Testimonial> findByUserId(UUID userId) {
        return jdbc.query("SELECT * FROM testimonials WHERE user_id = :userId", Map.of("userId", userId),
                        TESTIMONIAL_MAPPER)
                .stream().findFirst();
    }

    /** Submit or resubmit: one row per student — replaces text and resets to PENDING. */
    public Testimonial upsertByUser(UUID userId, String studentName, String text) {
        String sql = """
                INSERT INTO testimonials (id, user_id, student_name, text, status, submitted_at, reviewed_at)
                VALUES (:id, :userId, :studentName, :text, 'PENDING', NOW(), NULL)
                ON CONFLICT (user_id) DO UPDATE SET
                    student_name = EXCLUDED.student_name,
                    text         = EXCLUDED.text,
                    status       = 'PENDING',
                    submitted_at = NOW(),
                    reviewed_at  = NULL
                RETURNING *
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("userId", userId)
                .addValue("studentName", studentName)
                .addValue("text", text), TESTIMONIAL_MAPPER);
    }

    public List<Testimonial> findAll() {
        return jdbc.query("SELECT * FROM testimonials ORDER BY display_order", Map.of(), TESTIMONIAL_MAPPER);
    }

    public Testimonial setStatus(UUID id, TestimonialStatus status) {
        String sql = """
                UPDATE testimonials SET status = :status, reviewed_at = NOW()
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name()), TESTIMONIAL_MAPPER);
    }

    public Testimonial setText(UUID id, String text) {
        String sql = "UPDATE testimonials SET text = :text WHERE id = :id RETURNING *";
        return jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("text", text), TESTIMONIAL_MAPPER);
    }

    @Transactional
    public void reorder(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            jdbc.update("UPDATE testimonials SET display_order = :order WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("order", i)
                            .addValue("id", orderedIds.get(i)));
        }
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM testimonials WHERE id = :id", Map.of("id", id));
    }
}
