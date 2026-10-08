package com.kuky.backend.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.dto.ActivityReviewQueueItemDto;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityHasSubmissionsException;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityPageTakenException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.exception.AlreadyReviewedException;
import com.kuky.backend.learning.exception.NotSubmittedException;
import com.kuky.backend.learning.exception.SubmissionNotFoundException;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityQuestion;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import com.kuky.backend.notification.service.NotificationService;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Page activities of unit PDFs: authoring (mark/edit/unmark a page) and teacher review. */
@Service
@Transactional
public class ActivityAdminService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final ActivityRepository activityRepository;
    private final ActivityQuestionRepository questionRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityAnswerRepository answerRepository;
    private final PresentationRepository presentationRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final HomeworkAdminService homeworkAdminService;
    private final ActivityExerciseGradingService exerciseGradingService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ActivityAdminService(ActivityRepository activityRepository,
                                ActivityQuestionRepository questionRepository,
                                ActivitySubmissionRepository submissionRepository,
                                ActivityAnswerRepository answerRepository,
                                PresentationRepository presentationRepository,
                                UnitRepository unitRepository,
                                UserRepository userRepository,
                                HomeworkAdminService homeworkAdminService,
                                ActivityExerciseGradingService exerciseGradingService,
                                NotificationService notificationService,
                                ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.presentationRepository = presentationRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.homeworkAdminService = homeworkAdminService;
        this.exerciseGradingService = exerciseGradingService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public ActivityAdminDetail get(UUID id) {
        return toDetail(requireActivity(id));
    }

    /** Marks {@code page} of the unit's PDF as an activity with the given work. */
    public ActivityAdminDetail createOnPage(UUID unitId, int page, String title,
                                            List<HomeworkQuestionDto> questionDtos) {
        unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException("Unidad no encontrada."));
        PresentationRepository.OwnedPdf pdf = presentationRepository.findOwnedByUnit(unitId)
                .filter(p -> p.fileId() != null && p.pageCount() != null)
                .orElseThrow(() -> new ActivityValidationException("La unidad no tiene PDF."));
        if (page < 1 || page > pdf.pageCount()) {
            throw new ActivityValidationException(
                    "La página debe estar entre 1 y " + pdf.pageCount() + ".");
        }
        if (activityRepository.findByPresentationAndPage(pdf.presentationId(), page).isPresent()) {
            throw new ActivityPageTakenException("La página " + page + " ya es una actividad.");
        }
        List<ActivityQuestion> questions = mapQuestions(questionDtos);

        Activity activity = new Activity();
        activity.setPresentationId(pdf.presentationId());
        activity.setPage(page);
        activity.setTitle(normalizeTitle(title));
        activity.setFormat(deriveFormat(questions));
        activityRepository.insert(activity);
        questionRepository.replaceQuestions(activity.getId(), questions);
        return toDetail(requireActivity(activity.getId()));
    }

    public ActivityAdminDetail update(UUID id, SaveActivityRequest request) {
        Activity existing = requireActivity(id);
        List<ActivityQuestion> questions = mapQuestions(request == null ? null : request.questions());
        existing.setTitle(normalizeTitle(request == null ? null : request.title()));
        existing.setFormat(deriveFormat(questions));
        activityRepository.update(existing);
        questionRepository.replaceQuestions(id, questions);
        return toDetail(requireActivity(id));
    }

    /** Unmarks the page. Student work is only deleted when the teacher confirmed it. */
    public void delete(UUID id, boolean deleteSubmissions) {
        requireActivity(id);
        int submissions = activityRepository.countSubmissions(id);
        if (submissions > 0 && !deleteSubmissions) {
            throw new ActivityHasSubmissionsException(
                    "La actividad tiene " + submissions + " respuesta(s) de alumnos que se borrarían.");
        }
        if (activityRepository.delete(id) == 0) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
    }

    // --- review --------------------------------------------------------------

    public List<ActivityReviewQueueItemDto> getReviewQueue(UUID unitIdOrNull) {
        return submissionRepository.findSubmittedManualQueue(unitIdOrNull).stream()
                .map(r -> new ActivityReviewQueueItemDto(
                        r.submissionId(), r.studentId(), r.studentEmail(), r.studentFirstName(),
                        r.studentLastName(), r.studentUsername(),
                        displayTitle(r.activityTitle(), r.page()), r.submittedAt(), r.unseen(),
                        r.activityId(), r.unitId(), r.page()))
                .toList();
    }

    public HomeworkSubmissionAdminDto getSubmissionDetail(UUID submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        notificationService.markActivitySeen(submissionId);
        return toSubmissionAdminDto(submission);
    }

    public ExerciseSubmissionResultAdminDto getExerciseResult(UUID submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido calificada automáticamente.");
        }
        Activity activity = requireActivity(submission.getActivityId());
        if (activity.getFormat() != HomeworkFormat.EXERCISE
                && activity.getFormat() != HomeworkFormat.MIXED) {
            throw new ActivityNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        notificationService.markActivitySeen(submissionId);
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        var view = exerciseGradingService.viewGradedSubmission(submission);
        return new ExerciseSubmissionResultAdminDto(
                submission.getId(),
                activity.getId(),
                activity.displayTitle(),
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getUsername(),
                view.questions(),
                view.result(),
                FormattedTextSegment.decodePlainFeedback(submission.getFeedback()));
    }

    public HomeworkSubmissionAdminDto saveFeedback(UUID submissionId, SaveHomeworkFeedbackRequest request) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if ("LEGACY_RICH".equals(submission.getReviewModel())) {
            throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
        }

        Activity activity = requireActivity(submission.getActivityId());
        List<ActivityQuestion> questions = questionRepository.findByActivityId(activity.getId());
        HomeworkComposition composition = HomeworkCompositionSupport.activityComposition(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());

        boolean firstReview = HomeworkStatus.SUBMITTED.name().equals(submission.getStatus());
        boolean scoredReEdit = (composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_MANUAL)
                && HomeworkStatus.GRADED.name().equals(submission.getStatus())
                && "ANNOTATED".equals(submission.getReviewModel());
        boolean legacyManualReEdit = composition != HomeworkComposition.MIXED
                && composition != HomeworkComposition.ALL_MANUAL
                && HomeworkStatus.REVIEWED.name().equals(submission.getStatus())
                && "ANNOTATED".equals(submission.getReviewModel());

        if (!firstReview && !scoredReEdit && !legacyManualReEdit) {
            if (HomeworkStatus.REVIEWED.name().equals(submission.getStatus())
                    || HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
                throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
            }
            throw new NotSubmittedException("Esta entrega todavía no ha sido enviada por el alumno.");
        }

        String feedbackJson = FormattedTextSegment.encodePlainFeedback(
                request == null ? null : request.feedbackText(),
                FormattedTextSegment.MAX_MANUAL_FEEDBACK_LENGTH);
        List<HomeworkAnswer> storedAnswers = answerRepository.findBySubmission(submissionId);
        Map<UUID, QuestionKind> kindByQuestion = questions.stream()
                .collect(Collectors.toMap(ActivityQuestion::getId, ActivityQuestion::getKind, (a, b) -> a));

        boolean finalize = scoredReEdit
                || (request != null && Boolean.TRUE.equals(request.finalizeGrade()));

        if (storedAnswers.isEmpty()) {
            // Activities have no WRITE path with empty answers in normal use; keep annotate-only fallback.
            List<FormattedTextSegment> response = request == null ? null : request.response();
            FormattedTextSegment.validate(response);
            assertUnchangedWording(response, submission.getResponseText());
            String responseText = FormattedTextSegment.toJson(response);
            submissionRepository.saveAnnotatedReview(submissionId, feedbackJson, responseText, firstReview);
        } else if (composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_MANUAL) {
            saveQuestionFeedback(submissionId, request, storedAnswers, kindByQuestion, questions,
                    feedbackJson, firstReview, finalize);
        } else {
            throw new IllegalStateException("Unexpected composition for activity review: " + composition);
        }
        ActivitySubmission updated = submissionRepository.findById(submissionId).orElseThrow();
        return toSubmissionAdminDto(updated);
    }

    private void saveQuestionFeedback(UUID submissionId, SaveHomeworkFeedbackRequest request,
                                      List<HomeworkAnswer> storedAnswers,
                                      Map<UUID, QuestionKind> kindByQuestion,
                                      List<ActivityQuestion> questions,
                                      String feedbackJson, boolean firstReview, boolean finalize) {
        List<SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest> requested =
                request == null || request.answers() == null ? List.of() : request.answers();
        Map<UUID, SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest> byQuestion = requested.stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest::questionId,
                        a -> a, (a, b) -> a));

        List<HomeworkAnswer> freeTextAnswers = storedAnswers.stream()
                .filter(a -> a.getQuestionId() != null
                        && kindByQuestion.get(a.getQuestionId()) == QuestionKind.FREE_TEXT)
                .toList();

        for (var stored : freeTextAnswers) {
            var annotation = byQuestion.get(stored.getQuestionId());
            if (annotation == null) {
                if (finalize && stored.getTeacherScorePercent() == null) {
                    throw new IllegalArgumentException(
                            "Debes asignar una puntuación a cada respuesta de texto libre.");
                }
                continue;
            }

            if (annotation.formatted() != null) {
                FormattedTextSegment.validate(annotation.formatted());
                assertUnchangedWording(annotation.formatted(), stored.getAnswerText());
            }

            Integer requestedPercent = annotation.teacherScorePercent();
            if (requestedPercent != null) {
                int percent = parseTeacherPercent(requestedPercent);
                String answerText = annotation.formatted() != null
                        ? FormattedTextSegment.toJson(annotation.formatted())
                        : stored.getAnswerText();
                BigDecimal score = HomeworkCompositionSupport.scoreAsDecimal(
                        HomeworkCompositionSupport.teacherPercentAsScore(percent));
                answerRepository.updateManualReview(stored.getId(), answerText, percent, score);
                stored.setTeacherScorePercent(percent);
                stored.setScore(score);
                if (annotation.formatted() != null) {
                    stored.setAnswerText(answerText);
                }
            } else if (annotation.formatted() != null) {
                String answerText = FormattedTextSegment.toJson(annotation.formatted());
                if (stored.getTeacherScorePercent() != null) {
                    answerRepository.updateManualReview(
                            stored.getId(), answerText, stored.getTeacherScorePercent(), stored.getScore());
                } else if (finalize) {
                    throw new IllegalArgumentException(
                            "Debes asignar una puntuación a cada respuesta de texto libre.");
                } else {
                    answerRepository.updateAnswerText(stored.getId(), answerText);
                }
                stored.setAnswerText(answerText);
            } else if (finalize && stored.getTeacherScorePercent() == null) {
                throw new IllegalArgumentException(
                        "Debes asignar una puntuación a cada respuesta de texto libre.");
            }
        }

        if (!finalize) {
            submissionRepository.saveAnnotatedProgress(submissionId, feedbackJson);
            return;
        }

        Map<UUID, HomeworkAnswer> answersByQ = new LinkedHashMap<>();
        for (var refreshed : answerRepository.findBySubmission(submissionId)) {
            if (refreshed.getQuestionId() != null) {
                answersByQ.put(refreshed.getQuestionId(), refreshed);
            }
        }

        for (var stored : freeTextAnswers) {
            var refreshed = answersByQ.get(stored.getQuestionId());
            Integer percent = refreshed == null ? null : refreshed.getTeacherScorePercent();
            if (percent == null) {
                throw new IllegalArgumentException(
                        "Debes asignar una puntuación a cada respuesta de texto libre.");
            }
        }

        List<BigDecimal> scores = new ArrayList<>();
        for (ActivityQuestion q : questions) {
            scores.addAll(HomeworkCompositionSupport.contributions(
                    q.toHomeworkQuestion(), answersByQ.get(q.getId())));
        }
        int scorePercent = HomeworkCompositionSupport.scorePercentFromScores(scores);
        submissionRepository.saveScoredAnnotatedReview(submissionId, feedbackJson, scorePercent, firstReview);
    }

    private static int parseTeacherPercent(Integer raw) {
        if (raw == null) {
            throw new IllegalArgumentException(
                    "Debes asignar una puntuación a cada respuesta de texto libre.");
        }
        if (raw < 0 || raw > 100) {
            throw new IllegalArgumentException("La puntuación debe ser un entero entre 0 y 100.");
        }
        return raw;
    }

    private static void assertUnchangedWording(List<FormattedTextSegment> incoming, String stored) {
        if (!FormattedTextSegment.plainText(incoming)
                .equals(FormattedTextSegment.storedPlainWording(stored))) {
            throw new IllegalArgumentException("No se puede modificar el texto de la respuesta del alumno.");
        }
    }

    public ExerciseSubmissionResultAdminDto saveExerciseFeedback(UUID submissionId, String feedback) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido calificada automáticamente.");
        }
        Activity activity = requireActivity(submission.getActivityId());
        if (activity.getFormat() != HomeworkFormat.EXERCISE
                && activity.getFormat() != HomeworkFormat.MIXED) {
            throw new ActivityNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        String encoded = FormattedTextSegment.encodePlainFeedback(feedback);
        submissionRepository.saveExerciseFeedback(submissionId, encoded);
        return getExerciseResult(submissionId);
    }

    // --- helpers -------------------------------------------------------------

    private Activity requireActivity(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException("Actividad no encontrada."));
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String trimmed = title.strip();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ActivityValidationException("El título no puede superar los 200 caracteres.");
        }
        return trimmed;
    }

    private static String displayTitle(String title, int page) {
        return title == null || title.isBlank() ? "Actividad – página " + page : title;
    }

    private static HomeworkFormat deriveFormat(List<ActivityQuestion> questions) {
        return HomeworkCompositionSupport.deriveActivityFormat(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
    }

    private List<ActivityQuestion> mapQuestions(List<HomeworkQuestionDto> questionDtos) {
        if (questionDtos == null || questionDtos.isEmpty()) {
            throw new ActivityValidationException("Añade al menos una pregunta.");
        }
        List<HomeworkQuestion> mapped;
        try {
            mapped = homeworkAdminService.validateAndMapQuestions(false, questionDtos);
        } catch (IllegalArgumentException e) {
            throw new ActivityValidationException(e.getMessage());
        }
        return mapped.stream()
                .map(q -> ActivityQuestion.fromHomeworkQuestion(q, null))
                .toList();
    }

    private HomeworkComposition compositionOf(Activity a, List<ActivityQuestion> questions) {
        if (questions != null && !questions.isEmpty()) {
            return HomeworkCompositionSupport.activityComposition(
                    questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        }
        if (a.getFormat() == HomeworkFormat.MIXED) return HomeworkComposition.MIXED;
        if (a.getFormat() == HomeworkFormat.EXERCISE) return HomeworkComposition.ALL_AUTO;
        return HomeworkComposition.ALL_MANUAL;
    }

    private ActivityAdminDetail toDetail(Activity a) {
        List<ActivityQuestion> questionModels = questionRepository.findByActivityId(a.getId());
        List<HomeworkQuestionDto> questions = questionModels.stream().map(this::toQuestionDto).toList();
        HomeworkComposition composition = compositionOf(a, questionModels);
        return new ActivityAdminDetail(
                a.getId(),
                activityRepository.findUnitId(a.getId()).orElse(null),
                a.getPresentationId(),
                a.getPage(),
                a.getTitle(),
                a.getFormat().name(),
                composition.name(),
                questions,
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private HomeworkQuestionDto toQuestionDto(ActivityQuestion q) {
        List<HomeworkQuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new HomeworkQuestionDto.OptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new HomeworkQuestionDto(
                q.getId(), q.getKind().name(), q.getPrompt(), options, readStructure(q.getStructureJson()));
    }

    private JsonNode readStructure(String structureJson) {
        if (structureJson == null || structureJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(structureJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer la estructura de la pregunta.", e);
        }
    }

    private HomeworkSubmissionAdminDto toSubmissionAdminDto(ActivitySubmission submission) {
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        Activity activity = requireActivity(submission.getActivityId());
        List<ManualAnswerViewDto> answers = answerRepository.findBySubmission(submission.getId()).stream()
                .filter(a -> a.getPromptSnapshot() != null || a.getAnswerText() != null)
                .map(a -> ManualAnswerViewDto.fromStored(
                        a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText(),
                        a.getTeacherScorePercent(),
                        a.getScore() == null ? null : a.getScore().doubleValue()))
                .toList();
        List<FormattedTextSegment> response = answers.isEmpty()
                ? FormattedTextSegment.fromJson(submission.getResponseText())
                : null;
        String formatName = activity.getFormat() == null ? HomeworkFormat.MANUAL.name() : activity.getFormat().name();
        HomeworkComposition composition = switch (activity.getFormat() == null
                ? HomeworkFormat.MANUAL : activity.getFormat()) {
            case MIXED -> HomeworkComposition.MIXED;
            case EXERCISE -> HomeworkComposition.ALL_AUTO;
            case MANUAL -> HomeworkComposition.ALL_MANUAL;
        };
        return new HomeworkSubmissionAdminDto(
                submission.getId(),
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getUsername(),
                activity.displayTitle(),
                submission.getStatus(),
                formatName,
                composition.name(),
                submission.getReviewModel(),
                response,
                answers,
                "LEGACY_RICH".equals(submission.getReviewModel())
                        ? FormattedTextSegment.fromJson(submission.getFeedback()) : null,
                "ANNOTATED".equals(submission.getReviewModel())
                        ? FormattedTextSegment.decodePlainFeedback(submission.getFeedback()) : null,
                submission.getScorePercent(),
                null,
                submission.getSubmittedAt(),
                submission.getReviewedAt());
    }
}
