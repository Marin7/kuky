package com.kuky.backend.testimonials.dto;

import com.kuky.backend.testimonials.model.TestimonialStatus;

import java.time.Instant;

public record MyTestimonialResponse(String text, TestimonialStatus status, Instant submittedAt) {}
