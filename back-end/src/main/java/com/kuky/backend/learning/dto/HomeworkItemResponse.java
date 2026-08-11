package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HomeworkItemResponse(
        UUID id,
        String title,
        String instructions,
        LocalDate dueOn,
        String homeworkType,
        String level,
        String format,            // MANUAL | EXERCISE | MIXED
        String composition,       // WRITE | ALL_MANUAL | ALL_AUTO | MIXED
        String status,
        String reviewModel,
        List<FormattedTextSegment> response,
        List<FormattedTextSegment> feedback,   // teacher's formatted feedback, present once REVIEWED
        String feedbackText,
        Integer scorePercent,     // present when status == GRADED (final combined for MIXED)
        Integer provisionalScorePercent, // auto-only mean while MIXED awaits teacher
        Instant submittedAt,
        boolean overdue,
        String audioUrl,          // listening homework external source (nullable)
        UUID audioFileId,         // listening homework uploaded file (nullable)
        String mediaSourceKind,   // AUDIO_URL | UPLOADED_FILE | VIDEO_PAGE | YOUTUBE | null
        UnitRef unit,             // owning unit for grouping (nullable for legacy/unattached)
        Integer unitPosition,     // rank within unit mixed sequence (nullable when unattached)
        boolean hasTeacherFeedback,
        List<ExerciseQuestionDto> questions,
        List<ManualAnswerViewDto> answers,
        ExerciseResultResponse result  // auto / mixed auto-subset results after submit
) {}
