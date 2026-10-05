package com.kuky.backend.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.dto.ExerciseResultResponse.UnitResultDto;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.service.QuestionScoring;
import com.kuky.backend.learning.service.SpotWrongWords;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tokenizer vectors from specs/054-spot-wrong-words/data-model.md + grading rules. */
class SpotWrongWordsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // --- tokenizer (must match front-end/src/lib/spotWrongWords.ts) ----------

    @Test
    void tokenize_dropsSpanishPunctuation() {
        assertThat(SpotWrongWords.tokenize("¿Dónde está el baño?"))
                .containsExactly("Dónde", "está", "el", "baño");
    }

    @Test
    void tokenize_keepsInternalApostropheAndHyphen() {
        assertThat(SpotWrongWords.tokenize("Yo sabo que l'hotel es bien-estar."))
                .containsExactly("Yo", "sabo", "que", "l'hotel", "es", "bien-estar");
    }

    @Test
    void tokenize_numbersAreWordsAndEmDashIsAGap() {
        assertThat(SpotWrongWords.tokenize("En 1990, mi madre —y yo— fuimos."))
                .containsExactly("En", "1990", "mi", "madre", "y", "yo", "fuimos");
    }

    @Test
    void tokenize_spacedHyphenIsAGap() {
        assertThat(SpotWrongWords.tokenize("rock - and")).containsExactly("rock", "and");
    }

    @Test
    void tokenize_typographicApostrophe() {
        assertThat(SpotWrongWords.tokenize("d’Artagnan")).containsExactly("d’Artagnan");
    }

    @Test
    void tokenize_repeatedWordsStaySeparate() {
        assertThat(SpotWrongWords.tokenize("la la la")).containsExactly("la", "la", "la");
    }

    @Test
    void tokenize_paragraphs() {
        assertThat(SpotWrongWords.tokenize("¡Hola!\n\nAdiós.")).containsExactly("Hola", "Adiós");
    }

    // --- grading -------------------------------------------------------------

    /** "Ayer yo sabo que mi hermana estás en Madrid." — errors at 2 (sabo) and 6 (estás). */
    private HomeworkQuestion question() {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setKind(QuestionKind.SPOT_WRONG_WORDS);
        q.setPrompt("Ayer yo sabo que mi hermana estás en Madrid.");
        q.setStructureJson("""
                {"errors":[
                  {"wordIndex":2,"word":"sabo","correction":"supe"},
                  {"wordIndex":6,"word":"estás","correction":null}
                ]}
                """);
        q.setOptions(List.of());
        return q;
    }

    private QuestionScoring.GradedAnswer grade(String answerJson) throws Exception {
        return SpotWrongWords.grade(question(), mapper.readTree(answerJson), mapper);
    }

    @Test
    void grade_allFound() throws Exception {
        QuestionScoring.GradedAnswer g = grade("{\"selected\":[6,2]}");
        assertThat(g.score()).isEqualTo(1.0);
        assertThat(g.answerJson()).isEqualTo("{\"selected\":[2,6]}");
    }

    @Test
    void grade_halfFoundPlusExtra_isHalfAndExtraIsNotPenalised() throws Exception {
        QuestionScoring.GradedAnswer g = grade("{\"selected\":[2,4]}");
        assertThat(g.score()).isEqualTo(0.5);
        assertThat(g.unitResults())
                .extracting(UnitResultDto::index, UnitResultDto::correct,
                        UnitResultDto::studentDisplay, UnitResultDto::label)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2, true, "sabo", null),
                        org.assertj.core.groups.Tuple.tuple(4, false, "mi", SpotWrongWords.EXTRA_LABEL),
                        org.assertj.core.groups.Tuple.tuple(6, false, null, null));
        assertThat(g.unitResults().get(0).expectedDisplay()).containsExactly("supe");
        assertThat(g.unitResults().get(1).expectedDisplay()).isEmpty();
        assertThat(g.unitResults().get(2).expectedDisplay()).isEmpty();
    }

    @Test
    void grade_nothingSelected_isZero() throws Exception {
        assertThat(grade("{}").score()).isEqualTo(0.0);
        assertThat(grade("{\"selected\":[]}").score()).isEqualTo(0.0);
    }

    @Test
    void grade_ignoresOutOfRangeDuplicateAndNonIntegerIndices() throws Exception {
        QuestionScoring.GradedAnswer g = grade("{\"selected\":[2,2,99,-1,\"6\",1.5]}");
        assertThat(g.score()).isEqualTo(0.5);
        assertThat(g.answerJson()).isEqualTo("{\"selected\":[2]}");
    }

    @Test
    void grade_moreSelectionsThanErrors_isRefused() {
        assertThatThrownBy(() -> grade("{\"selected\":[0,1,2]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("más palabras que errores");
    }
}
