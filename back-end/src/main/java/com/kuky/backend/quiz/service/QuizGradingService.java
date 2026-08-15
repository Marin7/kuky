package com.kuky.backend.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import com.kuky.backend.learning.service.QuestionScoring;
import com.kuky.backend.learning.service.SingleChoiceItems;
import com.kuky.backend.quiz.dto.QuizSkillScoreDto;
import com.kuky.backend.quiz.dto.QuizTakeResponse;
import com.kuky.backend.quiz.model.QuizAnswer;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizGradingService {

    private final QuestionScoring questionScoring;
    private final ExerciseGradingService exerciseGradingService;
    private final ObjectMapper objectMapper;

    public QuizGradingService(ExerciseGradingService exerciseGradingService, ObjectMapper objectMapper) {
        this.exerciseGradingService = exerciseGradingService;
        this.objectMapper = objectMapper;
        this.questionScoring = new QuestionScoring(objectMapper);
    }

    public record GradeOutcome(
            List<QuizAnswer> answers,
            int scorePercent,
            int fullyCorrectCount,
            int questionUnitCount,
            boolean awaitingTeacher,
            List<QuizSkillScoreDto> skills,
            List<QuizTakeResponse.QuizQuestionResultDto> questionResults
    ) {
        public QuizAttemptStatus status() {
            return awaitingTeacher ? QuizAttemptStatus.SUBMITTED : QuizAttemptStatus.GRADED;
        }
    }

    public GradeOutcome gradeSubmit(
            List<QuizQuestion> questions,
            Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion) {
        List<QuizAnswer> answers = new ArrayList<>();
        for (QuizQuestion q : questions) {
            HomeworkQuestion hq = QuizSnapshot.toHomeworkQuestion(q);
            SubmitExerciseRequest.AnswerDto given = byQuestion.get(q.getId());
            QuizAnswer answer = new QuizAnswer();
            answer.setQuestionId(q.getId());
            if (q.getKind() == QuestionKind.FREE_TEXT) {
                answer.setAnswerText(given == null ? null : given.text());
                answers.add(answer);
                continue;
            }
            SingleChoiceItems.requireCompleteSelections(hq, given);
            QuestionScoring.GradedAnswer graded = questionScoring.grade(hq, given);
            answer.setAnswerJson(graded.answerJson());
            answer.setScore(HomeworkCompositionSupport.scoreAsDecimal(graded.score()));
            answer.setSelectedOptionIds(graded.selectedOptionIds());
            answers.add(answer);
        }
        return summarize(questions, answers, false);
    }

    public GradeOutcome summarize(List<QuizQuestion> questions, List<QuizAnswer> answers, boolean autoOnly) {
        Map<UUID, QuizAnswer> byId = answers.stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));

        double scoreSum = 0;
        int fullyCorrect = 0;
        int counted = 0;
        boolean awaiting = false;
        List<QuizTakeResponse.QuizQuestionResultDto> results = new ArrayList<>();
        Map<QuizSkill, SkillAccum> perSkill = new LinkedHashMap<>();

        for (QuizQuestion q : questions) {
            HomeworkQuestion hq = QuizSnapshot.toHomeworkQuestion(q);
            QuizAnswer a = byId.get(q.getId());
            boolean freeText = q.getKind() == QuestionKind.FREE_TEXT;
            boolean missingTeacher = freeText && (a == null || a.getTeacherPercent() == null);
            if (missingTeacher) awaiting = true;

            if (freeText && (autoOnly || missingTeacher)) {
                SkillAccum acc = perSkill.computeIfAbsent(q.getSkill(), s -> new SkillAccum());
                acc.awaiting = true;
                results.add(toQuestionResult(q, hq, a));
                continue;
            }

            HomeworkAnswer ha = toHomeworkAnswer(a);
            List<BigDecimal> contribs = HomeworkCompositionSupport.contributions(hq, ha);
            for (BigDecimal c : contribs) {
                counted++;
                scoreSum += c.doubleValue();
                if (HomeworkCompositionSupport.isFullyCorrect(c)) fullyCorrect++;
                SkillAccum acc = perSkill.computeIfAbsent(q.getSkill(), s -> new SkillAccum());
                acc.counted++;
                acc.scoreSum += c.doubleValue();
                if (HomeworkCompositionSupport.isFullyCorrect(c)) acc.fullyCorrect++;
            }
            results.add(toQuestionResult(q, hq, a));
        }

        int percent = counted == 0 ? 0 : HomeworkCompositionSupport.scorePercent(scoreSum, counted);
        List<QuizSkillScoreDto> skills = perSkill.entrySet().stream()
                .map(e -> {
                    SkillAccum acc = e.getValue();
                    Integer skillPercent = acc.awaiting ? null
                            : (acc.counted == 0 ? 0 : HomeworkCompositionSupport.scorePercent(acc.scoreSum, acc.counted));
                    Integer fc = acc.awaiting ? null : acc.fullyCorrect;
                    Integer qc = acc.awaiting && acc.counted == 0 ? null : acc.counted;
                    if (acc.awaiting && acc.counted == 0) {
                        qc = freeTextUnitCount(questions, e.getKey());
                    }
                    return new QuizSkillScoreDto(e.getKey().name(), skillPercent, fc, qc == null ? 0 : qc, acc.awaiting);
                })
                .toList();

        return new GradeOutcome(answers, percent, fullyCorrect, counted, awaiting, skills, results);
    }

    private int freeTextUnitCount(List<QuizQuestion> questions, QuizSkill skill) {
        int n = 0;
        for (QuizQuestion q : questions) {
            if (q.getSkill() == skill && q.getKind() == QuestionKind.FREE_TEXT) n++;
        }
        return n;
    }

    public List<QuizTakeResponse.QuizStudentQuestionDto> studentQuestions(
            List<QuizQuestion> questions, boolean revealKeys) {
        List<HomeworkQuestion> hw = questions.stream().map(QuizSnapshot::toHomeworkQuestion).toList();
        List<ExerciseQuestionDto> stripped = exerciseGradingService.studentQuestionsFor(hw);
        Map<UUID, ExerciseQuestionDto> byId = stripped.stream()
                .collect(Collectors.toMap(ExerciseQuestionDto::id, Function.identity(), (a, b) -> a));
        List<QuizTakeResponse.QuizStudentQuestionDto> out = new ArrayList<>();
        for (QuizQuestion q : questions) {
            ExerciseQuestionDto dto = byId.get(q.getId());
            JsonNode structure = dto == null ? null : dto.structure();
            List<ExerciseQuestionDto.StudentOptionDto> options = dto == null ? List.of() : dto.options();
            if (revealKeys) {
                // Keys live in result items; take DTO still strips until submit.
            }
            out.add(new QuizTakeResponse.QuizStudentQuestionDto(
                    q.getId(),
                    q.getSkill().name(),
                    q.getKind().name(),
                    q.getPrompt(),
                    options,
                    structure,
                    q.getMediaSourceKind() == null ? null : q.getMediaSourceKind().name(),
                    q.getAudioUrl(),
                    q.getAudioFileId()));
        }
        return out;
    }

    public void applyToAttempt(QuizAttempt attempt, GradeOutcome outcome) {
        attempt.setScorePercent(outcome.awaitingTeacher() && outcome.questionUnitCount() == 0
                ? null : outcome.scorePercent());
        attempt.setFullyCorrectCount(outcome.awaitingTeacher() ? null : outcome.fullyCorrectCount());
        attempt.setQuestionUnitCount(outcome.questionUnitCount());
        attempt.setStatus(outcome.status());
    }

    private QuizTakeResponse.QuizQuestionResultDto toQuestionResult(
            QuizQuestion q, HomeworkQuestion hq, QuizAnswer a) {
        boolean numbered = SingleChoiceItems.isNumbered(hq);
        double score = a == null || a.getScore() == null ? 0.0 : a.getScore().doubleValue();
        boolean correct = score >= 1.0;
        List<UUID> correctOptionIds = numbered ? List.of() : correctOptionIds(hq);
        List<ExerciseResultResponse.UnitResultDto> units = List.of();
        if (a != null && (numbered || q.getKind().isStructured())) {
            JsonNode answerJson = parseAnswerJson(a.getAnswerJson());
            SubmitExerciseRequest.AnswerDto given =
                    new SubmitExerciseRequest.AnswerDto(q.getId(), List.of(), answerJson);
            try {
                units = questionScoring.grade(hq, given).unitResults();
            } catch (RuntimeException ignored) {
                units = List.of();
            }
        }
        List<UUID> selected = a == null || a.getSelectedOptionIds() == null ? List.of() : a.getSelectedOptionIds();
        return new QuizTakeResponse.QuizQuestionResultDto(
                q.getId(), q.getSkill().name(), score, correct, correctOptionIds,
                List.of(), units, selected,
                a == null ? null : a.getAnswerText(),
                a == null ? null : a.getTeacherPercent());
    }

    public QuizTakeResponse.QuizQuestionResultDto stripTeacherPercent(QuizTakeResponse.QuizQuestionResultDto dto) {
        if (dto == null) return null;
        return new QuizTakeResponse.QuizQuestionResultDto(
                dto.questionId(), dto.skill(), dto.score(), dto.correct(), dto.correctOptionIds(),
                dto.acceptedAnswers(), dto.unitResults(), dto.selectedOptionIds(),
                dto.answerText(), null);
    }

    private static List<UUID> correctOptionIds(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.SINGLE_CHOICE
                && q.getKind() != QuestionKind.MULTI_CHOICE
                && q.getKind() != QuestionKind.TRUE_FALSE) {
            return List.of();
        }
        return q.getOptions().stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
    }

    private JsonNode parseAnswerJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null || node.isNull() ? objectMapper.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private static HomeworkAnswer toHomeworkAnswer(QuizAnswer a) {
        if (a == null) return null;
        HomeworkAnswer h = new HomeworkAnswer();
        h.setId(a.getId());
        h.setQuestionId(a.getQuestionId());
        h.setAnswerJson(a.getAnswerJson());
        h.setAnswerText(a.getAnswerText());
        h.setScore(a.getScore());
        h.setTeacherScorePercent(a.getTeacherPercent());
        h.setSelectedOptionIds(a.getSelectedOptionIds());
        return h;
    }

    private static final class SkillAccum {
        double scoreSum;
        int fullyCorrect;
        int counted;
        boolean awaiting;
    }
}
