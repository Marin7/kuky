package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.dto.SetAssigneesRequest;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.service.HomeworkAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/homework")
public class HomeworkAdminController {

    private final HomeworkAdminService service;

    public HomeworkAdminController(HomeworkAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<HomeworkAdminItem> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public HomeworkAdminItem get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<HomeworkAdminItem> create(@Valid @RequestBody CreateHomeworkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public HomeworkAdminItem update(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateHomeworkRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/assignees")
    public HomeworkAdminItem setAssignees(@PathVariable UUID id,
                                          @Valid @RequestBody SetAssigneesRequest request) {
        return service.setAssignees(id, request.assigneeIds());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Teacher review of MANUAL submissions --------------------------------

    @GetMapping("/submissions")
    public List<HomeworkReviewQueueItemDto> reviewQueue() {
        return service.getReviewQueue();
    }

    @GetMapping("/submissions/{submissionId}")
    public HomeworkSubmissionAdminDto submissionDetail(@PathVariable UUID submissionId) {
        return service.getSubmissionDetail(submissionId);
    }

    @PutMapping("/submissions/{submissionId}/feedback")
    public HomeworkSubmissionAdminDto saveFeedback(@PathVariable UUID submissionId,
                                                   @Valid @RequestBody SaveHomeworkFeedbackRequest request) {
        return service.saveFeedback(submissionId, request.feedback());
    }
}
