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
        u.setUniversityLevel(rs.getString("university_level"));
        u.setGdprConsent(rs.getBoolean("gdpr_consent"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setUsername(rs.getString("username"));
        u.setAvatarImageId(rs.getObject("avatar_image_id", UUID.class));
        u.setTimezone(rs.getString("timezone"));
        u.setTimezoneManual(rs.getBoolean("timezone_is_manual"));
        u.setExtendedClassEligible(rs.getBoolean("extended_class_eligible"));
        u.setInterestsNote(rs.getString("interests_note"));
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

    public List<User> findUniversityStudents() {
        return jdbc.query("SELECT * FROM users WHERE role = 'UNIVERSITY_STUDENT' ORDER BY email",
                Map.of(), USER_MAPPER);
    }

    public List<User> findRegisteredUsers() {
        String sql = "SELECT * FROM users WHERE role = 'USER' ORDER BY email";
        return jdbc.query(sql, Map.of(), USER_MAPPER);
    }

    public List<UUID> findExtendedClassEligibleStudentIds() {
        String sql = "SELECT id FROM users WHERE role = 'STUDENT' AND extended_class_eligible = true";
        return jdbc.query(sql, Map.of(), (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    /** Grant student status by id (idempotent: no-op if already STUDENT). Returns rows affected. */
    public int promoteToStudentById(UUID id) {
        String sql = "UPDATE users SET role = 'STUDENT', updated_at = NOW() "
                + "WHERE id = :id AND role = 'USER'";
        return jdbc.update(sql, Map.of("id", id));
    }

    /** Revoke student status by id (idempotent: no-op if already USER). Returns rows affected. */
    public int revokeStudentById(UUID id) {
        String sql = "UPDATE users SET role = 'USER', updated_at = NOW() "
                + "WHERE id = :id AND role = 'STUDENT'";
        return jdbc.update(sql, Map.of("id", id));
    }

    public int grantUniversityStudentById(UUID id, String level) {
        return jdbc.update("UPDATE users SET role = 'UNIVERSITY_STUDENT', university_level = :level, updated_at = NOW() "
                + "WHERE id = :id AND role = 'USER'", Map.of("id", id, "level", level));
    }

    public int revokeUniversityStudentById(UUID id) {
        return jdbc.update("UPDATE users SET role = 'USER', university_level = NULL, updated_at = NOW() "
                + "WHERE id = :id AND role = 'UNIVERSITY_STUDENT'", Map.of("id", id));
    }

    public int updateUniversityLevelById(UUID id, String level) {
        return jdbc.update("UPDATE users SET university_level = :level, updated_at = NOW() "
                + "WHERE id = :id AND role = 'UNIVERSITY_STUDENT'", Map.of("id", id, "level", level));
    }

    /** Grant extended-class eligibility by id (idempotent: no-op if already eligible). Returns rows affected. */
    public int grantExtendedClassById(UUID id) {
        String sql = "UPDATE users SET extended_class_eligible = true, updated_at = NOW() "
                + "WHERE id = :id AND extended_class_eligible = false";
        return jdbc.update(sql, Map.of("id", id));
    }

    /** Revoke extended-class eligibility by id (idempotent: no-op if already ineligible). Returns rows affected. */
    public int revokeExtendedClassById(UUID id) {
        String sql = "UPDATE users SET extended_class_eligible = false, updated_at = NOW() "
                + "WHERE id = :id AND extended_class_eligible = true";
        return jdbc.update(sql, Map.of("id", id));
    }

    public boolean existsByUsernameIgnoreCase(String username, UUID excludeId) {
        String sql = "SELECT COUNT(1) FROM users WHERE LOWER(username) = LOWER(:username) AND id <> :excludeId";
        Integer count = jdbc.queryForObject(sql,
                Map.of("username", username, "excludeId", excludeId), Integer.class);
        return count != null && count > 0;
    }

    public void updateProfile(UUID id, String firstName, String lastName, String username) {
        String sql = """
                UPDATE users
                   SET first_name = :firstName,
                       last_name  = :lastName,
                       username   = :username,
                       updated_at = :updatedAt
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("firstName", firstName)
                .addValue("lastName", lastName)
                .addValue("username", username)
                .addValue("updatedAt", Timestamp.from(Instant.now())));
    }

    public void updateTimezone(UUID id, String zone, boolean manual) {
        String sql = "UPDATE users SET timezone = :zone, timezone_is_manual = :manual, updated_at = NOW() "
                + "WHERE id = :id";
        jdbc.update(sql, Map.of("id", id, "zone", zone, "manual", manual));
    }

    public void updateAvatar(UUID id, UUID avatarImageId) {
        String sql = "UPDATE users SET avatar_image_id = :avatarImageId, updated_at = NOW() WHERE id = :id";
        jdbc.update(sql, Map.of("id", id, "avatarImageId", avatarImageId));
    }

    public List<String> findInterestCodesByUserId(UUID userId) {
        String sql = "SELECT interest_code FROM user_interests WHERE user_id = :id ORDER BY interest_code";
        return jdbc.query(sql, Map.of("id", userId), (rs, rowNum) -> rs.getString("interest_code"));
    }

    public void replaceInterests(UUID userId, List<String> codes) {
        jdbc.update("DELETE FROM user_interests WHERE user_id = :id", Map.of("id", userId));
        if (codes == null || codes.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO user_interests (user_id, interest_code) VALUES (:userId, :code)";
        for (String code : codes) {
            jdbc.update(sql, Map.of("userId", userId, "code", code));
        }
    }

    public void updateInterestsNote(UUID userId, String note) {
        String sql = "UPDATE users SET interests_note = :note, updated_at = NOW() WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("note", note));
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
