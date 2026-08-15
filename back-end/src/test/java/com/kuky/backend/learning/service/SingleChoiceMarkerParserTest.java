package com.kuky.backend.learning.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SingleChoiceMarkerParserTest {

    @Test
    void consecutiveMarkersAreValid() {
        var r = SingleChoiceMarkerParser.parse("Elige: (1) ser (2) por (3) muy");
        assertThat(r.numbered()).isTrue();
        assertThat(r.valid()).isTrue();
        assertThat(r.n()).isEqualTo(3);
        assertThat(r.expectedNumbers()).containsExactly(1, 2, 3);
    }

    @Test
    void gapIsInvalid() {
        var r = SingleChoiceMarkerParser.parse("(1) uno (3) tres");
        assertThat(r.numbered()).isTrue();
        assertThat(r.valid()).isFalse();
        assertThat(r.errorMessage()).contains("consecutivos");
    }

    @Test
    void nonNumericParenthesesAreIgnored() {
        var r = SingleChoiceMarkerParser.parse("El verbo (ser) es...");
        assertThat(r.numbered()).isFalse();
        assertThat(r.valid()).isTrue();
        assertThat(SingleChoiceMarkerParser.hasMarkers("El verbo (ser) es...")).isFalse();
    }

    @Test
    void leadingZeroEqualsUnpadded() {
        assertThat(SingleChoiceMarkerParser.distinctNumbers("(01) y (1)"))
                .containsExactly(1);
        var r = SingleChoiceMarkerParser.parse("Solo (01)");
        assertThat(r.numbered()).isTrue();
        assertThat(r.valid()).isTrue();
        assertThat(r.n()).isEqualTo(1);
    }

    @Test
    void moreThanTwentyIsInvalid() {
        StringBuilder prompt = new StringBuilder();
        for (int i = 1; i <= 21; i++) {
            prompt.append('(').append(i).append(") ");
        }
        var r = SingleChoiceMarkerParser.parse(prompt.toString());
        assertThat(r.valid()).isFalse();
        assertThat(r.errorMessage()).contains("20");
    }

    @Test
    void startAtTwoIsInvalid() {
        var r = SingleChoiceMarkerParser.parse("(2) solo");
        assertThat(r.valid()).isFalse();
    }

    @Test
    void duplicateOneIsSameItem() {
        var r = SingleChoiceMarkerParser.parse("(1) a (1) b");
        assertThat(r.valid()).isTrue();
        assertThat(r.n()).isEqualTo(1);
    }

    @Test
    void unmarkedPromptIsClassic() {
        var r = SingleChoiceMarkerParser.parse("El plural de lápiz");
        assertThat(r.numbered()).isFalse();
        assertThat(r.valid()).isTrue();
        assertThat(r.n()).isEqualTo(0);
    }
}
