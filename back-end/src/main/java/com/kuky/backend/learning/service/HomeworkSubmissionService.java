package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.ManualAnswerDto;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.QuestionKind;
import org.springframework.http.HttpStatus;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles a student submitting / marking done a homework assignment. Enforces the
 * PENDING → SUBMITTED lifecycle (REVIEWED is read-only to students) and persists
 * the per-student submission.
 */
@Service
public class HomeworkSubmissionService {

    private static final int MAX_PLAIN_ANSWER_CHARS = 2000;

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final SchedulingProperties props;

    public HomeworkSubmissionService(ContentRepository contentRepository,
                                     HomeworkSubmissionRepository submissionRepository,
                                     HomeworkQuestionRepository questionRepository,
                                     HomeworkAnswerRepository answerRepository,
                                     UserRepository userRepository,
                                     SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    @Transactional
    public HomeworkItemResponse submit(String userEmail, UUID assignmentId,
                                       List<FormattedTextSegment> response,
                                       List<ManualAnswerDto> answers) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        HomeworkAssignment assignment = contentRepository.findPublishedAssignmentById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));

        if (assignment.getFormat() == HomeworkFormat.EXERCISE) {
            throw new SubmissionNotAllowedException(
                    "Este ejercicio se entrega desde su propia página.", HttpStatus.BAD_REQUEST);
        }

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);

        if (existing.isPresent() && HomeworkStatus.REVIEWED.name().equals(existing.get().getStatus())) {
            throw new SubmissionNotAllowedException("Esta tarea ya ha sido revisada y no puede modificarse.");
        }

        boolean multi = HomeworkItems.isMultiManual(assignment);
        HomeworkSubmission saved;
        if (multi) {
            if (response != null && !response.isEmpty()) {
                throw new IllegalArgumentException("Esta tarea se entrega con respuestas por pregunta.");
            }
            List<HomeworkQuestion> questions = freeTextQuestions(assignmentId);
            List<HomeworkAnswer> mapped = validateAndMapFreeTextAnswers(questions, answers);
            saved = submissionRepository.upsert(
                    user.getId(),
                    assignmentId,
                    HomeworkStatus.SUBMITTED.name(),
                    null,
                    Instant.now());
            answerRepository.saveAll(saved.getId(), mapped);
        } else {
            if (answers != null && !answers.isEmpty()) {
                throw new IllegalArgumentException("Esta tarea de escritura no admite respuestas por pregunta.");
            }
            if (response != null) {
                FormattedTextSegment.validate(response);
            }
            saved = submissionRepository.upsert(
                    user.getId(),
                    assignmentId,
                    HomeworkStatus.SUBMITTED.name(),
                    FormattedTextSegment.toJson(response),
                    Instant.now());
        }

        return toItem(assignment, saved);
    }

    HomeworkItemResponse toItem(HomeworkAssignment assignment, HomeworkSubmission submission) {
        LocalDate today = LocalDate.now(ZoneId.of(props.getScheduling().getTeacherTimezone()));
        if (!HomeworkItems.isMultiManual(assignment)) {
            return HomeworkItems.toResponse(assignment, submission, today);
        }
        List<HomeworkQuestion> questions = freeTextQuestions(assignment.getId());
        List<HomeworkAnswer> answers = submission == null
                ? List.of()
                : answerRepository.findBySubmission(submission.getId());
        return HomeworkItems.toResponse(assignment, submission, today, null, null, questions, answers);
    }

    private List<HomeworkQuestion> freeTextQuestions(UUID assignmentId) {
        return questionRepository.findByAssignment(assignmentId).stream()
                .filter(q -> q.getKind() == QuestionKind.FREE_TEXT)
                .toList();
    }

    static List<HomeworkAnswer> validateAndMapFreeTextAnswers(List<HomeworkQuestion> questions,
                                                              List<ManualAnswerDto> answers) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Esta tarea no tiene preguntas configuradas.");
        }
        List<ManualAnswerDto> given = answers == null ? List.of() : answers;
        Map<UUID, String> byQuestion = new HashMap<>();
        for (ManualAnswerDto a : given) {
            if (a == null || a.questionId() == null) {
                throw new IllegalArgumentException("Cada respuesta necesita una pregunta.");
            }
            if (byQuestion.containsKey(a.questionId())) {
                throw new IllegalArgumentException("Hay respuestas duplicadas para la misma pregunta.");
            }
            String text = a.text() == null ? "" : a.text().strip();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            if (text.length() > MAX_PLAIN_ANSWER_CHARS) {
                throw new IllegalArgumentException("Una de las respuestas es demasiado larga.");
            }
            byQuestion.put(a.questionId(), text);
        }

        Set<UUID> expected = new HashSet<>();
        List<HomeworkAnswer> mapped = new ArrayList<>();
        for (HomeworkQuestion q : questions) {
            expected.add(q.getId());
            String text = byQuestion.get(q.getId());
            if (text == null) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            HomeworkAnswer row = new HomeworkAnswer();
            row.setQuestionId(q.getId());
            row.setAnswerText(text);
            row.setPromptSnapshot(q.getPrompt());
            row.setScore(BigDecimal.ZERO);
            row.setSelectedOptionIds(List.of());
            mapped.add(row);
        }
        if (!byQuestion.keySet().equals(expected)) {
            throw new IllegalArgumentException("Las respuestas no coinciden con las preguntas actuales.");
        }
        return mapped;
    }
}
