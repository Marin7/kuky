package com.kuky.backend.notification.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.notification.dto.BadgeSummary;
import com.kuky.backend.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public BadgeSummary badges(User user) {
        if (user == null || user.getRole() == null) {
            return BadgeSummary.none();
        }
        boolean homework = false;
        boolean quiz = false;
        boolean learning = false;
        if ("ADMIN".equals(user.getRole())) {
            homework = repository.hasUnseenHomeworkSubmissions();
            quiz = repository.hasUnseenQuizAttempts();
        }
        if ("STUDENT".equals(user.getRole())) {
            learning = repository.hasUnseenLearning(user.getId());
        }
        return new BadgeSummary(homework || quiz, homework, quiz, learning);
    }

    public void markHomeworkSeen(UUID submissionId) {
        repository.markHomeworkTeacherSeen(submissionId);
    }

    public void markQuizAttemptSeen(UUID attemptId) {
        repository.markQuizAttemptTeacherSeen(attemptId);
    }

    public boolean markUnitSeen(UUID unitId, UUID userId) {
        if (!repository.isUnitAssigned(unitId, userId)) {
            return false;
        }
        repository.markUnitStudentSeen(unitId, userId);
        return true;
    }

    public void markQuizAssigneeSeen(UUID quizId, UUID userId) {
        repository.markQuizAssigneeStudentSeen(quizId, userId);
    }

    public boolean markHomeworkSeen(UUID assignmentId, UUID userId) {
        if (!repository.isHomeworkAssigned(assignmentId, userId)) {
            return false;
        }
        repository.markHomeworkTargetStudentSeen(assignmentId, userId);
        return true;
    }

    public Set<UUID> unseenUnitIds(UUID userId) {
        return repository.findUnseenUnitIds(userId);
    }

    public Set<UUID> unseenQuizIds(UUID userId) {
        return repository.findUnseenQuizIds(userId);
    }

    public Set<UUID> unseenHomeworkIds(UUID userId) {
        return repository.findUnseenHomeworkIds(userId);
    }
}
