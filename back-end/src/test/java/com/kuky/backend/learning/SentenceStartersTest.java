package com.kuky.backend.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.service.SentenceStarters;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Normalisation, composition and limits from specs/057-sentence-starter-answer/data-model.md. */
class SentenceStartersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) throws Exception {
        return mapper.readTree(raw);
    }

    private String structure(String... starters) {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < starters.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"starter\":\"").append(starters[i]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    // --- starter normalisation -------------------------------------------------

    @Test
    void normalizeStarter_dropsTrailingEllipsis() {
        assertThat(SentenceStarters.normalizeStarter("porque...")).isEqualTo("porque");
        assertThat(SentenceStarters.normalizeStarter("  porque … ")).isEqualTo("porque");
        assertThat(SentenceStarters.normalizeStarter("Si yo fuera tú,")).isEqualTo("Si yo fuera tú,");
    }

    @Test
    void normalizeStructure_normalisesAndDropsBlankModelAnswers() throws Exception {
        String out = SentenceStarters.normalizeStructure(json(
                "{\"items\":[{\"starter\":\"porque...\",\"modelAnswer\":\" Porque sí. \",\"x\":1},"
                        + "{\"starter\":\"para\",\"modelAnswer\":\"  \"}]}"));
        assertThat(json(out)).isEqualTo(json(
                "{\"items\":[{\"starter\":\"porque\",\"modelAnswer\":\"Porque sí.\"},{\"starter\":\"para\"}]}"));
    }

    @Test
    void normalizeStructure_enforcesLimits() throws Exception {
        assertThatThrownBy(() -> SentenceStarters.normalizeStructure(json("{\"items\":[]}")))
                .isInstanceOf(IllegalArgumentException.class);
        String eleven = structure("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
        assertThatThrownBy(() -> SentenceStarters.normalizeStructure(json(eleven)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SentenceStarters.normalizeStructure(json(structure("..."))))
                .hasMessage("Cada inicio de frase necesita texto.");
        assertThatThrownBy(() -> SentenceStarters.normalizeStructure(json(structure("a".repeat(81)))))
                .isInstanceOf(IllegalArgumentException.class);
        String longModel = "{\"items\":[{\"starter\":\"por\",\"modelAnswer\":\"" + "a".repeat(301) + "\"}]}";
        assertThatThrownBy(() -> SentenceStarters.normalizeStructure(json(longModel)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void studentStructure_hidesModelAnswers() {
        String stored = "{\"items\":[{\"starter\":\"porque\",\"modelAnswer\":\"Porque sí.\"}]}";
        assertThat(SentenceStarters.studentStructure(stored).toString())
                .isEqualTo("{\"starters\":[\"porque\"]}");
    }

    // --- composition -------------------------------------------------------------

    @Test
    void compose_joinsWithSpaceUnlessPunctuation() {
        assertThat(SentenceStarters.compose("porque", " quiere ir.")).isEqualTo("porque quiere ir.");
        assertThat(SentenceStarters.compose("Si yo fuera tú", ", iría.")).isEqualTo("Si yo fuera tú, iría.");
        assertThat(SentenceStarters.compose("para", "ver\n a su\r\nfamilia")).isEqualTo("para ver a su familia");
    }

    @Test
    void composeAnswer_buildsLinesAndSnapshot() throws Exception {
        String stored = "{\"items\":[{\"starter\":\"porque\",\"modelAnswer\":\"Porque sí.\"},{\"starter\":\"para\"}]}";
        SentenceStarters.Composed c = SentenceStarters.composeAnswer(stored, List.of("quiere.", "verla."));
        assertThat(c.answerText()).isEqualTo("porque quiere.\npara verla.");
        assertThat(json(c.answerJson())).isEqualTo(json(
                "{\"starters\":[\"porque\",\"para\"],\"modelAnswers\":[\"Porque sí.\",null],"
                        + "\"continuations\":[\"quiere.\",\"verla.\"]}"));
        assertThat(SentenceStarters.snapshotStarters(c.answerJson())).containsExactly("porque", "para");
        assertThat(SentenceStarters.snapshotModelAnswers(c.answerJson())).containsExactly("Porque sí.", null);
    }

    @Test
    void composeAnswer_rejectsBadInput() {
        String stored = structure("porque", "para");
        assertThatThrownBy(() -> SentenceStarters.composeAnswer(stored, List.of("x")))
                .hasMessage("Las respuestas no coinciden con las preguntas actuales.");
        assertThatThrownBy(() -> SentenceStarters.composeAnswer(stored, Arrays.asList("x", "  ")))
                .hasMessage("Debes responder a todas las preguntas.");
        assertThatThrownBy(() -> SentenceStarters.composeAnswer(stored, List.of("x", "a".repeat(501))))
                .hasMessage("Una de las respuestas es demasiado larga.");
        String five = structure("a", "b", "c", "d", "e");
        List<String> long5 = List.of("a".repeat(450), "a".repeat(450), "a".repeat(450), "a".repeat(450), "a".repeat(450));
        assertThatThrownBy(() -> SentenceStarters.composeAnswer(five, long5))
                .hasMessage("Una de las respuestas es demasiado larga.");
    }

    @Test
    void continuationsFrom_readsArrayOrEmpty() throws Exception {
        assertThat(SentenceStarters.continuationsFrom(json("{\"continuations\":[\"a\",\"b\"]}"))).containsExactly("a", "b");
        assertThat(SentenceStarters.continuationsFrom(null)).isEmpty();
        assertThat(SentenceStarters.snapshotStarters("{}")).isNull();
    }
}
