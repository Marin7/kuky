package com.kuky.backend.units;

import com.kuky.backend.AbstractIntegrationTest;
import com.kuky.backend.units.dto.UnitSummary;
import com.kuky.backend.units.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository-level checks of units (PDF + page activities + assignees) against a real database. */
class UnitRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UnitRepository unitRepository;

    private UUID studentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM unit_assignments");
        jdbcTemplate.execute("DELETE FROM presentations WHERE unit_id IS NOT NULL");
        jdbcTemplate.execute("DELETE FROM units");
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'unit-repo-test@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO NOTHING
                """);
        studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'unit-repo-test@kuky.es'", UUID.class);
    }

    private UUID insertUnit(String subject) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO units (id, level, subject, position) VALUES (?, 'A1', ?, 0)", id, subject);
        return id;
    }

    private UUID insertOwnedPdf(UUID unitId, int pageCount) {
        UUID presentationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO presentations (id, title, unit_id) VALUES (?, 'PDF', ?)",
                presentationId, unitId);
        jdbcTemplate.update("""
                INSERT INTO presentation_files
                    (id, presentation_id, original_name, display_name, content_type, byte_size, page_count)
                VALUES (gen_random_uuid(), ?, 'u.pdf', 'u.pdf', 'application/pdf', 1, ?)
                """, presentationId, pageCount);
        return presentationId;
    }

    private void insertActivity(UUID presentationId, int page) {
        jdbcTemplate.update("""
                INSERT INTO activities (id, presentation_id, page, format) VALUES (gen_random_uuid(), ?, ?, 'MANUAL')
                """, presentationId, page);
    }

    @Test
    void summaryReportsPdfPagesAndActivityCount() {
        UUID unitId = insertUnit("Con PDF");
        UUID pdf = insertOwnedPdf(unitId, 12);
        insertActivity(pdf, 3);
        insertActivity(pdf, 7);
        UUID empty = insertUnit("Sin PDF");

        List<UnitSummary> summaries = unitRepository.listSummaries();

        UnitSummary withPdf = summaries.stream().filter(u -> u.id().equals(unitId)).findFirst().orElseThrow();
        assertThat(withPdf.hasPdf()).isTrue();
        assertThat(withPdf.pageCount()).isEqualTo(12);
        assertThat(withPdf.activityCount()).isEqualTo(2);
        UnitSummary noPdf = summaries.stream().filter(u -> u.id().equals(empty)).findFirst().orElseThrow();
        assertThat(noPdf.hasPdf()).isFalse();
        assertThat(noPdf.activityCount()).isZero();
    }

    @Test
    void deletingAUnitDeletesItsPdfAndActivities() {
        UUID unitId = insertUnit("Borrar");
        UUID pdf = insertOwnedPdf(unitId, 4);
        insertActivity(pdf, 2);

        unitRepository.delete(unitId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presentations WHERE id = ?", Integer.class, pdf)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE presentation_id = ?", Integer.class, pdf)).isZero();
    }

    @Test
    void replaceAssigneesAddsAndRemovesStudents() {
        UUID unitId = insertUnit("Alumnos");

        unitRepository.replaceAssignees(unitId, List.of(studentId));
        assertThat(unitRepository.findAssignedStudents(unitId)).extracting(s -> s.id()).containsExactly(studentId);

        unitRepository.replaceAssignees(unitId, List.of());
        assertThat(unitRepository.findAssignedStudents(unitId)).isEmpty();
    }
}
