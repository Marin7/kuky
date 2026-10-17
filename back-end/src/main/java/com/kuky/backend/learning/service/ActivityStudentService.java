package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ActivityItemResponse;
import com.kuky.backend.learning.dto.ActivitySummary;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.ManualAnswerDto;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.ActivityAlreadySubmittedException;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityQuestion;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityStudentService {


    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityQuestionRepository questionRepository;
    private final ActivityAnswerRepository answerRepository;
    private final PresentationRepository presentationRepository;
    private final UserRepository userRepository;
    private final ActivityExerciseGradingService gradingService;

    public ActivityStudentService(ActivityRepository activityRepository,
                                  ActivitySubmissionRepository submissionRepository,
                                  ActivityQuestionRepository questionRepository,
                                  ActivityAnswerRepository answerRepository,
                                  PresentationRepository presentationRepository,
                                  UserRepository userRepository,
                                  ActivityExerciseGradingService gradingService) {
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.presentationRepository = presentationRepository;
        this.userRepository = userRepository;
        this.gradingService = gradingService;
    }

    /** Activities for the given presentations, keyed by presentation id. */
    public Map<UUID, List<ActivitySummary>> summariesForPresentations(UUID userId, List<UUID> presentationIds) {
        if (presentationIds == null || presentationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ActivitySubmission> byActivity = submissionRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ActivitySubmission::getActivityId, s -> s, (a, b) -> a));

        Map<UUID, List<ActivitySummary>> result = new HashMap<>();
        for (UUID presentationId : presentationIds) {
            List<ActivitySummary> list = new ArrayList<>();
            for (Activity a : activityRepository.listByPresentationId(presentationId)) {
                ActivitySubmission sub = byActivity.get(a.getId());
                String status = sub == null ? HomeworkStatus.PENDING.name() : sub.getStatus();
                list.add(new ActivitySummary(
                        a.getId(), a.getPage(), a.getFormat().name(), status));
            }
            result.put(presentationId, list);
        }
        return result;
    }

    public ActivityItemResponse get(String email, UUID activityId) {
        User user = requireUser(email);
        Activity activity = requireAccessible(activityId, user.getId());
        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        return toItemResponse(activity, existing.orElse(null));
    }

    /** Transitional ALL_MANUAL path (legacy PUT /activities/{id}). */
    @Transactional
    public ActivityItemResponse submitManual(String email, UUID activityId,
                                             List<FormattedTextSegment> response,
                                             List<ManualAnswerDto> answers) {
        User user = requireUser(email);
        Activity activity = requireAccessible(activityId, user.getId());
        HomeworkComposition composition = compositionOf(activity);
        if (composition != HomeworkComposition.ALL_MANUAL) {
            throw new ActivityValidationException("Este ejercicio se entrega desde su propia página.");
        }
        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        rejectIfTerminal(existing);
        if (response != null && !response.isEmpty()) {
            throw new ActivityValidationException("Esta actividad se entrega con respuestas por pregunta.");
        }
        List<ActivityQuestion> questions = freeTextQuestions(activityId);
        List<HomeworkAnswer> mapped;
        try {
            mapped = HomeworkSubmissionService.validateAndMapFreeTextAnswers(
                    questions.stream().map(ActivityQuestion::toHomeworkQuestion).toList(),
                    answers);
        } catch (IllegalArgumentException e) {
            throw new ActivityValidationException(e.getMessage());
        }
        ActivitySubmission saved = submissionRepository.upsertManual(
                user.getId(),
                activityId,
                HomeworkStatus.SUBMITTED.name(),
                null,
                Instant.now());
        answerRepository.saveAll(saved.getId(), mapped);
        return toItemResponse(activity, saved);
    }

    /**
     * Unified submit: ALL_AUTO → GRADED; ALL_MANUAL → SUBMITTED;
     * MIXED → SUBMITTED with auto scores + provisional %. Scores are stored for the
     * teacher only; the response to the student carries none.
     */
    @Transactional
    public ActivityItemResponse submitAnswers(String email, UUID activityId, SubmitExerciseRequest request) {
        User user = requireUser(email);
        Activity activity = requireAccessible(activityId, user.getId());
        HomeworkComposition composition = compositionOf(activity);

        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        rejectIfTerminal(existing);

        List<HomeworkQuestion> questions = gradingService.toHomeworkQuestions(activityId);
        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = indexAnswers(request);

        List<HomeworkAnswer> allAnswers = new ArrayList<>();
        ExerciseResultResponse autoResult = null;

        try {
            if (composition == HomeworkComposition.ALL_AUTO || composition == HomeworkComposition.MIXED) {
                HomeworkSubmissionService.requireEveryQuestionAnswered(questions, byQuestion);
                ActivityExerciseGradingService.StructuredGradeResult graded =
                        gradingService.gradeStructuredSubset(questions, byQuestion);
                allAnswers.addAll(graded.structuredAnswers());
                autoResult = graded.toProvisionalResult();

                if (composition == HomeworkComposition.ALL_AUTO) {
                    ActivitySubmission saved = submissionRepository.upsertGraded(
                            user.getId(), activityId, graded.provisionalScorePercent(), Instant.now());
                    answerRepository.saveAll(saved.getId(), allAnswers);
                    return toItemResponse(activity, saved, questions, allAnswers,
                            gradingService.studentQuestionsFor(questions), autoResult);
                }

                allAnswers.addAll(HomeworkSubmissionService.mapFreeTextFromUnified(questions, byQuestion));
                ActivitySubmission saved = submissionRepository.upsertManual(
                        user.getId(), activityId, HomeworkStatus.SUBMITTED.name(), null, Instant.now());
                answerRepository.saveAll(saved.getId(), allAnswers);
                return toItemResponse(activity, saved, questions, allAnswers,
                        gradingService.studentQuestionsFor(questions), autoResult);
            }

            // ALL_MANUAL via /answers
            List<ManualAnswerDto> manual = HomeworkSubmissionService.toManualDtos(byQuestion);
            List<HomeworkAnswer> mapped = HomeworkSubmissionService.validateAndMapFreeTextAnswers(
                    questions.stream().filter(q -> q.getKind().isManual()).toList(), manual);
            ActivitySubmission saved = submissionRepository.upsertManual(
                    user.getId(), activityId, HomeworkStatus.SUBMITTED.name(), null, Instant.now());
            answerRepository.saveAll(saved.getId(), mapped);
            return toItemResponse(activity, saved, questions, mapped,
                    gradingService.studentQuestionsFor(questions), null);
        } catch (IllegalArgumentException e) {
            throw new ActivityValidationException(e.getMessage());
        }
    }

    private ActivityItemResponse toItemResponse(Activity activity, ActivitySubmission submission) {
        List<HomeworkQuestion> questions = gradingService.toHomeworkQuestions(activity.getId());
        List<HomeworkAnswer> answers = submission == null
                ? List.of()
                : answerRepository.findBySubmission(submission.getId());
        HomeworkComposition composition = HomeworkCompositionSupport.activityComposition(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        List<ExerciseQuestionDto> studentQuestions = gradingService.studentQuestionsFor(questions);
        ExerciseResultResponse result = null;
        if (submission != null && !answers.isEmpty()
                && (composition == HomeworkComposition.ALL_AUTO || composition == HomeworkComposition.MIXED)
                && (HomeworkStatus.GRADED.name().equals(submission.getStatus())
                || HomeworkStatus.SUBMITTED.name().equals(submission.getStatus()))) {
            result = composition == HomeworkComposition.MIXED
                    && HomeworkStatus.SUBMITTED.name().equals(submission.getStatus())
                    ? gradingService.storedProvisionalResultFor(questions, submission)
                    : gradingService.storedResultFor(questions, submission);
        }
        return toItemResponse(activity, submission, questions, answers, studentQuestions, result);
    }

    private ActivityItemResponse toItemResponse(Activity activity, ActivitySubmission submission,
                                                List<HomeworkQuestion> questions,
                                                List<HomeworkAnswer> answers,
                                                List<ExerciseQuestionDto> studentQuestions,
                                                ExerciseResultResponse result) {
        HomeworkComposition composition = HomeworkCompositionSupport.activityComposition(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        String status = submission == null ? HomeworkStatus.PENDING.name() : submission.getStatus();
        String format = activity.getFormat() == null ? HomeworkFormat.MANUAL.name() : activity.getFormat().name();
        boolean annotated = submission != null && "ANNOTATED".equals(submission.getReviewModel());

        List<ManualAnswerViewDto> answerViews = List.of();
        if (!answers.isEmpty()
                && (composition == HomeworkComposition.ALL_MANUAL || composition == HomeworkComposition.MIXED)) {
            // Students never see scores on activities — not even the teacher's % per free-text answer.
            answerViews = answers.stream()
                    .filter(a -> a.getPromptSnapshot() != null || a.getAnswerText() != null)
                    .map(a -> ManualAnswerViewDto.fromStored(
                            a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText())
                            .withSentenceStarters(a.getAnswerJson()))
                    .toList();
        }

        String teacherFeedback = null;
        List<FormattedTextSegment> feedback = null;
        String feedbackText = null;
        if (submission != null) {
            if (composition == HomeworkComposition.ALL_AUTO
                    || (composition == HomeworkComposition.MIXED
                    && HomeworkStatus.GRADED.name().equals(status))) {
                teacherFeedback = FormattedTextSegment.decodePlainFeedback(submission.getFeedback());
            }
            if (composition == HomeworkComposition.ALL_MANUAL
                    || (composition == HomeworkComposition.MIXED
                    && (HomeworkStatus.SUBMITTED.name().equals(status)
                    || HomeworkStatus.GRADED.name().equals(status)
                    || HomeworkStatus.REVIEWED.name().equals(status)))) {
                if (annotated) {
                    feedbackText = FormattedTextSegment.decodePlainFeedback(submission.getFeedback());
                } else {
                    feedback = FormattedTextSegment.fromJson(submission.getFeedback());
                }
            }
        }

        return new ActivityItemResponse(
                activity.getId(),
                format,
                composition.name(),
                status,
                activity.getPresentationId(),
                presentationRepository.listFileIds(activity.getPresentationId()).stream().findFirst().orElse(null),
                activity.getPage(),
                activityRepository.findUnitId(activity.getId()).orElse(null),
                submission == null ? null : submission.getReviewModel(),
                List.of(),
                feedback,
                feedbackText,
                studentQuestions,
                result == null ? null : result.withoutScore(),
                teacherFeedback,
                answerViews);
    }

    private HomeworkComposition compositionOf(Activity activity) {
        List<HomeworkQuestion> questions = gradingService.toHomeworkQuestions(activity.getId());
        return HomeworkCompositionSupport.activityComposition(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
    }

    private List<ActivityQuestion> freeTextQuestions(UUID activityId) {
        return questionRepository.findByActivityId(activityId).stream()
                .filter(q -> q.getKind().isManual())
                .toList();
    }

    private static Map<UUID, SubmitExerciseRequest.AnswerDto> indexAnswers(SubmitExerciseRequest request) {
        if (request == null || request.answers() == null) return Map.of();
        return request.answers().stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));
    }

    /** Once delivered (SUBMITTED / REVIEWED / GRADED), the student cannot change answers. */
    private static void rejectIfTerminal(Optional<ActivitySubmission> existing) {
        if (existing.isEmpty()) {
            return;
        }
        String status = existing.get().getStatus();
        if (HomeworkStatus.REVIEWED.name().equals(status)) {
            throw new ActivityAlreadySubmittedException(
                    "Esta actividad ya ha sido revisada y no puede modificarse.");
        }
        if (HomeworkStatus.GRADED.name().equals(status)) {
            throw new ActivityAlreadySubmittedException(
                    "Este ejercicio ya ha sido entregado y no puede repetirse.");
        }
        if (HomeworkStatus.SUBMITTED.name().equals(status)) {
            throw new ActivityAlreadySubmittedException(
                    "Esta actividad ya ha sido entregada y no puede modificarse.");
        }
    }

    private Activity requireAccessible(UUID activityId, UUID userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("Actividad no encontrada."));
        if (!presentationRepository.isSharedWith(activity.getPresentationId(), userId)) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
        return activity;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }
}
