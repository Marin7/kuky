package com.kuky.backend.quiz.exception;

public class QuizNotAssignedException extends RuntimeException {
    public QuizNotAssignedException(String message) {
        super(message);
    }
}
