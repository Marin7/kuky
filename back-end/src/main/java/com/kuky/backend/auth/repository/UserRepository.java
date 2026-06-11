package com.kuky.backend.auth.repository;

import com.kuky.backend.auth.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getObject("id", UUID.class));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setStatus(rs.getString("status"));
        u.setRole(rs.getString("role"));
        u.setGdprConsent(rs.getBoolean("gdpr_consent"));
        u.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        u.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return u;
    };

    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = :id";
        List<User> results = jdbc.query(sql, Map.of("id", id), USER_MAPPER);
        return results.stream().findFirst();
    }

    public Optional<User> findByEmailIgnoreCase(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email)";
        List<User> results = jdbc.query(sql, Map.of("email", email), USER_MAPPER);
        return results.stream().findFirst();
    }

    public boolean existsByEmailIgnoreCase(String email) {
        String sql = "SELECT COUNT(1) FROM users WHERE LOWER(email) = LOWER(:email)";
        Integer count = jdbc.queryForObject(sql, Map.of("email", email), Integer.class);
        return count != null && count > 0;
    }

    public List<User> findStudents() {
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' ORDER BY email";
        return jdbc.query(sql, Map.of(), USER_MAPPER);
    }

    /** Promote a user to ADMIN by email (idempotent). Returns rows affected. */
    public int promoteToAdminByEmail(String email) {
        String sql = "UPDATE users SET role = 'ADMIN', updated_at = NOW() "
                + "WHERE LOWER(email) = LOWER(:email) AND role <> 'ADMIN'";
        return jdbc.update(sql, Map.of("email", email));
    }

    public User save(User user) {
        if (user.getId() == null) {
            Instant now = Instant.now();
            UUID id = UUID.randomUUID();
            String sql = """
                    INSERT INTO users (id, email, password_hash, status, role, gdpr_consent, created_at, updated_at)
                    VALUES (:id, :email, :passwordHash, :status, :role, :gdprConsent, :createdAt, :updatedAt)
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("status", user.getStatus())
                    .addValue("role", user.getRole())
                    .addValue("gdprConsent", user.isGdprConsent())
                    .addValue("createdAt", Timestamp.from(now))
                    .addValue("updatedAt", Timestamp.from(now));
            jdbc.update(sql, params);
            user.setId(id);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
        } else {
            Instant now = Instant.now();
            String sql = """
                    UPDATE users
                    SET email = :email, password_hash = :passwordHash, status = :status, role = :role,
                        gdpr_consent = :gdprConsent, updated_at = :updatedAt
                    WHERE id = :id
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", user.getId())
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("status", user.getStatus())
                    .addValue("role", user.getRole())
                    .addValue("gdprConsent", user.isGdprConsent())
                    .addValue("updatedAt", Timestamp.from(now));
            jdbc.update(sql, params);
            user.setUpdatedAt(now);
        }
        return user;
    }
}
