package com.kuky.backend.learning;

import com.kuky.backend.AbstractIntegrationTest;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.preferences.model.EmailPreferenceType;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The database-level guarantee the "new homework" email rests on: an insert into
 * {@code homework_targets} is reported exactly once, and never again for the same pair.
 *
 * <p>This is the automated form of quickstart scenario S7 (re-saving an assignment must
 * send nothing) and the regression guard for FR-013.
 */
class NewHomeworkGrantIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HomeworkTargetRepository targetRepository;
    @Autowired private EmailPreferencesRepository preferencesRepository;

    private UUID studentId;
    private UUID otherStudentId;
    private UUID pendingStudentId;
    private UUID assignmentId;

    @BeforeEach
    void setUp() {
        studentId = insertStudent("ACTIVE", true);
        otherStudentId = insertStudent("ACTIVE", false);
        pendingStudentId = insertStudent("PENDING", true);
        assignmentId = insertAssignment();
    }

    private UUID insertStudent(String status, boolean optedIn) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent,
                                   email_on_homework_assigned)
                VALUES (?, ?, 'hash', ?, 'STUDENT', true, ?)
                """, id, "grant-" + id + "@example.com", status, optedIn);
        return id;
    }

    private UUID insertAssignment() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, homework_type, level, format)
                VALUES (?, 'Los verbos reflexivos', 'x', 'WRITE', 'A1', 'MANUAL')
                """, id);
        return id;
    }

    @Test
    void firstGrantIsReported_andTheSameGrantIsNeverReportedAgain() {
        List<UUID> first = targetRepository.addTargets(assignmentId, List.of(studentId));
        assertThat(first).containsExactly(studentId);

        List<UUID> second = targetRepository.addTargets(assignmentId, List.of(studentId));
        assertThat(second).isEmpty();
    }

    @Test
    void reSavingAnUnchangedAssigneeListReportsNothing() {
        assertThat(targetRepository.replaceTargets(assignmentId, List.of(studentId, otherStudentId)))
                .containsExactlyInAnyOrder(studentId, otherStudentId);

        // The teacher re-saves without changing anything.
        assertThat(targetRepository.replaceTargets(assignmentId, List.of(studentId, otherStudentId)))
                .isEmpty();
    }

    @Test
    void addingOneStudentToAnExistingAssignmentReportsOnlyThatStudent() {
        targetRepository.replaceTargets(assignmentId, List.of(studentId));

        assertThat(targetRepository.replaceTargets(assignmentId, List.of(studentId, otherStudentId)))
                .containsExactly(otherStudentId);
    }

    @Test
    void removingThenReAddingIsAGenuineNewGrant() {
        targetRepository.addTargets(assignmentId, List.of(studentId));
        targetRepository.removeTargets(assignmentId, List.of(studentId));

        assertThat(targetRepository.addTargets(assignmentId, List.of(studentId)))
                .containsExactly(studentId);
    }

    @Test
    void alreadySeenRowsAreReportedToo_soUnitSourcedGrantsStillEmail() {
        // Unit-sourced targets are created already-seen (no in-site dot) but are still
        // newly available to the student, so they must be reported.
        List<UUID> reported = targetRepository.addTargets(
                assignmentId, List.of(studentId), java.time.Instant.now());

        assertThat(reported).containsExactly(studentId);
    }

    @Test
    void recipientLookupHonoursTheOptInFlagAndAccountStatus() {
        List<EmailPreferencesRepository.Recipient> recipients =
                preferencesRepository.findRecipientsFor(
                        List.of(studentId, otherStudentId, pendingStudentId),
                        EmailPreferenceType.NEW_HOMEWORK_ASSIGNED);

        // Opted-in + ACTIVE only: the opted-out student and the PENDING account are excluded.
        assertThat(recipients).extracting(EmailPreferencesRepository.Recipient::userId)
                .containsExactly(studentId);
    }

    @Test
    void newAccountsDefaultToOptedOut() {
        UUID fresh = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)
                """, fresh, "fresh-" + fresh + "@example.com");

        Boolean optedIn = jdbcTemplate.queryForObject(
                "SELECT email_on_homework_assigned FROM users WHERE id = ?", Boolean.class, fresh);

        assertThat(optedIn).isFalse();
    }
}
