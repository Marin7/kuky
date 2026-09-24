package com.kuky.backend.preferences.controller;

import com.kuky.backend.preferences.dto.EmailPreferenceResponse;
import com.kuky.backend.preferences.dto.UpdateEmailPreferenceRequest;
import com.kuky.backend.preferences.service.EmailPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The signed-in account's own email preferences.
 *
 * <p>The account is taken from the authenticated principal and never from a path or body
 * parameter — that is what makes "only this student can see or change it" and "the teacher
 * cannot" structural rather than a check that could be forgotten. There is deliberately no
 * admin-facing counterpart anywhere in the API.
 */
@RestController
@RequestMapping("/api/v1/me/email-preferences")
public class EmailPreferencesController {

    private final EmailPreferencesService service;

    public EmailPreferencesController(EmailPreferencesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<EmailPreferenceResponse>>> list(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(Map.of("preferences", service.list(email)));
    }

    @PutMapping("/{type}")
    public ResponseEntity<EmailPreferenceResponse> update(
            @AuthenticationPrincipal String email,
            @PathVariable String type,
            @Valid @RequestBody UpdateEmailPreferenceRequest request) {
        return ResponseEntity.ok(service.set(email, type, request.enabled()));
    }
}
