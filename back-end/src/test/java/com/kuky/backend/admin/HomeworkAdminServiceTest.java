package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HomeworkAdminServiceTest {

    private ContentRepository contentRepository;
    private HomeworkTargetRepository targetRepository;
    private HomeworkQuestionRepository questionRepository;
    private UserRepository userRepository;
    private HomeworkAdminService service;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        userRepository = mock(UserRepository.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository, userRepository);

        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private HomeworkAssignment assignment(UUID id) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(id);
        a.setTitle("Tarea");
        a.setInstructions("Hazla");
        a.setDueOn(LocalDate.of(2026, 6, 20));
        return a;
    }

    @Test
    void createAssignsTargetsAndReturnsItem() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of(
                new HomeworkTargetRepository.AssigneeView(studentId, "ana@example.com",
                        null, null, null, "SUBMITTED", "Mi respuesta", Instant.now(), null)));

        HomeworkAdminItem item = service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", LocalDate.of(2026, 6, 20), null, null, "MANUAL", List.of(), List.of(studentId)));

        verify(targetRepository).replaceTargets(id, List.of(studentId));
        assertThat(item.assignees()).hasSize(1);
        assertThat(item.assignees().get(0).status()).isEqualTo("SUBMITTED");
        assertThat(item.assignees().get(0).responseText()).isEqualTo("Mi respuesta");
    }

    @Test
    void createRejectsUnknownStudent() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, null, null, "MANUAL", List.of(), List.of(unknown))))
                .isInstanceOf(StudentNotFoundException.class);
        verify(contentRepository, never()).insertAssignment(any(), any(), any(), any(), any(), any());
    }

    @Test
    void setAssigneesReplacesTargets() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.setAssignees(id, List.of(studentId));

        verify(targetRepository).replaceTargets(id, List.of(studentId));
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(contentRepository.deleteAssignment(id)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void updateThrowsWhenAssignmentMissing() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id,
                new com.kuky.backend.admin.dto.UpdateHomeworkRequest("T", "I", null, null, null, "MANUAL", List.of())))
                .isInstanceOf(AssignmentNotFoundException.class);
        verify(contentRepository, never()).updateAssignment(eq(id), any(), any(), any(), any(), any(), any());
    }
}
