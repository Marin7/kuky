package com.kuky.backend.placement.service;

import com.kuky.backend.placement.dto.AdminQuestionDto;
import com.kuky.backend.placement.dto.LevelThresholdDto;
import com.kuky.backend.placement.dto.PlacementConfigDto;
import com.kuky.backend.placement.dto.SkillResultDto;
import com.kuky.backend.placement.dto.StudentEvaluationResponse;
import com.kuky.backend.placement.dto.UpsertQuestionRequest;
import com.kuky.backend.placement.dto.WritingSubmissionDto;
import com.kuky.backend.placement.exception.PlacementNotFoundException;
import com.kuky.backend.placement.model.CefrLevel;
import com.kuky.backend.placement.model.PlacementAttempt;
import com.kuky.backend.placement.model.PlacementAttemptSection;
import com.kuky.backend.placement.model.PlacementConfig;
import com.kuky.backend.placement.model.PlacementLevelThreshold;
import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.PlacementQuestionOption;
import com.kuky.backend.placement.model.QuestionKind;
import com.kuky.backend.placement.model.Skill;
import com.kuky.backend.placement.repository.PlacementAttemptRepository;
import com.kuky.backend.placement.repository.PlacementConfigRepository;
import com.kuky.backend.placement.repository.PlacementLevelThresholdRepository;
import com.kuky.backend.placement.repository.PlacementQuestionRepository;
import com.kuky.backend.placement.repository.PlacementWritingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Teacher-only authoring of placement questions/config/level thresholds and the per-student evaluation view (FR-014/FR-015). */
@Service
public class PlacementAdminService {

    private final PlacementConfigRepository configRepository;
    private final PlacementQuestionRepository questionRepository;
    private final PlacementAttemptRepository attemptRepository;
    private final PlacementWritingRepository writingRepository;
    private final PlacementLevelThresholdRepository levelThresholdRepository;

    public PlacementAdminService(PlacementConfigRepository configRepository,
                                  PlacementQuestionRepository questionRepository,
                                  PlacementAttemptRepository attemptRepository,
                                  PlacementWritingRepository writingRepository,
                                  PlacementLevelThresholdRepository levelThresholdRepository) {
        this.configRepository = configRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.writingRepository = writingRepository;
        this.levelThresholdRepository = levelThresholdRepository;
    }

    // --- config --------------------------------------------------------------

    public PlacementConfigDto getConfig() {
        return toDto(configRepository.get());
    }

    public PlacementConfigDto updateConfig(PlacementConfigDto request) {
        if (request.readingTimeSeconds() <= 0 || request.listeningTimeSeconds() <= 0
                || request.grammarTimeSeconds() <= 0 || request.writingTimeSeconds() <= 0) {
            throw new IllegalArgumentException("Los límites de tiempo deben ser mayores que cero.");
        }
        PlacementConfig config = configRepository.get();
        config.setReadingTimeSeconds(request.readingTimeSeconds());
        config.setListeningTimeSeconds(request.listeningTimeSeconds());
        config.setGrammarTimeSeconds(request.grammarTimeSeconds());
        config.setWritingTimeSeconds(request.writingTimeSeconds());
        config.setWritingPrompt(request.writingPrompt() == null ? "" : request.writingPrompt());
        return toDto(configRepository.update(config));
    }

    private PlacementConfigDto toDto(PlacementConfig c) {
        return new PlacementConfigDto(c.getReadingTimeSeconds(), c.getListeningTimeSeconds(),
                c.getGrammarTimeSeconds(), c.getWritingTimeSeconds(), c.getWritingPrompt());
    }

    // --- level thresholds (score % required per CEFR level) --------------------

    public List<LevelThresholdDto> getLevelThresholds() {
        return levelThresholdRepository.findAll().stream()
                .map(t -> new LevelThresholdDto(t.getLevel().name(), t.getMinScorePercent()))
                .toList();
    }

    public List<LevelThresholdDto> updateLevelThresholds(List<LevelThresholdDto> request) {
        if (request == null || request.size() != 6) {
            throw new IllegalArgumentException("Debes indicar un umbral para cada uno de los 6 niveles.");
        }
        List<PlacementLevelThreshold> parsed = request.stream().map(dto -> {
            if (dto.minScorePercent() < 0 || dto.minScorePercent() > 100) {
                throw new IllegalArgumentException("El umbral debe estar entre 0 y 100.");
            }
            PlacementLevelThreshold t = new PlacementLevelThreshold();
            t.setLevel(parseCefr(dto.level()));
            t.setMinScorePercent(dto.minScorePercent());
            return t;
        }).sorted(java.util.Comparator.comparingInt(t -> t.getLevel().ordinal())).toList();

        int previous = -1;
        for (PlacementLevelThreshold t : parsed) {
            if (t.getMinScorePercent() < previous) {
                throw new IllegalArgumentException(
                        "Los umbrales deben ser no decrecientes de A1 a C2 (cada nivel debe exigir al menos tanto como el anterior).");
            }
            previous = t.getMinScorePercent();
        }

        levelThresholdRepository.updateAll(parsed);
        return getLevelThresholds();
    }

    // --- questions -------------------------------------------------------------

    public List<AdminQuestionDto> listQuestions(Skill skill) {
        return questionRepository.findAllBySkill(skill).stream().map(this::toAdminDto).toList();
    }

    public AdminQuestionDto createQuestion(UpsertQuestionRequest request) {
        PlacementQuestion question = toModel(request, null);
        validate(question);
        return toAdminDto(questionRepository.create(question));
    }

    public AdminQuestionDto updateQuestion(UUID id, UpsertQuestionRequest request) {
        questionRepository.findById(id).orElseThrow(() -> new PlacementNotFoundException("Pregunta no encontrada."));
        PlacementQuestion question = toModel(request, id);
        validate(question);
        return toAdminDto(questionRepository.update(question));
    }

    public void deleteQuestion(UUID id) {
        questionRepository.findById(id).orElseThrow(() -> new PlacementNotFoundException("Pregunta no encontrada."));
        questionRepository.delete(id);
    }

    public void reorder(Skill skill, List<UUID> orderedIds) {
        questionRepository.reorder(skill, orderedIds == null ? List.of() : orderedIds);
    }

    private PlacementQuestion toModel(UpsertQuestionRequest request, UUID existingId) {
        PlacementQuestion q = new PlacementQuestion();
        q.setId(existingId);
        q.setSkill(parseSkill(request.skill()));
        q.setKind(parseKind(request.kind()));
        q.setPrompt(request.prompt());
        q.setAudioUrl(request.audioUrl());
        q.setAudioFileId(request.audioFileId());
        q.setActive(request.active() == null || request.active());
        q.setOptions(request.options() == null ? List.of() : request.options().stream().map(o -> {
            PlacementQuestionOption opt = new PlacementQuestionOption();
            opt.setLabel(o.label());
            opt.setCorrect(o.isCorrect());
            return opt;
        }).toList());
        return q;
    }

    private void validate(PlacementQuestion q) {
        if (q.getPrompt() == null || q.getPrompt().isBlank()) {
            throw new IllegalArgumentException("El enunciado no puede estar vacío.");
        }
        long correctCount = q.getOptions().stream().filter(PlacementQuestionOption::isCorrect).count();
        switch (q.getKind()) {
            case SINGLE_CHOICE -> {
                if (q.getOptions().size() < 2 || correctCount != 1) {
                    throw new IllegalArgumentException(
                            "Una pregunta de opción única necesita al menos 2 opciones y exactamente 1 correcta.");
                }
            }
            case MULTI_CHOICE -> {
                if (q.getOptions().size() < 2 || correctCount < 1) {
                    throw new IllegalArgumentException(
                            "Una pregunta de opción múltiple necesita al menos 2 opciones y al menos 1 correcta.");
                }
            }
            case FILL_BLANK -> {
                if (q.getOptions().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Una pregunta de espacio en blanco necesita al menos una respuesta aceptada.");
                }
            }
        }
        if (q.getSkill() == Skill.LISTENING && q.getAudioUrl() != null && !q.getAudioUrl().isBlank()
                && q.getAudioFileId() != null) {
            throw new IllegalArgumentException("Usa solo una fuente de audio: URL o archivo subido, no ambas.");
        }
    }

    private AdminQuestionDto toAdminDto(PlacementQuestion q) {
        List<AdminQuestionDto.AdminOptionDto> options = q.getOptions().stream()
                .map(o -> new AdminQuestionDto.AdminOptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new AdminQuestionDto(q.getId(), q.getSkill().name(), q.getPosition(), q.getKind().name(),
                q.getPrompt(), q.getAudioUrl(), q.getAudioFileId(), q.isActive(), options);
    }

    private Skill parseSkill(String value) {
        try {
            return Skill.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Destreza no válida.");
        }
    }

    private QuestionKind parseKind(String value) {
        try {
            return QuestionKind.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de pregunta no válido.");
        }
    }

    private CefrLevel parseCefr(String value) {
        try {
            return CefrLevel.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nivel CEFR no válido.");
        }
    }

    // --- student evaluation (FR-015) ------------------------------------------

    public StudentEvaluationResponse getStudentEvaluation(UUID studentId) {
        List<WritingSubmissionDto> writing = writingRepository.findByUser(studentId).stream()
                .map(s -> new WritingSubmissionDto(s.getId(), s.getBody(), s.getSubmittedAt()))
                .toList();

        StudentEvaluationResponse.ResultDto result = attemptRepository.findLatestCompleted(studentId)
                .map(this::toResultDto)
                .orElse(null);

        return new StudentEvaluationResponse(result, writing);
    }

    private StudentEvaluationResponse.ResultDto toResultDto(PlacementAttempt attempt) {
        List<PlacementAttemptSection> sections = attemptRepository.findSections(attempt.getId());
        List<SkillResultDto> skills = sections.stream()
                .filter(PlacementAttemptSection::isSubmitted)
                .map(s -> new SkillResultDto(s.getSkill().name(), s.getScorePercent(), s.getCefrLevel()))
                .toList();
        Instant completedAt = attempt.getCompletedAt();
        return new StudentEvaluationResponse.ResultDto(attempt.getOverallCefr(), completedAt, skills);
    }
}
