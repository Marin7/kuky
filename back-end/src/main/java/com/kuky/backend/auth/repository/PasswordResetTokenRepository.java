package com.kuky.backend.auth.repository;

import com.kuky.backend.auth.model.PasswordResetToken;
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
public class PasswordResetTokenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PasswordResetTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PasswordResetToken> TOKEN_MAPPER = (rs, rowNum) -> {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(rs.getObject("id", UUID.class));
        t.setUserId(rs.getObject("user_id", UUID.class));
        t.setToken(rs.getString("token"));
        t.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        t.setUsed(rs.getBoolean("used"));
        t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return t;
    };

    public Optional<PasswordResetToken> findByToken(String token) {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = :token";
        List<PasswordResetToken> results = jdbc.query(sql, Map.of("token", token), TOKEN_MAPPER);
        return results.stream().findFirst();
    }

    public List<PasswordResetToken> findAllByUserIdAndUsedFalse(UUID userId) {
        String sql = "SELECT * FROM password_reset_tokens WHERE user_id = :userId AND used = false";
        return jdbc.query(sql, Map.of("userId", userId), TOKEN_MAPPER);
    }

    public PasswordResetToken save(PasswordResetToken token) {
        if (token.getId() == null) {
            Instant now = Instant.now();
            UUID id = UUID.randomUUID();
            String sql = """
                    INSERT INTO password_reset_tokens (id, user_id, token, expires_at, used, created_at)
                    VALUES (:id, :userId, :token, :expiresAt, :used, :createdAt)
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("userId", token.getUserId())
                    .addValue("token", token.getToken())
                    .addValue("expiresAt", Timestamp.from(token.getExpiresAt()))
                    .addValue("used", token.isUsed())
                    .addValue("createdAt", Timestamp.from(now));
            jdbc.update(sql, params);
            token.setId(id);
            token.setCreatedAt(now);
        } else {
            String sql = """
                    UPDATE password_reset_tokens
                    SET used = :used, expires_at = :expiresAt
                    WHERE id = :id
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", token.getId())
                    .addValue("used", token.isUsed())
                    .addValue("expiresAt", Timestamp.from(token.getExpiresAt()));
            jdbc.update(sql, params);
        }
        return token;
    }

    public List<PasswordResetToken> saveAll(List<PasswordResetToken> tokens) {
        tokens.forEach(this::save);
        return tokens;
    }
}
