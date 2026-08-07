package com.kuky.backend.units;

import com.kuky.backend.units.dto.UnitContentRef;
import com.kuky.backend.units.exception.InvalidContentOrderException;
import com.kuky.backend.units.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class UnitContentOrderServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UnitService unitService;

    private UUID unitId;
    private UUID presentationId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM unit_assignments");
        jdbcTemplate.execute("DELETE FROM presentations");
        jdbcTemplate.execute("DELETE FROM homework_assignments");
        jdbcTemplate.execute("DELETE FROM units");

        unitId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO units (id, level, subject, position) VALUES (?, 'A1', 'Order', 0)",
                unitId);
        presentationId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO presentations (id, title, unit_id, unit_position)
                VALUES (?, 'P1', ?, 0)
                """, presentationId, unitId);
    }

    @Test
    void reorderContentsRejectsNonPermutation() {
        assertThatThrownBy(() -> unitService.reorderContents(unitId, List.of(
                new UnitContentRef("PRESENTATION", UUID.randomUUID())
        ))).isInstanceOf(InvalidContentOrderException.class);
    }

    @Test
    void reorderContentsRejectsExtraItem() {
        UUID extra = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO presentations (id, title) VALUES (?, 'Other')", extra);

        assertThatThrownBy(() -> unitService.reorderContents(unitId, List.of(
                new UnitContentRef("PRESENTATION", presentationId),
                new UnitContentRef("PRESENTATION", extra)
        ))).isInstanceOf(InvalidContentOrderException.class);
    }
}
