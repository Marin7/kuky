package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.dto.ActivityAdminItem;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.ReorderActivitiesRequest;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.admin.dto.SaveExerciseFeedbackRequest;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.service.ActivityAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class ActivityAdminController {

    private final ActivityAdminService service;

    public ActivityAdminController(ActivityAdminService service) {
        this.service = service;
    }

    @GetMapping("/activities")
    public List<ActivityAdminItem> list(@RequestParam(required = false) UUID presentationId) {
        return service.list(presentationId);
    }

    // Review routes before /{id} so "submissions" is not parsed as a UUID path var.
    @GetMapping("/activities/submissions")
    public List<HomeworkReviewQueueItemDto> reviewQueue() {
        return service.getReviewQueue();
    }

    @GetMapping("/activities/submissions/{submissionId}")
    public HomeworkSubmissionAdminDto submissionDetail(@PathVariable UUID submissionId) {
        return service.getSubmissionDetail(submissionId);
    }

    @GetMapping("/activities/submissions/{submissionId}/exercise-result")
    public ExerciseSubmissionResultAdminDto exerciseResult(@PathVariable UUID submissionId) {
        return service.getExerciseResult(submissionId);
    }

    @PutMapping("/activities/submissions/{submissionId}/feedback")
    public HomeworkSubmissionAdminDto saveFeedback(@PathVariable UUID submissionId,
                                                   @Valid @RequestBody SaveHomeworkFeedbackRequest request) {
        return service.saveFeedback(submissionId, request.feedback());
    }

    @PutMapping("/activities/submissions/{submissionId}/exercise-feedback")
    public ExerciseSubmissionResultAdminDto saveExerciseFeedback(
            @PathVariable UUID submissionId,
            @Valid @RequestBody SaveExerciseFeedbackRequest request) {
        return service.saveExerciseFeedback(submissionId, request.feedback());
    }

    @GetMapping("/activities/{id}")
    public ActivityAdminDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/activities")
    public ResponseEntity<ActivityAdminDetail> create(@Valid @RequestBody SaveActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/activities/{id}")
    public ActivityAdminDetail update(@PathVariable UUID id,
                                      @Valid @RequestBody SaveActivityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/presentations/{presentationId}/activities/reorder")
    public ResponseEntity<Void> reorder(@PathVariable UUID presentationId,
                                        @Valid @RequestBody ReorderActivitiesRequest request) {
        service.reorder(presentationId, request.activityIds());
        return ResponseEntity.noContent().build();
    }
}
