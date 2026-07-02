package com.kuky.backend.testimonials.exception;

public class TestimonialNotFoundException extends RuntimeException {
    public TestimonialNotFoundException(String message) {
        super(message);
    }
}
