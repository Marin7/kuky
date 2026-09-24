package com.kuky.backend.preferences.repository;

import com.kuky.backend.preferences.model.EmailPreferenceType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-account email preferences, stored as one boolean column per option on {@code users}.
 *
 * <p>The enum-to-column mapping lives here and nowhere else. At a third option this should
 * become a {@code user_email_preferences} key/value table (see {@code research.md} R1).
 */
@Repository
public class EmailPreferencesRepository {

    private static final Map<EmailPreferenceType, String> COLUMNS =
            new EnumMap<>(Map.of(EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, "email_on_homework_assigned"));

    private final NamedParameterJdbcTemplate jdbc;

    public EmailPreferencesRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A student who has opted in to a given email and can actually receive it. */
    public record Recipient(UUID userId, String email, String firstName) {}

    private static String column(EmailPreferenceType type) {
        String column = COLUMNS.get(type);
        if (column == null) {
            throw new IllegalStateException("No column mapped for email preference " + type);
        }
        return column;
    }

    /** Every supported preference with its effective value for this account. */
    public Map<EmailPreferenceType, Boolean> findByEmail(String email) {
        Map<EmailPreferenceType, Boolean> out = new EnumMap<>(EmailPreferenceType.class);
        String columns = COLUMNS.values().stream().reduce((a, b) -> a + ", " + b).orElseThrow();
        jdbc.query("SELECT " + columns + " FROM users WHERE lower(email) = lower(:email)",
                Map.of("email", email), rs -> {
                    for (EmailPreferenceType type : EmailPreferenceType.values()) {
                        out.put(type, rs.getBoolean(column(type)));
                    }
                });
        // No row (deleted account mid-session) reads as every option off.
        for (EmailPreferenceType type : EmailPreferenceType.values()) {
            out.putIfAbsent(type, false);
        }
        return out;
    }

    /** Sets one preference for this account. Returns the number of rows updated (0 if no such user). */
    public int update(String email, EmailPreferenceType type, boolean enabled) {
        return jdbc.update(
                "UPDATE users SET " + column(type) + " = :enabled, updated_at = NOW() "
                        + "WHERE lower(email) = lower(:email)",
                new MapSqlParameterSource()
                        .addValue("enabled", enabled)
                        .addValue("email", email));
    }

    /**
     * Of the given accounts, those that have opted in to {@code type} and can receive mail.
     *
     * <p>One query rather than a per-user lookup. {@code status = 'ACTIVE'} implements
     * "an account still awaiting activation is not emailed".
     */
    public List<Recipient> findRecipientsFor(Collection<UUID> userIds, EmailPreferenceType type) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(
                "SELECT id, email, first_name FROM users "
                        + "WHERE id IN (:ids) AND " + column(type) + " = true AND status = 'ACTIVE'",
                Map.of("ids", userIds),
                (rs, n) -> new Recipient(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("first_name")));
    }
}
