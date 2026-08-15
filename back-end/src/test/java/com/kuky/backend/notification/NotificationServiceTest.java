package com.kuky.backend.notification;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.notification.dto.BadgeSummary;
import com.kuky.backend.notification.repository.NotificationRepository;
import com.kuky.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationService(repository);
    }

    @Test
    void emptyDb_allBadgesFalse() {
        when(repository.hasUnseenHomeworkSubmissions()).thenReturn(false);
        when(repository.hasUnseenQuizAttempts()).thenReturn(false);
        User admin = user("ADMIN");
        assertThat(service.badges(admin)).isEqualTo(BadgeSummary.none());
    }

    @Test
    void userRole_learningAlwaysFalse() {
        User user = user("USER");
        when(repository.hasUnseenLearning(user.getId())).thenReturn(true);
        when(repository.hasUnseenHomeworkSubmissions()).thenReturn(true);
        when(repository.hasUnseenQuizAttempts()).thenReturn(true);
        assertThat(service.badges(user)).isEqualTo(BadgeSummary.none());
    }

    @Test
    void adminWithNoUnseen_panelFalse() {
        User admin = user("ADMIN");
        when(repository.hasUnseenHomeworkSubmissions()).thenReturn(false);
        when(repository.hasUnseenQuizAttempts()).thenReturn(false);
        BadgeSummary badges = service.badges(admin);
        assertThat(badges.panel()).isFalse();
        assertThat(badges.homework()).isFalse();
        assertThat(badges.quiz()).isFalse();
        assertThat(badges.learning()).isFalse();
    }

    @Test
    void adminHomeworkUnseen_setsPanelAndHomework() {
        User admin = user("ADMIN");
        when(repository.hasUnseenHomeworkSubmissions()).thenReturn(true);
        when(repository.hasUnseenQuizAttempts()).thenReturn(false);
        BadgeSummary badges = service.badges(admin);
        assertThat(badges.panel()).isTrue();
        assertThat(badges.homework()).isTrue();
        assertThat(badges.quiz()).isFalse();
    }

    @Test
    void studentLearningUnseen_setsLearningOnly() {
        User student = user("STUDENT");
        when(repository.hasUnseenLearning(student.getId())).thenReturn(true);
        BadgeSummary badges = service.badges(student);
        assertThat(badges.learning()).isTrue();
        assertThat(badges.panel()).isFalse();
        assertThat(badges.homework()).isFalse();
        assertThat(badges.quiz()).isFalse();
    }

    private static User user(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(role.toLowerCase() + "@example.com");
        u.setRole(role);
        return u;
    }
}
