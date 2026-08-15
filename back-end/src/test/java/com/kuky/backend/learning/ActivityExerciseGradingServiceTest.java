package com.kuky.backend.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.SubmitExerciseRequest.AnswerDto;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ActivityExerciseGradingServiceTest {

    private ActivityExerciseGradingService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ActivityExerciseGradingService(
                mock(ActivityRepository.class),
                mock(ActivityQuestionRepository.class),
                mock(ActivitySubmissionRepository.class),
                mock(ActivityAnswerRepository.class),
                mock(PresentationRepository.class),
                mock(UserRepository.class),
                mapper);
    }

    @Test
    void dragDrop_studentStrip_reusableWhenSameIdOnTwoBlanks() {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion q = dragDrop(
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"el"},
                  {"id":"%s","label":"la"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id1, id1));
        var structure = service.studentQuestionsFor(List.of(q)).getFirst().structure();
        assertThat(structure.path("bankReusable").asBoolean()).isTrue();
        assertThat(structure.has("blanks")).isFalse();
        assertThat(structure.toString()).doesNotContain("correctBankIds");
    }

    @Test
    void dragDrop_duplicatePlacement_scoresBothBlanksWhenSharedKey() throws Exception {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion q = dragDrop(
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"el"},
                  {"id":"%s","label":"la"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id1, id1));
        var graded = service.gradeStructuredSubset(List.of(q), Map.of(
                q.getId(),
                new AnswerDto(q.getId(), List.of(), mapper.readTree(
                        """
                        {"placements":["%s","%s"]}
                        """.formatted(id1, id1)))));
        assertThat(graded.provisionalScorePercent()).isEqualTo(100);
        assertThat(graded.questionResults().getFirst().unitResults()).hasSize(2);
        assertThat(graded.questionResults().getFirst().unitResults().get(0).correct()).isTrue();
        assertThat(graded.questionResults().getFirst().unitResults().get(1).correct()).isTrue();
        assertThat(graded.questionResults().getFirst().unitResults().get(0).studentDisplay()).isEqualTo("el");
        assertThat(graded.questionResults().getFirst().unitResults().get(1).studentDisplay()).isEqualTo("el");
    }

    private static HomeworkQuestion dragDrop(String prompt, String structureJson) {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setKind(QuestionKind.DRAG_DROP);
        q.setPrompt(prompt);
        q.setStructureJson(structureJson);
        q.setOptions(List.of());
        return q;
    }
}
