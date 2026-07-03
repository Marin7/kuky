package com.kuky.backend.admin.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;

public record SaveHomeworkFeedbackRequest(List<FormattedTextSegment> feedback) {}
