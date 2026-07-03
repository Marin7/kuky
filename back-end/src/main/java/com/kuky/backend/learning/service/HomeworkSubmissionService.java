package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import org.springframework.http.HttpStatus;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles a student submitting / marking done a homework assignment. Enforces the
 * PENDING → SUBMITTED lifecycle (REVIEWED is read-only to students) and persists
 * the per-student submission.
 */
@Service
public class HomeworkSubmissionService {

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SchedulingProperties props;

    public HomeworkSubmissionService(ContentRepository contentRepository,
                                     HomeworkSubmissionRepository submissionRepository,
                                     UserRepository userRepository,
                                     SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    public HomeworkItemResponse submit(String userEmail, UUID assignmentId, List<FormattedTextSegment> response) {
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

        // A MANUAL homework may be "marked done" with no answer text at all (response == null);
        // when text is provided, it must pass the same formatted-text rules as teacher feedback.
        if (response != null) {
            FormattedTextSegment.validate(response);
        }

        HomeworkSubmission saved = submissionRepository.upsert(
                user.getId(),
                assignmentId,
                HomeworkStatus.SUBMITTED.name(),
                FormattedTextSegment.toJson(response),
                Instant.now());

        LocalDate today = LocalDate.now(ZoneId.of(props.getScheduling().getTeacherTimezone()));
        return HomeworkItems.toResponse(assignment, saved, today);
    }
}
