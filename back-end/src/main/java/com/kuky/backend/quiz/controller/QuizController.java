package com.kuky.backend.quiz.controller;

import com.kuky.backend.quiz.dto.QuizListResponse;
import com.kuky.backend.quiz.dto.QuizTakeResponse;
import com.kuky.backend.quiz.dto.SubmitQuizRequest;
import com.kuky.backend.quiz.service.QuizService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @GetMapping
    public QuizListResponse list(@AuthenticationPrincipal String email) {
        return service.listMine(email);
    }

    @GetMapping("/{quizId}")
    public QuizTakeResponse get(@AuthenticationPrincipal String email, @PathVariable UUID quizId) {
        return service.getOrStart(email, quizId);
    }

    @PutMapping("/{quizId}/answers")
    public QuizTakeResponse submit(@AuthenticationPrincipal String email,
                                   @PathVariable UUID quizId,
                                   @RequestBody SubmitQuizRequest request) {
        return service.submit(email, quizId, request);
    }
}
