package com.kuky.backend.learning.service;

import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HomeworkItemsStudentStripTest {

    @Test
    void submittedStripsTeacherPercentAndManualScore() {
        HomeworkAssignment assignment = assignment();
        HomeworkQuestion q = freeTextQuestion();
        HomeworkAnswer answer = freeTextAnswer(q.getId(), 70);
        HomeworkSubmission submission = submitted();

        HomeworkItemResponse response = HomeworkItems.toResponse(
                assignment, submission, LocalDate.now(), null, null,
                List.of(q), List.of(answer), List.of(), null);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.scorePercent()).isNull();
        assertThat(response.answers()).hasSize(1);
        ManualAnswerViewDto view = response.answers().get(0);
        assertThat(view.teacherScorePercent()).isNull();
        assertThat(view.score()).isNull();
        assertThat(view.text()).isEqualTo("respuesta");
    }

    @Test
    void gradedExposesTeacherPercentAndScore() {
        HomeworkAssignment assignment = assignment();
        HomeworkQuestion q = freeTextQuestion();
        HomeworkAnswer answer = freeTextAnswer(q.getId(), 70);
        HomeworkSubmission submission = graded(70);

        HomeworkItemResponse response = HomeworkItems.toResponse(
                assignment, submission, LocalDate.now(), null, null,
                List.of(q), List.of(answer), List.of(), null);

        assertThat(response.status()).isEqualTo("GRADED");
        assertThat(response.scorePercent()).isEqualTo(70);
        ManualAnswerViewDto view = response.answers().get(0);
        assertThat(view.teacherScorePercent()).isEqualTo(70);
        assertThat(view.score()).isEqualTo(0.7);
    }

    private static HomeworkAssignment assignment() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(UUID.randomUUID());
        a.setTitle("Manual");
        a.setFormat(HomeworkFormat.MANUAL);
        a.setHomeworkType(HomeworkType.AUDIO);
        return a;
    }

    private static HomeworkQuestion freeTextQuestion() {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setKind(QuestionKind.FREE_TEXT);
        q.setPrompt("prompt");
        return q;
    }

    private static HomeworkAnswer freeTextAnswer(UUID questionId, int percent) {
        HomeworkAnswer a = new HomeworkAnswer();
        a.setId(UUID.randomUUID());
        a.setQuestionId(questionId);
        a.setPromptSnapshot("prompt");
        a.setAnswerText("respuesta");
        a.setTeacherScorePercent(percent);
        a.setScore(BigDecimal.valueOf(percent / 100.0).setScale(3, java.math.RoundingMode.HALF_UP));
        return a;
    }

    private static HomeworkSubmission submitted() {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setStatus("SUBMITTED");
        s.setReviewModel("ANNOTATED");
        return s;
    }

    private static HomeworkSubmission graded(int scorePercent) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setStatus("GRADED");
        s.setReviewModel("ANNOTATED");
        s.setScorePercent(scorePercent);
        return s;
    }
}
