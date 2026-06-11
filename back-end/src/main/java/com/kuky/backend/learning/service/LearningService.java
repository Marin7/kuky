package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.PastClassResponse;
import com.kuky.backend.learning.dto.PresentationBlockResponse;
import com.kuky.backend.learning.dto.SharedPresentationSummary;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class LearningService {

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final PresentationRepository presentationRepository;
    private final SchedulingProperties props;

    public LearningService(ContentRepository contentRepository,
                           HomeworkSubmissionRepository submissionRepository,
                           UserRepository userRepository,
                           PresentationRepository presentationRepository,
                           SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.presentationRepository = presentationRepository;
        this.props = props;
    }

    public LearningResponse getOverview(String userEmail) {
        User user = requireUser(userEmail);

        List<PresentationBlockResponse> presentation = contentRepository.findPublishedPresentation().stream()
                .map(p -> new PresentationBlockResponse(p.getHeading(), p.getBody()))
                .toList();

        LocalDate enrolledOn = user.getCreatedAt().atZone(teacherZone()).toLocalDate();
        List<PastClassResponse> pastClasses = contentRepository.findPublishedPastClassesSince(enrolledOn).stream()
                .map(c -> new PastClassResponse(c.getId(), c.getTitle(), c.getHeldOn(), c.getTeacherNote()))
                .toList();

        Map<UUID, HomeworkSubmission> submissionsByAssignment = submissionRepository.findByUserId(user.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        HomeworkSubmission::getAssignmentId, Function.identity()));

        LocalDate today = LocalDate.now(teacherZone());

        List<HomeworkItemResponse> homework = contentRepository.findAssignmentsForUser(user.getId()).stream()
                .map(a -> HomeworkItems.toResponse(a, submissionsByAssignment.get(a.getId()), today))
                .toList();

        List<SharedPresentationSummary> sharedPresentations =
                presentationRepository.findSharedSummariesForUser(user.getId()).stream()
                        .map(s -> new SharedPresentationSummary(s.id(), s.title(), s.hasFile()))
                        .toList();

        return new LearningResponse(presentation, pastClasses, homework, sharedPresentations);
    }

    /** Downloads the .pptx file for a shared presentation. Enforces share-gate. */
    public PresentationFile getPresentationFile(String userEmail, UUID presentationId) {
        User user = requireUser(userEmail);
        if (!presentationRepository.isSharedWith(presentationId, user.getId())) {
            throw new PresentationNotFoundException("Presentación no encontrada.");
        }
        return presentationRepository.findFile(presentationId)
                .orElseThrow(() -> new PresentationNotFoundException("No hay archivo para esta presentación."));
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private ZoneId teacherZone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }
}
