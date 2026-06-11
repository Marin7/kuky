package com.kuky.backend.admin.service;

import com.kuky.backend.admin.dto.AssigneeDto;
import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Teacher-side homework authoring + assignment + submission review. */
@Service
@Transactional
public class HomeworkAdminService {

    private final ContentRepository contentRepository;
    private final HomeworkTargetRepository targetRepository;
    private final UserRepository userRepository;

    public HomeworkAdminService(ContentRepository contentRepository,
                                HomeworkTargetRepository targetRepository,
                                UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
    }

    public List<HomeworkAdminItem> list() {
        return contentRepository.findAllAssignments().stream()
                .map(this::toItem)
                .toList();
    }

    public HomeworkAdminItem create(CreateHomeworkRequest req) {
        List<UUID> assignees = req.assigneeIds() == null ? List.of() : req.assigneeIds();
        validateStudents(assignees);
        UUID id = contentRepository.insertAssignment(req.title(), req.instructions(), req.dueOn());
        if (!assignees.isEmpty()) {
            targetRepository.replaceTargets(id, assignees);
        }
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem update(UUID id, UpdateHomeworkRequest req) {
        requireAssignment(id);
        contentRepository.updateAssignment(id, req.title(), req.instructions(), req.dueOn());
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem setAssignees(UUID id, List<UUID> assigneeIds) {
        requireAssignment(id);
        validateStudents(assigneeIds);
        targetRepository.replaceTargets(id, assigneeIds);
        return toItem(requireAssignment(id));
    }

    public void delete(UUID id) {
        if (contentRepository.deleteAssignment(id) == 0) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
    }

    // --- helpers -------------------------------------------------------------

    private HomeworkAssignment requireAssignment(UUID id) {
        return contentRepository.findAssignmentById(id)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
    }

    private void validateStudents(List<UUID> userIds) {
        for (UUID userId : userIds) {
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
            if (!"STUDENT".equals(u.getRole())) {
                throw new StudentNotFoundException("El destinatario no es un alumno.");
            }
        }
    }

    private HomeworkAdminItem toItem(HomeworkAssignment a) {
        List<AssigneeDto> assignees = targetRepository.findAssigneesWithSubmissions(a.getId()).stream()
                .map(v -> new AssigneeDto(v.userId(), v.email(), v.status(), v.responseText(), v.submittedAt()))
                .toList();
        return new HomeworkAdminItem(a.getId(), a.getTitle(), a.getInstructions(), a.getDueOn(), assignees);
    }
}
