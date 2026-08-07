package com.kuky.backend.units;

import com.kuky.backend.units.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class UnitAssigneeHomeworkSyncTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UnitService unitService;

    private UUID studentId;
    private UUID unitId;
    private UUID homeworkId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM unit_assignments");
        jdbcTemplate.execute("DELETE FROM homework_targets");
        jdbcTemplate.execute("DELETE FROM homework_submissions");
        jdbcTemplate.execute("DELETE FROM homework_assignments");
        jdbcTemplate.execute("DELETE FROM presentations");
        jdbcTemplate.execute("DELETE FROM units");
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'unit-hw-sync@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO UPDATE SET role = 'STUDENT', status = 'ACTIVE'
                """);
        studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'unit-hw-sync@kuky.es'", UUID.class);

        unitId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO units (id, level, subject, position) VALUES (?, 'A1', 'Sync', 0)",
                unitId);
        homeworkId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, unit_id, unit_position, published)
                VALUES (?, 'HW Sync', 'Instrucciones', ?, 0, true)
                """, homeworkId, unitId);
    }

    @Test
    void assigningUnitTargetsAllUnitHomeworks() {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Integer.class, homeworkId, studentId);
        assertThat(before).isZero();

        unitService.setAssignees(unitId, List.of(studentId));

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Integer.class, homeworkId, studentId);
        assertThat(after).isEqualTo(1);
    }

    @Test
    void unassigningUnitRemovesUnitHomeworkTargets() {
        unitService.setAssignees(unitId, List.of(studentId));
        unitService.setAssignees(unitId, List.of());

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Integer.class, homeworkId, studentId);
        assertThat(after).isZero();
    }

    @Test
    void addingHomeworkToAssignedUnitTargetsExistingAssignees() {
        unitService.setAssignees(unitId, List.of(studentId));

        UUID hw2 = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, published)
                VALUES (?, 'HW2', 'Instrucciones', true)
                """, hw2);

        unitService.setHomeworks(unitId, List.of(homeworkId, hw2));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Integer.class, hw2, studentId);
        assertThat(count).isEqualTo(1);
    }
}
