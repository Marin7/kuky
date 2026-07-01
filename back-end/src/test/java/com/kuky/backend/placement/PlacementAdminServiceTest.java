package com.kuky.backend.placement;

import com.kuky.backend.placement.dto.AdminQuestionDto;
import com.kuky.backend.placement.dto.LevelThresholdDto;
import com.kuky.backend.placement.dto.PlacementConfigDto;
import com.kuky.backend.placement.dto.StudentEvaluationResponse;
import com.kuky.backend.placement.dto.UpsertQuestionRequest;
import com.kuky.backend.placement.dto.UpsertQuestionRequest.OptionInput;
import com.kuky.backend.placement.model.AttemptStatus;
import com.kuky.backend.placement.model.PlacementAttempt;
import com.kuky.backend.placement.model.PlacementAttemptSection;
import com.kuky.backend.placement.model.PlacementConfig;
import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.Skill;
import com.kuky.backend.placement.repository.PlacementAttemptRepository;
import com.kuky.backend.placement.repository.PlacementConfigRepository;
import com.kuky.backend.placement.repository.PlacementLevelThresholdRepository;
import com.kuky.backend.placement.repository.PlacementQuestionRepository;
import com.kuky.backend.placement.repository.PlacementWritingRepository;
import com.kuky.backend.placement.service.PlacementAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlacementAdminServiceTest {

    private PlacementConfigRepository configRepository;
    private PlacementQuestionRepository questionRepository;
    private PlacementAttemptRepository attemptRepository;
    private PlacementWritingRepository writingRepository;
    private PlacementLevelThresholdRepository levelThresholdRepository;
    private PlacementAdminService service;

    @BeforeEach
    void setUp() {
        configRepository = mock(PlacementConfigRepository.class);
        questionRepository = mock(PlacementQuestionRepository.class);
        attemptRepository = mock(PlacementAttemptRepository.class);
        writingRepository = mock(PlacementWritingRepository.class);
        levelThresholdRepository = mock(PlacementLevelThresholdRepository.class);
        service = new PlacementAdminService(configRepository, questionRepository, attemptRepository,
                writingRepository, levelThresholdRepository);

        when(questionRepository.create(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.findById(any())).thenAnswer(inv -> Optional.of(stubQuestion(inv.getArgument(0))));
    }

    private static PlacementQuestion stubQuestion(UUID id) {
        PlacementQuestion q = new PlacementQuestion();
        q.setId(id);
        return q;
    }

    private static UpsertQuestionRequest request(String kind, OptionInput... options) {
        return new UpsertQuestionRequest("GRAMMAR", kind, "¿…?", null, null, true, List.of(options));
    }

    // --- question validation ---------------------------------------------------

    @Test
    void singleChoiceWithExactlyOneCorrect_isAccepted() {
        var req = request("SINGLE_CHOICE", new OptionInput("a", true), new OptionInput("b", false));
        assertThatNoException().isThrownBy(() -> service.createQuestion(req));
    }

    @Test
    void singleChoiceWithZeroCorrect_isRejected() {
        var req = request("SINGLE_CHOICE", new OptionInput("a", false), new OptionInput("b", false));
        assertThatThrownBy(() -> service.createQuestion(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void singleChoiceWithTwoCorrect_isRejected() {
        var req = request("SINGLE_CHOICE", new OptionInput("a", true), new OptionInput("b", true));
        assertThatThrownBy(() -> service.createQuestion(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiChoiceWithAtLeastOneCorrect_isAccepted() {
        var req = request("MULTI_CHOICE", new OptionInput("a", true), new OptionInput("b", false));
        assertThatNoException().isThrownBy(() -> service.createQuestion(req));
    }

    @Test
    void multiChoiceWithNoCorrect_isRejected() {
        var req = request("MULTI_CHOICE", new OptionInput("a", false), new OptionInput("b", false));
        assertThatThrownBy(() -> service.createQuestion(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fillBlankWithAtLeastOneAcceptedAnswer_isAccepted() {
        var req = request("FILL_BLANK", new OptionInput("respuesta", true));
        assertThatNoException().isThrownBy(() -> service.createQuestion(req));
    }

    @Test
    void fillBlankWithNoAcceptedAnswers_isRejected() {
        var req = request("FILL_BLANK");
        assertThatThrownBy(() -> service.createQuestion(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listeningWithBothAudioSources_isRejected() {
        var req = new UpsertQuestionRequest("LISTENING", "SINGLE_CHOICE", "¿…?",
                "https://example.com/a.mp3", UUID.randomUUID(), true,
                List.of(new OptionInput("a", true), new OptionInput("b", false)));
        assertThatThrownBy(() -> service.createQuestion(req)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- config ----------------------------------------------------------------

    @Test
    void updateConfig_roundTripsValues() {
        PlacementConfig existing = new PlacementConfig();
        existing.setId(UUID.randomUUID());
        when(configRepository.get()).thenReturn(existing);
        when(configRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        PlacementConfigDto req = new PlacementConfigDto(600, 480, 420, 1200, "Escribe...");
        PlacementConfigDto result = service.updateConfig(req);

        assertThat(result.readingTimeSeconds()).isEqualTo(600);
        assertThat(result.writingPrompt()).isEqualTo("Escribe...");
        verify(configRepository).update(any());
    }

    @Test
    void updateConfig_rejectsNonPositiveTimeLimit() {
        when(configRepository.get()).thenReturn(new PlacementConfig());
        PlacementConfigDto req = new PlacementConfigDto(0, 480, 420, 1200, "");
        assertThatThrownBy(() -> service.updateConfig(req)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- level thresholds --------------------------------------------------------

    private static List<LevelThresholdDto> validThresholds() {
        return List.of(
                new LevelThresholdDto("A1", 0), new LevelThresholdDto("A2", 20),
                new LevelThresholdDto("B1", 40), new LevelThresholdDto("B2", 60),
                new LevelThresholdDto("C1", 75), new LevelThresholdDto("C2", 90));
    }

    @Test
    void updateLevelThresholds_monotonicSet_isAccepted() {
        when(levelThresholdRepository.findAll()).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.updateLevelThresholds(validThresholds()));
        verify(levelThresholdRepository).updateAll(any());
    }

    @Test
    void updateLevelThresholds_nonMonotonicSet_isRejected() {
        List<LevelThresholdDto> nonMonotonic = List.of(
                new LevelThresholdDto("A1", 0), new LevelThresholdDto("A2", 50),
                new LevelThresholdDto("B1", 30), new LevelThresholdDto("B2", 60),
                new LevelThresholdDto("C1", 75), new LevelThresholdDto("C2", 90));
        assertThatThrownBy(() -> service.updateLevelThresholds(nonMonotonic))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLevelThresholds_outOfRangeValue_isRejected() {
        List<LevelThresholdDto> req = List.of(
                new LevelThresholdDto("A1", 0), new LevelThresholdDto("A2", 20),
                new LevelThresholdDto("B1", 40), new LevelThresholdDto("B2", 60),
                new LevelThresholdDto("C1", 75), new LevelThresholdDto("C2", 150));
        assertThatThrownBy(() -> service.updateLevelThresholds(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLevelThresholds_missingLevel_isRejected() {
        List<LevelThresholdDto> req = List.of(
                new LevelThresholdDto("A1", 0), new LevelThresholdDto("A2", 20),
                new LevelThresholdDto("B1", 40), new LevelThresholdDto("B2", 60),
                new LevelThresholdDto("C1", 75));
        assertThatThrownBy(() -> service.updateLevelThresholds(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- reorder -----------------------------------------------------------------

    @Test
    void reorder_delegatesToRepository() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        service.reorder(Skill.READING, ids);
        verify(questionRepository).reorder(Skill.READING, ids);
    }

    // --- student evaluation aggregation (FR-015) --------------------------------

    @Test
    void getStudentEvaluation_combinesResultAndWritingIntoOneShape() {
        UUID studentId = UUID.randomUUID();
        PlacementAttempt attempt = new PlacementAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setOverallCefr("B1");
        attempt.setCompletedAt(Instant.now());
        when(attemptRepository.findLatestCompleted(studentId)).thenReturn(Optional.of(attempt));

        PlacementAttemptSection section = new PlacementAttemptSection();
        section.setSkill(Skill.READING);
        section.setSubmittedAt(Instant.now());
        section.setScorePercent(80);
        section.setCefrLevel("B1");
        when(attemptRepository.findSections(attempt.getId())).thenReturn(List.of(section));

        when(writingRepository.findByUser(studentId)).thenReturn(List.of());

        StudentEvaluationResponse response = service.getStudentEvaluation(studentId);

        assertThat(response.result()).isNotNull();
        assertThat(response.result().overallCefr()).isEqualTo("B1");
        assertThat(response.result().skills()).hasSize(1);
        assertThat(response.writing()).isEmpty();
    }

    @Test
    void getStudentEvaluation_withNoAttempt_returnsNullResult() {
        UUID studentId = UUID.randomUUID();
        when(attemptRepository.findLatestCompleted(studentId)).thenReturn(Optional.empty());
        when(writingRepository.findByUser(studentId)).thenReturn(List.of());

        StudentEvaluationResponse response = service.getStudentEvaluation(studentId);

        assertThat(response.result()).isNull();
    }
}
