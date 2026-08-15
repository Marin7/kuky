package com.kuky.backend.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class QuizSnapshot {

    private final ObjectMapper objectMapper;

    public QuizSnapshot(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Quiz quiz, List<QuizQuestion> questions) {
        try {
            return objectMapper.writeValueAsString(new Payload(
                    quiz.getTitle(),
                    quiz.getDescription(),
                    questions.stream().map(this::toSnap).toList()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar la instantánea de la prueba de evaluación.", e);
        }
    }

    public List<QuizQuestion> questionsOf(String json) {
        Payload payload = parse(json);
        List<QuizQuestion> out = new ArrayList<>();
        int i = 0;
        for (SnapQuestion s : payload.questions()) {
            QuizQuestion q = new QuizQuestion();
            q.setId(s.id());
            q.setPosition(s.position() >= 0 ? s.position() : i);
            q.setSkill(QuizSkill.valueOf(s.skill()));
            q.setKind(QuestionKind.valueOf(s.kind()));
            q.setPrompt(s.prompt());
            q.setStructureJson(s.structure() == null || s.structure().isNull()
                    ? "{}" : s.structure().toString());
            q.setMediaSourceKind(s.mediaSourceKind() == null ? null : MediaSourceKind.valueOf(s.mediaSourceKind()));
            q.setAudioUrl(s.audioUrl());
            q.setAudioFileId(s.audioFileId());
            List<QuestionOption> options = new ArrayList<>();
            if (s.options() != null) {
                int p = 0;
                for (SnapOption o : s.options()) {
                    QuestionOption opt = new QuestionOption();
                    opt.setId(o.id());
                    opt.setQuestionId(s.id());
                    opt.setPosition(o.position() >= 0 ? o.position() : p);
                    opt.setLabel(o.label());
                    opt.setCorrect(o.correct());
                    options.add(opt);
                    p++;
                }
            }
            q.setOptions(options);
            out.add(q);
            i++;
        }
        return out;
    }

    public Payload parse(String json) {
        try {
            Payload payload = objectMapper.readValue(json, Payload.class);
            return payload == null || payload.questions() == null
                    ? new Payload(null, null, List.of())
                    : payload;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer la instantánea de la prueba de evaluación.", e);
        }
    }

    public static HomeworkQuestion toHomeworkQuestion(QuizQuestion q) {
        HomeworkQuestion h = new HomeworkQuestion();
        h.setId(q.getId());
        h.setKind(q.getKind());
        h.setPrompt(q.getPrompt());
        h.setStructureJson(q.getStructureJson());
        h.setOptions(q.getOptions());
        h.setPosition(q.getPosition());
        return h;
    }

    private SnapQuestion toSnap(QuizQuestion q) {
        JsonNode structure;
        try {
            structure = objectMapper.readTree(q.getStructureJson() == null ? "{}" : q.getStructureJson());
        } catch (JsonProcessingException e) {
            structure = objectMapper.createObjectNode();
        }
        List<SnapOption> options = q.getOptions() == null ? List.of() : q.getOptions().stream()
                .map(o -> new SnapOption(o.getId(), o.getPosition(), o.getLabel(), o.isCorrect()))
                .toList();
        return new SnapQuestion(
                q.getId(),
                q.getPosition(),
                q.getSkill().name(),
                q.getKind().name(),
                q.getPrompt(),
                structure,
                q.getMediaSourceKind() == null ? null : q.getMediaSourceKind().name(),
                q.getAudioUrl(),
                q.getAudioFileId(),
                options);
    }

    public record Payload(String title, String description, List<SnapQuestion> questions) {}

    public record SnapQuestion(
            UUID id,
            int position,
            String skill,
            String kind,
            String prompt,
            JsonNode structure,
            String mediaSourceKind,
            String audioUrl,
            UUID audioFileId,
            List<SnapOption> options
    ) {}

    public record SnapOption(UUID id, int position, String label, boolean correct) {}
}
