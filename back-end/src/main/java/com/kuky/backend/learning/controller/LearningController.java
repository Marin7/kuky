package com.kuky.backend.learning.controller;

import com.kuky.backend.learning.dto.ExerciseResponse;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.dto.SubmitHomeworkRequest;
import com.kuky.backend.learning.service.ExerciseGradingService;
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
    private final ExerciseGradingService gradingService;

    public LearningController(LearningService learningService,
                             HomeworkSubmissionService submissionService,
                             ExerciseGradingService gradingService) {
        this.learningService = learningService;
        this.submissionService = submissionService;
        this.gradingService = gradingService;
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
        var response = request == null ? null : request.response();
        return ResponseEntity.ok(submissionService.submit(email, assignmentId, response));
    }

    @GetMapping("/homework/{assignmentId}")
    public ResponseEntity<ExerciseResponse> getExercise(
            @AuthenticationPrincipal String email,
            @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(gradingService.getExercise(email, assignmentId));
    }

    @PutMapping("/homework/{assignmentId}/answers")
    public ResponseEntity<ExerciseResultResponse> submitExercise(
            @AuthenticationPrincipal String email,
            @PathVariable UUID assignmentId,
            @RequestBody(required = false) SubmitExerciseRequest request) {
        return ResponseEntity.ok(gradingService.submit(email, assignmentId, request));
    }
}
