package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.dto.ActivityReviewQueueItemDto;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.admin.dto.SaveExerciseFeedbackRequest;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.service.ActivityAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Page activities are created from their unit ({@code POST /admin/units/{id}/activities}). */
@RestController
@RequestMapping("/api/v1/admin")
public class ActivityAdminController {

    private final ActivityAdminService service;

    public ActivityAdminController(ActivityAdminService service) {
        this.service = service;
    }

    // Review routes before /{id} so "submissions" is not parsed as a UUID path var.
    @GetMapping("/activities/submissions")
    public List<ActivityReviewQueueItemDto> reviewQueue(@RequestParam(required = false) UUID unitId) {
        return service.getReviewQueue(unitId);
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
        return service.saveFeedback(submissionId, request);
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

    @PutMapping("/activities/{id}")
    public ActivityAdminDetail update(@PathVariable UUID id,
                                      @Valid @RequestBody SaveActivityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "false") boolean deleteSubmissions) {
        service.delete(id, deleteSubmissions);
        return ResponseEntity.noContent().build();
    }
}
