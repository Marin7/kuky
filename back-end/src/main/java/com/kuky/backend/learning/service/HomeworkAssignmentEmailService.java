package com.kuky.backend.learning.service;

import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.preferences.model.EmailPreferenceType;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tells students, by email, that new homework is waiting — but only those who asked to be told.
 *
 * <p>One message per student per teacher action, however many homework that action granted
 * them, and never more than one recipient per message. Sending is best-effort: a failure is
 * logged and swallowed so it can never fail or roll back the teacher's assignment, matching
 * {@code BookingEmailService} and {@code TestimonialEmailService}.
 */
@Service
public class HomeworkAssignmentEmailService {

    private static final Logger log = LoggerFactory.getLogger(HomeworkAssignmentEmailService.class);

    private final JavaMailSender mailSender;
    private final EmailPreferencesRepository preferencesRepository;
    private final ContentRepository contentRepository;

    @Value("${app.frontend.base-url}")
    private String baseUrl;

    @Value("${app.mail.from:noreply@kuky.es}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public HomeworkAssignmentEmailService(JavaMailSender mailSender,
                                          EmailPreferencesRepository preferencesRepository,
                                          ContentRepository contentRepository) {
        this.mailSender = mailSender;
        this.preferencesRepository = preferencesRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * Emails each student in {@code grants} who has opted in, once, listing everything the
     * action made newly available to them. Safe to call with nothing to report.
     *
     * <p>Callers are class-level {@code @Transactional}, so the work is deferred until after
     * that transaction commits: a rollback must not leave students told about homework that
     * no longer exists. Outside a transaction (unit tests) it runs immediately.
     */
    public void notifyNewlyAssigned(NewHomeworkGrants grants) {
        if (grants == null || grants.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(grants);
                }
            });
            return;
        }
        send(grants);
    }

    private void send(NewHomeworkGrants grants) {
        List<EmailPreferencesRepository.Recipient> recipients;
        Map<UUID, String> titles;
        try {
            recipients = preferencesRepository.findRecipientsFor(
                    grants.byStudent().keySet(), EmailPreferenceType.NEW_HOMEWORK_ASSIGNED);
            if (recipients.isEmpty()) {
                return;
            }
            titles = contentRepository.findTitlesByIds(
                    grants.byStudent().values().stream().flatMap(List::stream).toList());
        } catch (Exception e) {
            // Never let notification work break the assignment that triggered it.
            log.warn("HomeworkAssignmentEmailService — could not resolve recipients: {}", e.getMessage());
            return;
        }

        for (EmailPreferencesRepository.Recipient recipient : recipients) {
            List<UUID> assignmentIds = grants.byStudent().get(recipient.userId());
            if (assignmentIds == null || assignmentIds.isEmpty()) {
                continue;
            }
            List<String> newTitles = assignmentIds.stream()
                    .map(titles::get)
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
            if (newTitles.isEmpty()) {
                continue;
            }
            sendQuietly(recipient, newTitles);
        }
    }

    private void sendQuietly(EmailPreferencesRepository.Recipient recipient, List<String> titles) {
        if (!mailEnabled) {
            log.debug("HomeworkAssignmentEmailService — mail disabled, skipping send to {}",
                    recipient.email());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.email());
            message.setSubject(subject(titles.size()));
            message.setText(body(recipient.firstName(), titles));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("HomeworkAssignmentEmailService — failed to send to {}: {}",
                    recipient.email(), e.getMessage());
        }
    }

    private String subject(int count) {
        return count == 1
                ? "Tienes una tarea nueva — Destino: Español"
                : "Tienes " + count + " tareas nuevas — Destino: Español";
    }

    private String body(String firstName, List<String> titles) {
        String greeting = firstName == null || firstName.isBlank()
                ? "¡Hola!"
                : "¡Hola, " + firstName + "!";
        String intro = titles.size() == 1
                ? "Paula te ha asignado una tarea nueva:"
                : "Paula te ha asignado " + titles.size() + " tareas nuevas:";

        StringBuilder list = new StringBuilder();
        for (String title : titles) {
            list.append("  · ").append(title).append("\n");
        }

        return greeting + "\n\n"
                + intro + "\n\n"
                + list + "\n"
                + "Puedes verlas en Mi aprendizaje:\n\n"
                + baseUrl + "/aprendizaje\n\n"
                + "Recibes este correo porque pediste que te avisáramos cuando Paula te asignara "
                + "una tarea nueva. Puedes cambiarlo o dejar de recibirlo cuando quieras desde tu cuenta:\n\n"
                + baseUrl + "/cuenta\n\n"
                + "Saludos,\nDestino: Español";
    }
}
