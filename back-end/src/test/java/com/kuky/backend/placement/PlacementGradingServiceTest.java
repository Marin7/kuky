package com.kuky.backend.placement;

import com.kuky.backend.placement.dto.SectionResultResponse;
import com.kuky.backend.placement.dto.SubmitSectionRequest;
import com.kuky.backend.placement.dto.SubmitSectionRequest.AnswerDto;
import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.PlacementQuestionOption;
import com.kuky.backend.placement.model.QuestionKind;
import com.kuky.backend.placement.service.PlacementGradingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Mirrors learning.ExerciseGradingServiceTest — the grading rules are duplicated on purpose (research.md §1). */
class PlacementGradingServiceTest {

    private final PlacementGradingService service = new PlacementGradingService();

    private static PlacementQuestionOption option(String label, boolean correct) {
        PlacementQuestionOption o = new PlacementQuestionOption();
        o.setId(UUID.randomUUID());
        o.setLabel(label);
        o.setCorrect(correct);
        return o;
    }

    private static PlacementQuestion question(QuestionKind kind, List<PlacementQuestionOption> options) {
        PlacementQuestion q = new PlacementQuestion();
        q.setId(UUID.randomUUID());
        q.setKind(kind);
        q.setPrompt("¿…?");
        q.setOptions(options);
        return q;
    }

    private SectionResultResponse.QuestionResultDto grade(PlacementQuestion q, AnswerDto answer) {
        PlacementGradingService.GradedSection graded = service.grade(
                List.of(q), new SubmitSectionRequest(List.of(answer)));
        return graded.questionResults().get(0);
    }

    // --- single choice -------------------------------------------------------

    @Test
    void singleChoiceCorrect() {
        PlacementQuestionOption a = option("los lápizes", false);
        PlacementQuestionOption b = option("los lápices", true);
        PlacementQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        var r = grade(q, new AnswerDto(q.getId(), List.of(b.getId()), null));
        assertThat(r.score()).isEqualTo(1.0);
        assertThat(r.correct()).isTrue();
    }

    @Test
    void singleChoiceWrong() {
        PlacementQuestionOption a = option("los lápizes", false);
        PlacementQuestionOption b = option("los lápices", true);
        PlacementQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        var r = grade(q, new AnswerDto(q.getId(), List.of(a.getId()), null));
        assertThat(r.score()).isEqualTo(0.0);
    }

    // --- multi choice (partial credit) --------------------------------------

    @Test
    void multiChoiceAllCorrect() {
        PlacementQuestionOption o0 = option("a", true);
        PlacementQuestionOption o1 = option("b", true);
        PlacementQuestionOption o2 = option("c", false);
        PlacementQuestionOption o3 = option("d", false);
        PlacementQuestion q = question(QuestionKind.MULTI_CHOICE, List.of(o0, o1, o2, o3));
        var r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o1.getId()), null));
        assertThat(r.score()).isEqualTo(1.0);
    }

    @Test
    void multiChoicePartialCredit() {
        PlacementQuestionOption o0 = option("a", true);
        PlacementQuestionOption o1 = option("b", true);
        PlacementQuestionOption o2 = option("c", false);
        PlacementQuestionOption o3 = option("d", false);
        PlacementQuestion q = question(QuestionKind.MULTI_CHOICE, List.of(o0, o1, o2, o3));
        // one correct selected (o0), one incorrect selected (o2): rightDecisions = o0 + o3 = 2/4
        var r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o2.getId()), null));
        assertThat(r.score()).isEqualTo(0.5);
        assertThat(r.correct()).isFalse();
    }

    // --- fill blank ------------------------------------------------------------

    @Test
    void fillBlankCaseInsensitiveMatch() {
        PlacementQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        var r = grade(q, new AnswerDto(q.getId(), List.of(), "Fui"));
        assertThat(r.score()).isEqualTo(1.0);
    }

    @Test
    void fillBlankTrimsWhitespace() {
        PlacementQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        var r = grade(q, new AnswerDto(q.getId(), List.of(), "  fui  "));
        assertThat(r.score()).isEqualTo(1.0);
    }

    @Test
    void fillBlankAccentMismatchIsWrong() {
        PlacementQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("compré", true)));
        var r = grade(q, new AnswerDto(q.getId(), List.of(), "compre"));
        assertThat(r.score()).isEqualTo(0.0);
    }

    @Test
    void unansweredQuestionScoresZero() {
        PlacementQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        PlacementGradingService.GradedSection graded = service.grade(
                List.of(q), new SubmitSectionRequest(List.of()));
        assertThat(graded.questionResults().get(0).score()).isEqualTo(0.0);
        assertThat(graded.scorePercent()).isEqualTo(0);
    }
}
