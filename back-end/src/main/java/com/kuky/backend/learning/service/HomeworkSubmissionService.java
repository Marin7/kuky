package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    public HomeworkItemResponse submit(String userEmail, UUID assignmentId, String response) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        HomeworkAssignment assignment = contentRepository.findPublishedAssignmentById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);

        if (existing.isPresent() && HomeworkStatus.REVIEWED.name().equals(existing.get().getStatus())) {
            throw new SubmissionNotAllowedException("Esta tarea ya ha sido revisada y no puede modificarse.");
        }

        HomeworkSubmission saved = submissionRepository.upsert(
                user.getId(),
                assignmentId,
                HomeworkStatus.SUBMITTED.name(),
                response,
                Instant.now());

        LocalDate today = LocalDate.now(ZoneId.of(props.getScheduling().getTeacherTimezone()));
        return HomeworkItems.toResponse(assignment, saved, today);
    }
}
