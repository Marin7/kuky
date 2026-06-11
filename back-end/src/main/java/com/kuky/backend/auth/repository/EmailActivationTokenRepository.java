package com.kuky.backend.auth.repository;

import com.kuky.backend.auth.model.EmailActivationToken;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailActivationTokenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public EmailActivationTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<EmailActivationToken> TOKEN_MAPPER = (rs, rowNum) -> {
        EmailActivationToken t = new EmailActivationToken();
        t.setId(rs.getObject("id", UUID.class));
        t.setUserId(rs.getObject("user_id", UUID.class));
        t.setToken(rs.getString("token"));
        t.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        t.setUsed(rs.getBoolean("used"));
        t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return t;
    };

    public Optional<EmailActivationToken> findByToken(String token) {
        return jdbc.query(
                "SELECT * FROM email_activation_tokens WHERE token = :token",
                Map.of("token", token),
                TOKEN_MAPPER
        ).stream().findFirst();
    }

    /** Marks all unused tokens for a user as used (call before issuing a new one). */
    public void invalidateAllForUser(UUID userId) {
        jdbc.update(
                "UPDATE email_activation_tokens SET used = true WHERE user_id = :userId AND used = false",
                Map.of("userId", userId)
        );
    }

    public EmailActivationToken save(EmailActivationToken token) {
        if (token.getId() == null) {
            Instant now = Instant.now();
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO email_activation_tokens (id, user_id, token, expires_at, used, created_at)
                    VALUES (:id, :userId, :token, :expiresAt, :used, :createdAt)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("userId", token.getUserId())
                            .addValue("token", token.getToken())
                            .addValue("expiresAt", Timestamp.from(token.getExpiresAt()))
                            .addValue("used", token.isUsed())
                            .addValue("createdAt", Timestamp.from(now)));
            token.setId(id);
            token.setCreatedAt(now);
        } else {
            jdbc.update(
                    "UPDATE email_activation_tokens SET used = :used WHERE id = :id",
                    Map.of("id", token.getId(), "used", token.isUsed()));
        }
        return token;
    }
}
