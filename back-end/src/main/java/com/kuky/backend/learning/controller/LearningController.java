package com.kuky.backend.learning.controller;

import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.SubmitHomeworkRequest;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import com.kuky.backend.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * "Mi aprendizaje" — student-only section. Every endpoint requires authentication
 * (covered by the existing {@code anyRequest().authenticated()} rule; guests get 401).
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final LearningService learningService;
    private final HomeworkSubmissionService submissionService;

    public LearningController(LearningService learningService,
                             HomeworkSubmissionService submissionService) {
        this.learningService = learningService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public ResponseEntity<LearningResponse> getOverview(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(learningService.getOverview(email));
    }

    @PutMapping("/homework/{assignmentId}")
    public ResponseEntity<HomeworkItemResponse> submitHomework(
            @AuthenticationPrincipal String email,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody(required = false) SubmitHomeworkRequest request) {
        String response = request == null ? null : request.response();
        return ResponseEntity.ok(submissionService.submit(email, assignmentId, response));
    }
}
