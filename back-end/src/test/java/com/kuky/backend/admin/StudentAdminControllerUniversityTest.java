package com.kuky.backend.admin;

import com.kuky.backend.admin.controller.StudentAdminController;
import com.kuky.backend.admin.dto.GrantUniversityStudentRequest;
import com.kuky.backend.admin.exception.RoleConflictException;
import com.kuky.backend.admin.service.StudentProfileAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentAdminControllerUniversityTest {
    @Test
    void universityGrantRejectsPrivateStudent() {
        UserRepository users = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        User student = new User(); student.setRole("STUDENT");
        when(users.findById(id)).thenReturn(Optional.of(student));
        StudentAdminController controller = new StudentAdminController(users, mock(StudentProfileAdminService.class), mock(EmailService.class));

        assertThrows(RoleConflictException.class,
                () -> controller.grantUniversityStudent(id, new GrantUniversityStudentRequest("BEGINNER")));
        verify(users, never()).grantUniversityStudentById(any(), anyString());
    }

    @Test
    void privateGrantRejectsUniversityStudent() {
        UserRepository users = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        User student = new User(); student.setRole("UNIVERSITY_STUDENT");
        when(users.findById(id)).thenReturn(Optional.of(student));
        StudentAdminController controller = new StudentAdminController(users, mock(StudentProfileAdminService.class), mock(EmailService.class));

        assertThrows(RoleConflictException.class, () -> controller.grantStudent(id));
        verify(users, never()).promoteToStudentById(any());
    }
}
