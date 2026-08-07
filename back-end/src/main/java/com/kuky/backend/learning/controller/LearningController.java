package com.kuky.backend.learning.controller;

import com.kuky.backend.learning.dto.ActivityItemResponse;
import com.kuky.backend.learning.dto.ExerciseResponse;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.dto.SubmitHomeworkRequest;
import com.kuky.backend.learning.service.ActivityStudentService;
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
    private final ActivityStudentService activityStudentService;

    public LearningController(LearningService learningService,
                             HomeworkSubmissionService submissionService,
                             ExerciseGradingService gradingService,
                             ActivityStudentService activityStudentService) {
        this.learningService = learningService;
        this.submissionService = submissionService;
        this.gradingService = gradingService;
        this.activityStudentService = activityStudentService;
    }

    @GetMapping
    public ResponseEntity<LearningResponse> getOverview(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(learningService.getOverview(email));
    }

    @GetMapping("/presentations/{id}/files/{fileId}")
    public ResponseEntity<byte[]> downloadPresentation(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id,
            @PathVariable UUID fileId) {
        PresentationFile f = learningService.getPresentationFile(email, id, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(f.displayName(), StandardCharsets.UTF_8)
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

    @GetMapping("/activities/{id}")
    public ResponseEntity<ActivityItemResponse> getActivity(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id) {
        return ResponseEntity.ok(activityStudentService.get(email, id));
    }

    @GetMapping("/activities/{id}/instructions")
    public ResponseEntity<byte[]> getActivityInstructions(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id) {
        var pdf = activityStudentService.getInstructions(email, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(pdf.meta().getOriginalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(pdf.data());
    }

    @PutMapping("/activities/{id}")
    public ResponseEntity<ActivityItemResponse> submitActivity(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) SubmitHomeworkRequest request) {
        var response = request == null ? null : request.response();
        return ResponseEntity.ok(activityStudentService.submitManual(email, id, response));
    }

    @PutMapping("/activities/{id}/answers")
    public ResponseEntity<ExerciseResultResponse> submitActivityExercise(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitExerciseRequest request) {
        return ResponseEntity.ok(activityStudentService.submitExercise(email, id, request));
    }
}
