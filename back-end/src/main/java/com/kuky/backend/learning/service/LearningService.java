package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.PastClassResponse;
import com.kuky.backend.learning.dto.PresentationBlockResponse;
import com.kuky.backend.learning.dto.SharedPresentationSummary;
import com.kuky.backend.learning.dto.UnitRef;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
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
    private final PresentationFileStore presentationFileStore;
    private final SchedulingProperties props;

    public LearningService(ContentRepository contentRepository,
                           HomeworkSubmissionRepository submissionRepository,
                           UserRepository userRepository,
                           PresentationRepository presentationRepository,
                           PresentationFileStore presentationFileStore,
                           SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.presentationRepository = presentationRepository;
        this.presentationFileStore = presentationFileStore;
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

        Map<UUID, ContentRepository.AssignmentUnit> assignmentUnits = contentRepository
                .findAssignmentUnitsForUser(user.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ContentRepository.AssignmentUnit::assignmentId,
                        au -> au));

        List<HomeworkItemResponse> homework = contentRepository.findAssignmentsForUser(user.getId()).stream()
                .map(a -> {
                    ContentRepository.AssignmentUnit au = assignmentUnits.get(a.getId());
                    UnitRef unit = au == null ? null
                            : new UnitRef(au.unitId(), au.level(), au.subject(), au.position());
                    Integer unitPosition = au == null ? null : au.unitPosition();
                    return HomeworkItems.toResponse(a, submissionsByAssignment.get(a.getId()), today,
                            unit, unitPosition);
                })
                .toList();

        var sharedRows = presentationRepository.findSharedSummariesForUser(user.getId());
        Map<UUID, List<com.kuky.backend.admin.dto.PresentationFileSummary>> filesByPresentation =
                presentationRepository.listFilesGrouped(sharedRows.stream().map(s -> s.id()).toList());
        List<SharedPresentationSummary> sharedPresentations = sharedRows.stream()
                .map(s -> new SharedPresentationSummary(
                        s.id(),
                        s.title(),
                        filesByPresentation.getOrDefault(s.id(), List.of()),
                        s.unit() == null ? null
                                : new UnitRef(
                                        s.unit().id(), s.unit().level(), s.unit().subject(),
                                        s.unit().position()),
                        s.contentUnitPosition()))
                .toList();

        return new LearningResponse(presentation, pastClasses, homework, sharedPresentations);
    }

    /** Downloads one file for a shared presentation (PPTX or PDF). Enforces share-gate. */
    public PresentationFile getPresentationFile(String userEmail, UUID presentationId, UUID fileId) {
        User user = requireUser(userEmail);
        if (!presentationRepository.isSharedWith(presentationId, user.getId())) {
            throw new PresentationNotFoundException("Presentación no encontrada.");
        }
        PresentationFile meta = presentationRepository.findFile(presentationId, fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        byte[] data = presentationFileStore.read(fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        return new PresentationFile(
                meta.id(), meta.presentationId(), meta.originalName(), meta.displayName(),
                meta.contentType(), meta.byteSize(), meta.createdAt(), data);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private ZoneId teacherZone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }
}
