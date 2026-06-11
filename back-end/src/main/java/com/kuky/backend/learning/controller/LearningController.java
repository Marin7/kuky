package com.kuky.backend.learning.controller;

import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.SubmitHomeworkRequest;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import com.kuky.backend.learning.service.LearningService;
import com.kuky.backend.presentations.model.PresentationFile;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    @GetMapping("/presentations/{id}/file")
    public ResponseEntity<byte[]> downloadPresentation(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id) {
        PresentationFile f = learningService.getPresentationFile(email, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(f.originalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(f.data());
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
