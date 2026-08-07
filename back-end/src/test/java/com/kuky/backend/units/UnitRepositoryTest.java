package com.kuky.backend.units;

import com.kuky.backend.units.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository-level verification of the unit-progress-for-student query against a real database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class UnitRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UnitRepository unitRepository;

    private UUID studentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM unit_assignments");
        jdbcTemplate.execute("DELETE FROM homework_targets");
        jdbcTemplate.execute("DELETE FROM homework_submissions");
        jdbcTemplate.execute("DELETE FROM homework_assignments");
        jdbcTemplate.execute("DELETE FROM units");
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'unit-progress-test@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO NOTHING
                """);
        studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'unit-progress-test@kuky.es'", UUID.class);
    }

    private UUID insertUnit(String subject, String level, int position) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO units (id, level, subject, position) VALUES (?, ?, ?, ?)",
                id, level, subject, position);
        return id;
    }

    private void assignUnit(UUID unitId) {
        jdbcTemplate.update(
                "INSERT INTO unit_assignments (id, unit_id, user_id) VALUES (gen_random_uuid(), ?, ?)",
                unitId, studentId);
    }

    private UUID insertHomework(UUID unitId, String title) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, unit_id)
                VALUES (?, ?, 'Instrucciones', ?)
                """, id, title, unitId);
        return id;
    }

    private void targetHomework(UUID assignmentId) {
        jdbcTemplate.update(
                "INSERT INTO homework_targets (id, assignment_id, user_id) VALUES (gen_random_uuid(), ?, ?)",
                assignmentId, studentId);
    }

    private void submitHomework(UUID assignmentId, String status) {
        jdbcTemplate.update("""
                INSERT INTO homework_submissions (id, user_id, assignment_id, status)
                VALUES (gen_random_uuid(), ?, ?, ?)
                """, studentId, assignmentId, status);
    }

    @Test
    void unitWithNoTargetedHomeworkStillAppearsWithZeroTotals() {
        UUID unitId = insertUnit("Fonética", "A2", 0);
        assignUnit(unitId);
        // Homework filed under the unit organisationally, but never targeted at this student.
        insertHomework(unitId, "Tarea no asignada");

        List<UnitRepository.UnitProgressView> progress = unitRepository.findProgressForStudent(studentId);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).unitId()).isEqualTo(unitId);
        assertThat(progress.get(0).totalHomeworks()).isZero();
        assertThat(progress.get(0).completedHomeworks()).isZero();
    }

    @Test
    void unitCountsOnlyHomeworksTargetedAtTheStudent() {
        UUID unitId = insertUnit("Gramática", "B1", 0);
        assignUnit(unitId);

        UUID reviewed = insertHomework(unitId, "Tarea 1");
        targetHomework(reviewed);
        submitHomework(reviewed, "REVIEWED");

        UUID pending = insertHomework(unitId, "Tarea 2");
        targetHomework(pending);
        // No submission row: coalesces to PENDING.

        List<UnitRepository.UnitProgressView> progress = unitRepository.findProgressForStudent(studentId);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).totalHomeworks()).isEqualTo(2);
        assertThat(progress.get(0).completedHomeworks()).isEqualTo(1);
    }

    @Test
    void unitWithZeroAssignedHomeworksReturnsZeroTotals() {
        UUID unitId = insertUnit("Vocabulario", "A1", 0);
        assignUnit(unitId);

        List<UnitRepository.UnitProgressView> progress = unitRepository.findProgressForStudent(studentId);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).totalHomeworks()).isZero();
        assertThat(progress.get(0).completedHomeworks()).isZero();
    }

    private UUID insertPresentation(UUID unitId, String title, int unitPosition) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO presentations (id, title, unit_id, unit_position)
                VALUES (?, ?, ?, ?)
                """, id, title, unitId, unitPosition);
        return id;
    }

    @Test
    void reorderContentsRewritesMixedUnitPositions() {
        UUID unitId = insertUnit("Orden", "A1", 0);
        UUID p1 = insertPresentation(unitId, "P1", 0);
        UUID h1 = insertHomework(unitId, "H1");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 1 WHERE id = ?", h1);
        UUID p2 = insertPresentation(unitId, "P2", 2);

        unitRepository.reorderContents(unitId, List.of(
                new com.kuky.backend.units.dto.UnitContentRef("HOMEWORK", h1),
                new com.kuky.backend.units.dto.UnitContentRef("PRESENTATION", p2),
                new com.kuky.backend.units.dto.UnitContentRef("PRESENTATION", p1)
        ));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(h1, p2, p1);
        assertThat(members).extracting(UnitRepository.ContentMember::unitPosition)
                .containsExactly(0, 1, 2);
    }

    @Test
    void setPresentationsAppendsNewcomersAndKeepsRelativeOrder() {
        UUID unitId = insertUnit("Membresía", "A1", 0);
        UUID p1 = insertPresentation(unitId, "P1", 0);
        UUID h1 = insertHomework(unitId, "H1");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 1 WHERE id = ?", h1);

        UUID p2 = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO presentations (id, title) VALUES (?, ?)", p2, "P2");

        unitRepository.setPresentations(unitId, List.of(p1, p2));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(p1, h1, p2);
    }

    @Test
    void setPresentationsDetachesAndCompactsRemaining() {
        UUID unitId = insertUnit("Detach", "A1", 0);
        UUID p1 = insertPresentation(unitId, "P1", 0);
        UUID h1 = insertHomework(unitId, "H1");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 1 WHERE id = ?", h1);
        UUID p2 = insertPresentation(unitId, "P2", 2);

        unitRepository.setPresentations(unitId, List.of(p2));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(h1, p2);
        assertThat(members).extracting(UnitRepository.ContentMember::unitPosition)
                .containsExactly(0, 1);

        Integer detachedUnit = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN unit_id IS NULL THEN 1 ELSE 0 END FROM presentations WHERE id = ?",
                Integer.class, p1);
        assertThat(detachedUnit).isEqualTo(1);
    }
}
