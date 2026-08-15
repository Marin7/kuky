package com.kuky.backend.notification.controller;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.notification.dto.BadgeSummary;
import com.kuky.backend.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/badges")
    public ResponseEntity<BadgeSummary> badges(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT)).orElse(null);
        return ResponseEntity.ok(notificationService.badges(user));
    }
}
