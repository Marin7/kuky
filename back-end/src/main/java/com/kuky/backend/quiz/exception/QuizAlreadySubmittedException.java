package com.kuky.backend.quiz.exception;

public class QuizAlreadySubmittedException extends RuntimeException {
    public QuizAlreadySubmittedException(String message) {
        super(message);
    }
}
