package com.kuky.backend.quiz.controller;

import com.kuky.backend.quiz.dto.CreateQuizRequest;
import com.kuky.backend.quiz.dto.QuizAdminDetail;
import com.kuky.backend.quiz.dto.QuizAdminListItem;
import com.kuky.backend.quiz.dto.QuizAttemptListItem;
import com.kuky.backend.quiz.dto.QuizReviewRequest;
import com.kuky.backend.quiz.dto.QuizTakeResponse;
import com.kuky.backend.quiz.dto.SetQuizAssigneesRequest;
import com.kuky.backend.quiz.dto.StudentQuizSummary;
import com.kuky.backend.quiz.dto.UpdateQuizRequest;
import com.kuky.backend.quiz.service.QuizAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class QuizAdminController {

    private final QuizAdminService service;

    public QuizAdminController(QuizAdminService service) {
        this.service = service;
    }

    @GetMapping("/quizzes")
    public List<QuizAdminListItem> list() {
        return service.list();
    }

    @PostMapping("/quizzes")
    public ResponseEntity<QuizAdminDetail> create(@Valid @RequestBody CreateQuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/quizzes/{id}")
    public QuizAdminDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/quizzes/{id}")
    public QuizAdminDetail update(@PathVariable UUID id, @Valid @RequestBody UpdateQuizRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/quizzes/{id}/assignees")
    public QuizAdminDetail setAssignees(@PathVariable UUID id,
                                        @Valid @RequestBody SetQuizAssigneesRequest request) {
        return service.setAssignees(id, request.studentIds());
    }

    @GetMapping("/quizzes/{id}/attempts")
    public List<QuizAttemptListItem> attempts(@PathVariable UUID id) {
        return service.listAttempts(id);
    }

    @GetMapping("/quizzes/{id}/attempts/{attemptId}")
    public QuizTakeResponse attempt(@PathVariable UUID id, @PathVariable UUID attemptId) {
        return service.getAttempt(id, attemptId);
    }

    @PutMapping("/quizzes/{id}/attempts/{attemptId}/review")
    public QuizTakeResponse review(@PathVariable UUID id,
                                   @PathVariable UUID attemptId,
                                   @RequestBody QuizReviewRequest request) {
        return service.review(id, attemptId, request);
    }

    @GetMapping("/students/{studentId}/quizzes")
    public List<StudentQuizSummary> studentQuizzes(@PathVariable UUID studentId) {
        return service.listForStudent(studentId);
    }
}
