package com.kuky.backend.placement.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.placement.dto.AttemptResultResponse;
import com.kuky.backend.placement.dto.FullEvaluationResponse;
import com.kuky.backend.placement.dto.PlacementTestResponse;
import com.kuky.backend.placement.dto.SectionDto;
import com.kuky.backend.placement.dto.SectionResultResponse;
import com.kuky.backend.placement.dto.SkillResultDto;
import com.kuky.backend.placement.dto.StartSectionResponse;
import com.kuky.backend.placement.dto.StudentOptionDto;
import com.kuky.backend.placement.dto.StudentQuestionDto;
import com.kuky.backend.placement.dto.SubmitSectionRequest;
import com.kuky.backend.placement.dto.WritingSectionDto;
import com.kuky.backend.placement.dto.WritingStartResponse;
import com.kuky.backend.placement.dto.WritingSubmissionDto;
import com.kuky.backend.placement.exception.PlacementNotFoundException;
import com.kuky.backend.placement.exception.SectionAlreadySubmittedException;
import com.kuky.backend.placement.exception.SectionNotStartedException;
import com.kuky.backend.placement.model.PlacementAttempt;
import com.kuky.backend.placement.model.PlacementAttemptSection;
import com.kuky.backend.placement.model.PlacementConfig;
import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.PlacementWritingAttempt;
import com.kuky.backend.placement.model.PlacementWritingSubmission;
import com.kuky.backend.placement.model.Skill;
import com.kuky.backend.placement.repository.PlacementAnswerRepository;
import com.kuky.backend.placement.repository.PlacementAttemptRepository;
import com.kuky.backend.placement.repository.PlacementConfigRepository;
import com.kuky.backend.placement.repository.PlacementLevelThresholdRepository;
import com.kuky.backend.placement.repository.PlacementQuestionRepository;
import com.kuky.backend.placement.repository.PlacementWritingAttemptRepository;
import com.kuky.backend.placement.repository.PlacementWritingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the student-facing placement flow: the test view (answer key
 * hidden), starting/submitting timed sections, the combined result, and the
 * full-evaluation panel (static payment text + Writing submission). Section
 * deadlines are computed and stored server-side at start time and are the sole
 * authority for expiry — see FR-006 and research.md §3.
 */
@Service
public class PlacementService {

    private final PlacementConfigRepository configRepository;
    private final PlacementQuestionRepository questionRepository;
    private final PlacementAttemptRepository attemptRepository;
    private final PlacementAnswerRepository answerRepository;
    private final PlacementWritingRepository writingRepository;
    private final PlacementWritingAttemptRepository writingAttemptRepository;
    private final PlacementLevelThresholdRepository levelThresholdRepository;
    private final PlacementGradingService gradingService;
    private final PlacementScoringService scoringService;
    private final UserRepository userRepository;
    private final Clock clock;

    public PlacementService(PlacementConfigRepository configRepository,
                             PlacementQuestionRepository questionRepository,
                             PlacementAttemptRepository attemptRepository,
                             PlacementAnswerRepository answerRepository,
                             PlacementWritingRepository writingRepository,
                             PlacementWritingAttemptRepository writingAttemptRepository,
                             PlacementLevelThresholdRepository levelThresholdRepository,
                             PlacementGradingService gradingService,
                             PlacementScoringService scoringService,
                             UserRepository userRepository,
                             Clock clock) {
        this.configRepository = configRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.writingRepository = writingRepository;
        this.writingAttemptRepository = writingAttemptRepository;
        this.levelThresholdRepository = levelThresholdRepository;
        this.gradingService = gradingService;
        this.scoringService = scoringService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // --- Test view + attempt lifecycle --------------------------------------

    public PlacementTestResponse getTest(String email) {
        User user = requireUser(email);
        PlacementConfig config = configRepository.get();
        Optional<PlacementAttempt> attempt = attemptRepository.findInProgress(user.getId());

        List<SectionDto> sections = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            List<PlacementQuestion> questions = questionRepository.findActiveBySkill(skill);
            Optional<PlacementAttemptSection> section = attempt.flatMap(
                    a -> attemptRepository.findSection(a.getId(), skill));

            String status = section.isEmpty() ? "NOT_STARTED"
                    : section.get().isSubmitted() ? "SUBMITTED" : "IN_PROGRESS";
            Instant deadline = section.map(PlacementAttemptSection::getDeadlineAt).orElse(null);

            sections.add(new SectionDto(
                    skill.name(), config.timeLimitSecondsFor(skill), status, deadline,
                    buildStudentQuestions(questions)));
        }

        return new PlacementTestResponse(attempt.map(PlacementAttempt::getId).orElse(null), sections);
    }

    public Map<String, Object> startAttempt(String email) {
        User user = requireUser(email);
        PlacementAttempt attempt = attemptRepository.findInProgress(user.getId())
                .orElseGet(() -> attemptRepository.create(user.getId(), Instant.now(clock)));
        return Map.of("attemptId", attempt.getId(), "status", attempt.getStatus().name());
    }

    public StartSectionResponse startSection(String email, UUID attemptId, Skill skill) {
        PlacementAttempt attempt = requireOwnedAttempt(email, attemptId);
        Optional<PlacementAttemptSection> existing = attemptRepository.findSection(attemptId, skill);
        if (existing.isPresent()) {
            if (existing.get().isSubmitted()) {
                throw new SectionAlreadySubmittedException("Esta sección ya ha sido entregada.");
            }
            return new StartSectionResponse(skill.name(), existing.get().getDeadlineAt(), Instant.now(clock));
        }

        PlacementConfig config = configRepository.get();
        Instant now = Instant.now(clock);
        Instant deadline = now.plusSeconds(config.timeLimitSecondsFor(skill));
        try {
            PlacementAttemptSection section = attemptRepository.startSection(attempt.getId(), skill, now, deadline);
            return new StartSectionResponse(skill.name(), section.getDeadlineAt(), now);
        } catch (DuplicateKeyException e) {
            // A concurrent request (e.g. a duplicate client call) won the race and already
            // inserted this (attempt, skill) row — fall back to it instead of failing.
            PlacementAttemptSection existingSection = attemptRepository.findSection(attemptId, skill)
                    .orElseThrow(() -> e);
            return new StartSectionResponse(skill.name(), existingSection.getDeadlineAt(), now);
        }
    }

    @Transactional
    public SectionResultResponse submitSection(String email, UUID attemptId, Skill skill, SubmitSectionRequest request) {
        PlacementAttempt attempt = requireOwnedAttempt(email, attemptId);
        PlacementAttemptSection section = attemptRepository.findSection(attemptId, skill)
                .orElseThrow(() -> new SectionNotStartedException("Esta sección no ha sido iniciada."));
        if (section.isSubmitted()) {
            throw new SectionAlreadySubmittedException("Esta sección ya ha sido entregada.");
        }

        List<PlacementQuestion> questions = questionRepository.findActiveBySkill(skill);
        PlacementGradingService.GradedSection graded = gradingService.grade(questions, request);
        answerRepository.saveAll(section.getId(), graded.answers());

        String cefrLevel = scoringService.bandSkill(graded.scorePercent(), levelThresholdRepository.findAll());

        Instant now = Instant.now(clock);
        attemptRepository.submitSection(section.getId(), graded.scorePercent(), cefrLevel, now);

        maybeCompleteAttempt(attempt.getId());

        return new SectionResultResponse(skill.name(), graded.scorePercent(), cefrLevel, graded.questionResults());
    }

    private void maybeCompleteAttempt(UUID attemptId) {
        List<PlacementAttemptSection> sections = attemptRepository.findSections(attemptId);
        if (sections.size() < Skill.values().length || sections.stream().anyMatch(s -> !s.isSubmitted())) {
            return;
        }
        List<String> levels = sections.stream().map(PlacementAttemptSection::getCefrLevel).toList();
        String overall = scoringService.overall(levels);
        attemptRepository.complete(attemptId, overall, Instant.now(clock));
    }

    public AttemptResultResponse getResult(String email, UUID attemptId) {
        PlacementAttempt attempt = requireOwnedAttempt(email, attemptId);
        List<PlacementAttemptSection> sections = attemptRepository.findSections(attemptId);

        List<SkillResultDto> skills = sections.stream()
                .filter(PlacementAttemptSection::isSubmitted)
                .map(s -> new SkillResultDto(s.getSkill().name(), s.getScorePercent(), s.getCefrLevel()))
                .toList();

        return new AttemptResultResponse(attempt.getStatus().name(), attempt.getOverallCefr(), skills);
    }

    // --- Full evaluation (offline payment, Writing) -------------------------

    public FullEvaluationResponse getFullEvaluation(String email) {
        User user = requireUser(email);
        PlacementConfig config = configRepository.get();
        WritingSubmissionDto mine = writingRepository.findLatestByUser(user.getId())
                .map(s -> new WritingSubmissionDto(s.getId(), s.getBody(), s.getSubmittedAt()))
                .orElse(null);

        Optional<PlacementWritingAttempt> inProgress = writingAttemptRepository.findInProgress(user.getId());
        WritingSectionDto writingSection = new WritingSectionDto(
                inProgress.isPresent() ? "IN_PROGRESS" : "NOT_STARTED",
                config.getWritingTimeSeconds(),
                inProgress.map(PlacementWritingAttempt::getDeadlineAt).orElse(null));

        return new FullEvaluationResponse(config.getWritingPrompt(), mine, writingSection);
    }

    /** Find-or-create the caller's in-progress Writing attempt, server-timed like the auto-graded sections. */
    public WritingStartResponse startWriting(String email) {
        User user = requireUser(email);
        Optional<PlacementWritingAttempt> existing = writingAttemptRepository.findInProgress(user.getId());
        if (existing.isPresent()) {
            return new WritingStartResponse(existing.get().getDeadlineAt(), Instant.now(clock));
        }

        PlacementConfig config = configRepository.get();
        Instant now = Instant.now(clock);
        Instant deadline = now.plusSeconds(config.getWritingTimeSeconds());
        try {
            PlacementWritingAttempt attempt = writingAttemptRepository.create(user.getId(), now, deadline);
            return new WritingStartResponse(attempt.getDeadlineAt(), now);
        } catch (DuplicateKeyException e) {
            // A concurrent request already won the race and started the attempt — fall back to it.
            PlacementWritingAttempt existingAttempt = writingAttemptRepository.findInProgress(user.getId())
                    .orElseThrow(() -> e);
            return new WritingStartResponse(existingAttempt.getDeadlineAt(), now);
        }
    }

    /**
     * Trust-based: any logged-in user may submit, no payment check (FR-010). Requires an
     * in-progress Writing attempt (started via {@link #startWriting}); a submit received
     * after the deadline is still accepted and stores exactly what was sent, mirroring the
     * auto-graded sections' server-authoritative deadline handling (FR-006).
     */
    public WritingSubmissionDto submitWriting(String email, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("La redacción no puede estar vacía.");
        }
        User user = requireUser(email);
        PlacementWritingAttempt attempt = writingAttemptRepository.findInProgress(user.getId())
                .orElseThrow(() -> new SectionNotStartedException("La redacción no ha sido iniciada."));

        PlacementConfig config = configRepository.get();
        Instant now = Instant.now(clock);

        PlacementWritingSubmission submission = new PlacementWritingSubmission();
        submission.setUserId(user.getId());
        submission.setWritingAttemptId(attempt.getId());
        submission.setBody(body);
        submission.setPromptSnapshot(config.getWritingPrompt());
        submission.setSubmittedAt(now);

        PlacementWritingSubmission saved = writingRepository.insert(submission);
        writingAttemptRepository.markSubmitted(attempt.getId(), now);
        return new WritingSubmissionDto(saved.getId(), saved.getBody(), saved.getSubmittedAt());
    }

    // --- helpers -------------------------------------------------------------

    private List<StudentQuestionDto> buildStudentQuestions(List<PlacementQuestion> questions) {
        return questions.stream().map(q -> {
            List<StudentOptionDto> options = q.getKind().name().equals("FILL_BLANK")
                    ? List.of()
                    : q.getOptions().stream()
                        .map(o -> new StudentOptionDto(o.getId(), o.getLabel()))
                        .toList();
            return new StudentQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(),
                    q.getAudioUrl(), q.getAudioFileId(), options);
        }).toList();
    }

    private PlacementAttempt requireOwnedAttempt(String email, UUID attemptId) {
        User user = requireUser(email);
        PlacementAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new PlacementNotFoundException("Intento no encontrado."));
        if (!attempt.getUserId().equals(user.getId())) {
            throw new PlacementNotFoundException("Intento no encontrado.");
        }
        return attempt;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }
}
