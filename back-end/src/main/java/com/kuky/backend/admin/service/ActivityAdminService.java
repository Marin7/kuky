package com.kuky.backend.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.dto.ActivityAdminItem;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityReorderInvalidException;
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
import com.kuky.backend.learning.model.TeacherValidation;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.learning.service.ActivityInstructionsFileStore;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import com.kuky.backend.learning.util.YoutubeUrls;
import com.kuky.backend.presentations.repository.ImageRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActivityAdminService {

    private final ActivityRepository activityRepository;
    private final ActivityQuestionRepository questionRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityAnswerRepository answerRepository;
    private final ActivityInstructionsFileStore instructionsFileStore;
    private final PresentationRepository presentationRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final HomeworkAdminService homeworkAdminService;
    private final ActivityExerciseGradingService exerciseGradingService;
    private final ObjectMapper objectMapper;

    public ActivityAdminService(ActivityRepository activityRepository,
                                ActivityQuestionRepository questionRepository,
                                ActivitySubmissionRepository submissionRepository,
                                ActivityAnswerRepository answerRepository,
                                ActivityInstructionsFileStore instructionsFileStore,
                                PresentationRepository presentationRepository,
                                ImageRepository imageRepository,
                                UserRepository userRepository,
                                HomeworkAdminService homeworkAdminService,
                                ActivityExerciseGradingService exerciseGradingService,
                                ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.instructionsFileStore = instructionsFileStore;
        this.presentationRepository = presentationRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.homeworkAdminService = homeworkAdminService;
        this.exerciseGradingService = exerciseGradingService;
        this.objectMapper = objectMapper;
    }

    public List<ActivityAdminItem> list(UUID presentationId) {
        return activityRepository.listAll(presentationId).stream()
                .map(row -> toItem(row.activity(), row.presentationTitle(), row.hasInstructions()))
                .toList();
    }

    public ActivityAdminDetail get(UUID id) {
        return toDetail(requireActivity(id));
    }

    public ActivityAdminDetail create(SaveActivityRequest request) {
        return create(
                request.title(),
                request.presentationId(),
                request.format(),
                request.level(),
                request.homeworkType(),
                request.triggerFileId(),
                request.triggerPage(),
                request.instructionsText(),
                request.youtubeUrl(),
                request.imageId(),
                request.questions());
    }

    public ActivityAdminDetail update(UUID id, SaveActivityRequest request) {
        return update(
                id,
                request.title(),
                request.presentationId(),
                request.format(),
                request.level(),
                request.homeworkType(),
                request.triggerFileId(),
                request.triggerPage(),
                request.instructionsText(),
                request.youtubeUrl(),
                request.imageId(),
                request.questions());
    }

    public ActivityAdminDetail create(String title, UUID presentationId, String formatRaw,
                                      String level, String homeworkType,
                                      UUID triggerFileId, Integer triggerPage,
                                      String instructionsText, String youtubeUrl, UUID imageId,
                                      List<HomeworkQuestionDto> questionDtos) {
        if (title == null || title.isBlank()) {
            throw new ActivityValidationException("El título es obligatorio.");
        }
        if (presentationId == null || !activityRepository.presentationExists(presentationId)) {
            throw new ActivityValidationException("La presentación no existe.");
        }
        // Client format is ignored — derived from question kinds.
        requirePageTrigger(presentationId, triggerFileId, triggerPage);
        String resolvedInstructions = requireInstructionsText(instructionsText);
        String resolvedYoutube = normalizeYoutubeUrl(youtubeUrl);
        UUID resolvedImageId = requireMedia(resolvedYoutube, imageId);
        List<ActivityQuestion> questions = mapQuestions(questionDtos);
        HomeworkFormat derived = HomeworkCompositionSupport.deriveActivityFormat(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());

        Activity activity = new Activity();
        activity.setPresentationId(presentationId);
        activity.setTitle(title.strip());
        activity.setFormat(derived);
        activity.setLevel(blankToNull(level));
        activity.setHomeworkType(blankToNull(homeworkType));
        activity.setPosition(activityRepository.maxPosition(presentationId) + 1);
        activity.setTriggerFileId(triggerFileId);
        activity.setTriggerPage(triggerPage);
        activity.setInstructionsText(resolvedInstructions);
        activity.setYoutubeUrl(resolvedYoutube);
        activity.setImageId(resolvedImageId);
        activityRepository.insert(activity);

        questionRepository.replaceQuestions(activity.getId(), questions);
        return toDetail(requireActivity(activity.getId()));
    }

    public ActivityAdminDetail update(UUID id, String title, UUID presentationId, String formatRaw,
                                      String level, String homeworkType,
                                      UUID triggerFileId, Integer triggerPage,
                                      String instructionsText, String youtubeUrl, UUID imageId,
                                      List<HomeworkQuestionDto> questionDtos) {
        Activity existing = requireActivity(id);
        if (title == null || title.isBlank()) {
            throw new ActivityValidationException("El título es obligatorio.");
        }
        UUID targetPresentationId = presentationId != null ? presentationId : existing.getPresentationId();
        if (!activityRepository.presentationExists(targetPresentationId)) {
            throw new ActivityValidationException("La presentación no existe.");
        }
        // Client format is ignored — derived from question kinds.
        UUID resolvedTriggerFile = triggerFileId;
        Integer resolvedTriggerPage = triggerPage;
        // Changing presentation clears invalid triggers
        if (!targetPresentationId.equals(existing.getPresentationId())) {
            if (resolvedTriggerFile != null
                    && !activityRepository.fileBelongsToPresentation(targetPresentationId, resolvedTriggerFile)) {
                resolvedTriggerFile = null;
                resolvedTriggerPage = null;
            }
            existing.setPosition(activityRepository.maxPosition(targetPresentationId) + 1);
        }
        requirePageTrigger(targetPresentationId, resolvedTriggerFile, resolvedTriggerPage);
        String resolvedInstructions = requireInstructionsText(instructionsText);
        String resolvedYoutube = normalizeYoutubeUrl(youtubeUrl);
        UUID resolvedImageId = requireMedia(resolvedYoutube, imageId);
        List<ActivityQuestion> questions = mapQuestions(questionDtos);
        HomeworkFormat derived = HomeworkCompositionSupport.deriveActivityFormat(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());

        existing.setPresentationId(targetPresentationId);
        existing.setTitle(title.strip());
        existing.setFormat(derived);
        existing.setLevel(blankToNull(level));
        existing.setHomeworkType(blankToNull(homeworkType));
        existing.setTriggerFileId(resolvedTriggerFile);
        existing.setTriggerPage(resolvedTriggerPage);
        existing.setInstructionsText(resolvedInstructions);
        existing.setYoutubeUrl(resolvedYoutube);
        existing.setImageId(resolvedImageId);
        activityRepository.update(existing);
        questionRepository.replaceQuestions(id, questions);
        return toDetail(requireActivity(id));
    }

    public void delete(UUID id) {
        Activity activity = requireActivity(id);
        activityRepository.findInstructionsByActivityId(id).ifPresent(f -> {
            instructionsFileStore.deleteQuietly(f.getId());
        });
        if (activityRepository.delete(activity.getId()) == 0) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
    }

    public void reorder(UUID presentationId, List<UUID> activityIds) {
        if (!activityRepository.presentationExists(presentationId)) {
            throw new ActivityValidationException("La presentación no existe.");
        }
        List<UUID> existing = activityRepository.listByPresentationId(presentationId).stream()
                .map(Activity::getId).toList();
        if (activityIds == null
                || activityIds.size() != existing.size()
                || !new HashSet<>(existing).equals(new HashSet<>(activityIds))) {
            throw new ActivityReorderInvalidException(
                    "La lista de actividades no es una permutación completa de la presentación.");
        }
        activityRepository.reorderPositions(presentationId, activityIds);
    }

    // --- review --------------------------------------------------------------

    public List<HomeworkReviewQueueItemDto> getReviewQueue() {
        return submissionRepository.findSubmittedManualQueue().stream()
                .map(r -> new HomeworkReviewQueueItemDto(
                        r.submissionId(), r.studentId(), r.studentEmail(), r.studentFirstName(),
                        r.studentLastName(), r.studentUsername(), r.activityTitle(), r.submittedAt()))
                .toList();
    }

    public HomeworkSubmissionAdminDto getSubmissionDetail(UUID submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
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
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        var view = exerciseGradingService.viewGradedSubmission(submission);
        return new ExerciseSubmissionResultAdminDto(
                submission.getId(),
                activity.getId(),
                activity.getTitle(),
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

        if (storedAnswers.isEmpty()) {
            // Activities have no WRITE path with empty answers in normal use; keep annotate-only fallback.
            List<FormattedTextSegment> response = request == null ? null : request.response();
            FormattedTextSegment.validate(response);
            assertUnchangedWording(response, submission.getResponseText());
            String responseText = FormattedTextSegment.toJson(response);
            submissionRepository.saveAnnotatedReview(submissionId, feedbackJson, responseText, firstReview);
        } else if (composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_MANUAL) {
            finalizeWithValidations(submissionId, request, storedAnswers, kindByQuestion, questions,
                    feedbackJson, firstReview);
        } else {
            throw new IllegalStateException("Unexpected composition for activity review: " + composition);
        }
        ActivitySubmission updated = submissionRepository.findById(submissionId).orElseThrow();
        return toSubmissionAdminDto(updated);
    }

    private void finalizeWithValidations(UUID submissionId, SaveHomeworkFeedbackRequest request,
                               List<HomeworkAnswer> storedAnswers,
                               Map<UUID, QuestionKind> kindByQuestion,
                               List<ActivityQuestion> questions,
                               String feedbackJson, boolean firstReview) {
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
                throw new IllegalArgumentException("Falta la anotación de una respuesta del alumno.");
            }
            TeacherValidation validation = parseTeacherValidation(annotation.teacherValidation());
            FormattedTextSegment.validate(annotation.formatted());
            assertUnchangedWording(annotation.formatted(), stored.getAnswerText());
            double score = HomeworkCompositionSupport.teacherValidationScore(validation);
            answerRepository.updateManualReview(
                    stored.getId(),
                    FormattedTextSegment.toJson(annotation.formatted()),
                    validation.name(),
                    HomeworkCompositionSupport.scoreAsDecimal(score));
            stored.setScore(HomeworkCompositionSupport.scoreAsDecimal(score));
            stored.setTeacherValidation(validation.name());
        }

        Map<UUID, HomeworkAnswer> answersByQ = storedAnswers.stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(HomeworkAnswer::getQuestionId, a -> a, (a, b) -> a));
        for (var refreshed : answerRepository.findBySubmission(submissionId)) {
            if (refreshed.getQuestionId() != null) {
                answersByQ.put(refreshed.getQuestionId(), refreshed);
            }
        }

        List<BigDecimal> scores = new ArrayList<>();
        for (ActivityQuestion q : questions) {
            var answer = answersByQ.get(q.getId());
            scores.add(answer == null || answer.getScore() == null ? BigDecimal.ZERO : answer.getScore());
        }
        int scorePercent = HomeworkCompositionSupport.scorePercentFromScores(scores);
        submissionRepository.saveScoredAnnotatedReview(submissionId, feedbackJson, scorePercent, firstReview);
    }

    private static TeacherValidation parseTeacherValidation(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes validar o invalidar cada respuesta de texto libre.");
        }
        try {
            return TeacherValidation.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Debes validar o invalidar cada respuesta de texto libre.");
        }
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

    private void requirePageTrigger(UUID presentationId, UUID triggerFileId, Integer triggerPage) {
        if (triggerFileId == null || triggerPage == null) {
            throw new ActivityValidationException(
                    "Debes indicar el PDF y la página tras la que se inserta la actividad.");
        }
        if (triggerPage < 1) {
            throw new ActivityValidationException("La página debe ser al menos 1.");
        }
        if (!activityRepository.fileBelongsToPresentation(presentationId, triggerFileId)) {
            throw new ActivityValidationException(
                    "El archivo del disparador no pertenece a la presentación.");
        }
    }

    private static String requireInstructionsText(String instructionsText) {
        if (instructionsText == null || instructionsText.isBlank()) {
            throw new ActivityValidationException("Las instrucciones son obligatorias.");
        }
        return instructionsText.strip();
    }

    /** Blank → null; non-blank must be a valid YouTube URL. */
    private static String normalizeYoutubeUrl(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            return null;
        }
        return YoutubeUrls.extractVideoId(youtubeUrl)
                .map(id -> "https://www.youtube.com/watch?v=" + id)
                .orElseThrow(() -> new ActivityValidationException(
                        "Indica una URL de YouTube válida."));
    }

    private UUID requireMedia(String youtubeUrl, UUID imageId) {
        if ((youtubeUrl == null || youtubeUrl.isBlank()) && imageId == null) {
            throw new ActivityValidationException(
                    "Añade un vídeo de YouTube o una foto (o ambos).");
        }
        if (imageId != null && imageRepository.findById(imageId).isEmpty()) {
            throw new ActivityValidationException("La imagen no existe.");
        }
        return imageId;
    }

    private List<ActivityQuestion> mapQuestions(List<HomeworkQuestionDto> questionDtos) {
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private HomeworkComposition compositionOf(Activity a, List<ActivityQuestion> questions) {
        if (questions != null && !questions.isEmpty()) {
            return HomeworkCompositionSupport.activityComposition(
                    questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        }
        // Fall back to stored format when questions not loaded for list rows.
        if (a.getFormat() == HomeworkFormat.MIXED) return HomeworkComposition.MIXED;
        if (a.getFormat() == HomeworkFormat.EXERCISE) return HomeworkComposition.ALL_AUTO;
        return HomeworkComposition.ALL_MANUAL;
    }

    private ActivityAdminItem toItem(Activity a, String presentationTitle, boolean hasInstructions) {
        HomeworkComposition composition = compositionOf(a, null);
        return new ActivityAdminItem(
                a.getId(), a.getTitle(), a.getFormat().name(), composition.name(),
                a.getLevel(), a.getHomeworkType(),
                a.getPresentationId(), presentationTitle, a.getPosition(),
                a.getTriggerFileId(), a.getTriggerPage(),
                a.getInstructionsText(), a.getYoutubeUrl(), a.getImageId(), hasInstructions,
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private ActivityAdminDetail toDetail(Activity a) {
        String presentationTitle = presentationRepository.findById(a.getPresentationId())
                .map(p -> p.getTitle())
                .orElse("");
        var instructions = activityRepository.findInstructionsByActivityId(a.getId()).orElse(null);
        List<ActivityQuestion> questionModels = questionRepository.findByActivityId(a.getId());
        List<HomeworkQuestionDto> questions = questionModels.stream().map(this::toQuestionDto).toList();
        HomeworkComposition composition = compositionOf(a, questionModels);
        ActivityAdminDetail.InstructionsMeta meta = instructions == null ? null
                : new ActivityAdminDetail.InstructionsMeta(
                        instructions.getId(), instructions.getOriginalName(),
                        instructions.getContentType(), instructions.getByteSize());
        return new ActivityAdminDetail(
                a.getId(), a.getTitle(), a.getFormat().name(), composition.name(),
                a.getLevel(), a.getHomeworkType(),
                a.getPresentationId(), presentationTitle, a.getPosition(),
                a.getTriggerFileId(), a.getTriggerPage(),
                a.getInstructionsText(), a.getYoutubeUrl(), a.getImageId(), instructions != null,
                a.getCreatedAt(), a.getUpdatedAt(), questions, meta);
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
                        a.getTeacherValidation(),
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
                activity.getTitle(),
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
                submission.getSubmittedAt(),
                submission.getReviewedAt());
    }
}
