package com.kuky.backend.learning;

import com.kuky.backend.learning.service.HomeworkDueDates;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeworkDueDatesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);

    @Test
    void requireNotPast_allowsNullAndTodayAndFuture() {
        assertThatCode(() -> HomeworkDueDates.requireNotPast(null, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> HomeworkDueDates.requireNotPast(TODAY, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> HomeworkDueDates.requireNotPast(TODAY.plusDays(1), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void requireNotPast_rejectsYesterday() {
        assertThatThrownBy(() -> HomeworkDueDates.requireNotPast(TODAY.minusDays(1), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
