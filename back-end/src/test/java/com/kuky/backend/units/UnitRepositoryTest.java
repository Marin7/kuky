package com.kuky.backend.units;

import com.kuky.backend.AbstractIntegrationTest;
import com.kuky.backend.units.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository-level verification of the unit-progress-for-student query against a real database. */
class UnitRepositoryTest extends AbstractIntegrationTest {

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
        jdbcTemplate.execute("DELETE FROM presentations WHERE unit_id IS NOT NULL");
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

    private UUID insertOwnedPdf(UUID unitId, int unitPosition) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO presentations (id, title, unit_id, unit_position)
                VALUES (?, 'PDF de la unidad', ?, ?)
                """, id, unitId, unitPosition);
        return id;
    }

    @Test
    void reorderContentsRewritesPdfAndHomeworkPositions() {
        UUID unitId = insertUnit("Orden", "A1", 0);
        UUID pdf = insertOwnedPdf(unitId, 0);
        UUID h1 = insertHomework(unitId, "H1");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 1 WHERE id = ?", h1);
        UUID h2 = insertHomework(unitId, "H2");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 2 WHERE id = ?", h2);

        unitRepository.reorderContents(unitId, List.of(
                new com.kuky.backend.units.dto.UnitContentRef("HOMEWORK", h1),
                new com.kuky.backend.units.dto.UnitContentRef("PDF", pdf),
                new com.kuky.backend.units.dto.UnitContentRef("HOMEWORK", h2)
        ));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(h1, pdf, h2);
        assertThat(members).extracting(UnitRepository.ContentMember::type)
                .containsExactly("HOMEWORK", "PDF", "HOMEWORK");
        assertThat(members).extracting(UnitRepository.ContentMember::unitPosition)
                .containsExactly(0, 1, 2);
    }

    @Test
    void setHomeworksAppendsNewcomersAndKeepsThePdfInPlace() {
        UUID unitId = insertUnit("Membresía", "A1", 0);
        UUID pdf = insertOwnedPdf(unitId, 0);
        UUID h1 = insertHomework(unitId, "H1");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 1 WHERE id = ?", h1);
        UUID h2 = insertHomework(null, "H2");

        unitRepository.setHomeworks(unitId, List.of(h1, h2));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(pdf, h1, h2);
    }

    @Test
    void setHomeworksDetachesAndCompactsRemaining() {
        UUID unitId = insertUnit("Detach", "A1", 0);
        UUID h1 = insertHomework(unitId, "H1");
        UUID pdf = insertOwnedPdf(unitId, 1);
        UUID h2 = insertHomework(unitId, "H2");
        jdbcTemplate.update("UPDATE homework_assignments SET unit_position = 2 WHERE id = ?", h2);

        unitRepository.setHomeworks(unitId, List.of(h2));

        List<UnitRepository.ContentMember> members = unitRepository.findContentMembers(unitId);
        assertThat(members).extracting(UnitRepository.ContentMember::id)
                .containsExactly(pdf, h2);
        assertThat(members).extracting(UnitRepository.ContentMember::unitPosition)
                .containsExactly(0, 1);

        Integer detachedUnit = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN unit_id IS NULL THEN 1 ELSE 0 END FROM homework_assignments WHERE id = ?",
                Integer.class, h1);
        assertThat(detachedUnit).isEqualTo(1);
    }

    @Test
    void deletingAUnitDeletesItsPdfButKeepsItsHomework() {
        UUID unitId = insertUnit("Borrar", "A1", 0);
        UUID pdf = insertOwnedPdf(unitId, 0);
        UUID h1 = insertHomework(unitId, "H1");

        unitRepository.delete(unitId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presentations WHERE id = ?", Integer.class, pdf)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_assignments WHERE id = ? AND unit_id IS NULL",
                Integer.class, h1)).isEqualTo(1);
    }
}
