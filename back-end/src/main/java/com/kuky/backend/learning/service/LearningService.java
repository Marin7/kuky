package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.PastClassResponse;
import com.kuky.backend.learning.dto.PresentationBlockResponse;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Assembles the "Mi aprendizaje" overview for a student: shared presentation +
 * past classes, plus homework with the caller's per-student status (defaulting to
 * PENDING when no submission exists) and a derived overdue flag.
 */
@Service
public class LearningService {

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SchedulingProperties props;

    public LearningService(ContentRepository contentRepository,
                           HomeworkSubmissionRepository submissionRepository,
                           UserRepository userRepository,
                           SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    public LearningResponse getOverview(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        List<PresentationBlockResponse> presentation = contentRepository.findPublishedPresentation().stream()
                .map(p -> new PresentationBlockResponse(p.getHeading(), p.getBody()))
                .toList();

        List<PastClassResponse> pastClasses = contentRepository.findPublishedPastClasses().stream()
                .map(c -> new PastClassResponse(c.getId(), c.getTitle(), c.getHeldOn(), c.getTeacherNote()))
                .toList();

        Map<UUID, HomeworkSubmission> submissionsByAssignment = submissionRepository.findByUserId(user.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        HomeworkSubmission::getAssignmentId, Function.identity()));

        LocalDate today = LocalDate.now(teacherZone());

        List<HomeworkItemResponse> homework = contentRepository.findPublishedAssignments().stream()
                .map(a -> HomeworkItems.toResponse(a, submissionsByAssignment.get(a.getId()), today))
                .toList();

        return new LearningResponse(presentation, pastClasses, homework);
    }

    private ZoneId teacherZone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }
}
