package com.kuky.backend.learning.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlankPassageParserTest {

    @Test
    void countsExactThreeUnderscoreTokens() {
        assertThat(BlankPassageParser.countBlanks("a ___ b ___ c")).isEqualTo(2);
        assertThat(BlankPassageParser.countBlanks("____")).isEqualTo(0); // four underscores — not a blank
        assertThat(BlankPassageParser.countBlanks("a__b")).isEqualTo(0);
        assertThat(BlankPassageParser.countBlanks("")).isEqualTo(0);
    }

    @Test
    void splitsSegmentsAroundBlanks() {
        assertThat(BlankPassageParser.splitSegments("Hoy ___ voy"))
                .containsExactly("Hoy ", null, " voy");
    }
}
