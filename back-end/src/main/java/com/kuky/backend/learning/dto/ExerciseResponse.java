package com.kuky.backend.learning.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Exercise / mixed homework take page as seen by a student.
 * When graded (or mixed submitted with auto results), {@code result} is populated.
 */
public record ExerciseResponse(
        UUID id,
        String title,
        String instructions,
        String format,                       // EXERCISE | MIXED
        String composition,                  // ALL_AUTO | MIXED
        String status,                       // PENDING | SUBMITTED | GRADED
        String homeworkType,                 // AUDIO | READ | … (nullable)
        String audioUrl,                     // listening homework external source (nullable)
        UUID audioFileId,                    // listening homework uploaded file (nullable)
        String mediaSourceKind,              // AUDIO_URL | UPLOADED_FILE | VIDEO_PAGE | YOUTUBE | null
        List<ExerciseQuestionDto> questions,
        ExerciseResultResponse result,       // null unless submitted/graded with auto results
        List<ManualAnswerViewDto> answers,   // FREE_TEXT answers for MIXED; else empty
        Integer scorePercent,                // final combined when GRADED
        Integer provisionalScorePercent,     // auto-only while MIXED SUBMITTED
        String feedbackText,                 // ANNOTATED plain note when present
        String teacherFeedback,              // plain teacher comment; null unless present
        Instant contentRevisedAt,            // present while PENDING; omitted after submit
        LocalDate dueOn                      // this student's due date; null if none
) {}
