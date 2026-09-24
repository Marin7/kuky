package com.kuky.backend.learning;

import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.service.HomeworkAssignmentEmailService;
import com.kuky.backend.learning.service.NewHomeworkGrants;
import com.kuky.backend.preferences.model.EmailPreferenceType;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeworkAssignmentEmailServiceTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BEA = UUID.randomUUID();
    private static final UUID HW1 = UUID.randomUUID();
    private static final UUID HW2 = UUID.randomUUID();
    private static final UUID HW3 = UUID.randomUUID();

    @Mock private JavaMailSender mailSender;
    @Mock private EmailPreferencesRepository preferencesRepository;
    @Mock private ContentRepository contentRepository;

    private HomeworkAssignmentEmailService service;

    @BeforeEach
    void setUp() {
        service = new HomeworkAssignmentEmailService(
                mailSender, preferencesRepository, contentRepository);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@kuky.es");
        ReflectionTestUtils.setField(service, "baseUrl", "https://kuky.es");
        ReflectionTestUtils.setField(service, "mailEnabled", true);

        when(contentRepository.findTitlesByIds(anyCollection())).thenReturn(Map.of(
                HW1, "Los verbos reflexivos",
                HW2, "Comprensión lectora: Madrid",
                HW3, "Escucha: en el mercado"));
    }

    private void optedIn(Recipient... recipients) {
        when(preferencesRepository.findRecipientsFor(
                any(Collection.class), any(EmailPreferenceType.class)))
                .thenReturn(List.of(recipients));
    }

    private List<SimpleMailMessage> captureSent(int times) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(times)).send(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void severalHomeworkInOneAction_sendsExactlyOneEmailListingAllOfThem() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));
        grants.add(HW2, List.of(ANA));
        grants.add(HW3, List.of(ANA));

        service.notifyNewlyAssigned(grants);

        List<SimpleMailMessage> sent = captureSent(1);
        assertThat(sent).singleElement().satisfies(m -> {
            assertThat(m.getTo()).containsExactly("ana@example.com");
            assertThat(m.getText())
                    .contains("Los verbos reflexivos")
                    .contains("Comprensión lectora: Madrid")
                    .contains("Escucha: en el mercado");
        });
    }

    @Test
    void eachRecipientGetsTheirOwnMessage_withNoOtherStudentNamed() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"),
                new Recipient(BEA, "bea@example.com", "Bea"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA, BEA));

        service.notifyNewlyAssigned(grants);

        List<SimpleMailMessage> sent = captureSent(2);
        assertThat(sent).allSatisfy(m -> assertThat(m.getTo()).hasSize(1));
        assertThat(sent).extracting(m -> m.getTo()[0])
                .containsExactlyInAnyOrder("ana@example.com", "bea@example.com");
        // No message may leak the other student.
        assertThat(sent.get(0).getText()).doesNotContain("bea@example.com", "Bea");
        assertThat(sent.get(1).getText()).doesNotContain("ana@example.com", "Ana");
    }

    @Test
    void recipientsInOneActionMayReceiveDifferentContent() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"),
                new Recipient(BEA, "bea@example.com", "Bea"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA, BEA));
        grants.add(HW2, List.of(ANA));   // new to Ana only

        service.notifyNewlyAssigned(grants);

        List<SimpleMailMessage> sent = captureSent(2);
        SimpleMailMessage toAna = sent.stream()
                .filter(m -> "ana@example.com".equals(m.getTo()[0])).findFirst().orElseThrow();
        SimpleMailMessage toBea = sent.stream()
                .filter(m -> "bea@example.com".equals(m.getTo()[0])).findFirst().orElseThrow();

        assertThat(toAna.getText()).contains("Los verbos reflexivos", "Comprensión lectora: Madrid");
        assertThat(toBea.getText()).contains("Los verbos reflexivos")
                .doesNotContain("Comprensión lectora: Madrid");
    }

    @Test
    void optedOutStudent_receivesNothing() {
        optedIn(); // repository filters everyone out
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));

        service.notifyNewlyAssigned(grants);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void emptyGrants_touchesNothing() {
        service.notifyNewlyAssigned(new NewHomeworkGrants());

        verifyNoInteractions(mailSender, preferencesRepository, contentRepository);
    }

    @Test
    void nullGrants_touchesNothing() {
        service.notifyNewlyAssigned(null);

        verifyNoInteractions(mailSender, preferencesRepository, contentRepository);
    }

    @Test
    void mailDisabled_skipsSendingEntirely() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));

        service.notifyNewlyAssigned(grants);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void emailLinksToBothTheWorkAndThePreferences() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));

        service.notifyNewlyAssigned(grants);

        SimpleMailMessage message = captureSent(1).getFirst();
        assertThat(message.getFrom()).isEqualTo("noreply@kuky.es");
        assertThat(message.getSubject()).isNotBlank();
        assertThat(message.getText())
                .contains("https://kuky.es/aprendizaje")
                .contains("https://kuky.es/cuenta")
                .contains("Recibes este correo porque");
    }

    @Test
    void oneRecipientFailing_neitherPropagatesNorStopsTheOthers() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"),
                new Recipient(BEA, "bea@example.com", "Bea"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA, BEA));

        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(org.mockito.ArgumentMatchers
                        .argThat((SimpleMailMessage m) ->
                                m != null && m.getTo() != null && "ana@example.com".equals(m.getTo()[0])));

        assertThatCode(() -> service.notifyNewlyAssigned(grants)).doesNotThrowAnyException();

        // Bea is still attempted despite Ana's failure.
        verify(mailSender, org.mockito.Mockito.times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void recipientLookupFailing_isSwallowed() {
        when(preferencesRepository.findRecipientsFor(any(Collection.class), any(EmailPreferenceType.class)))
                .thenThrow(new RuntimeException("db down"));
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));

        assertThatCode(() -> service.notifyNewlyAssigned(grants)).doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void unknownHomeworkTitle_producesNoEmptyEmail() {
        optedIn(new Recipient(ANA, "ana@example.com", "Ana"));
        when(contentRepository.findTitlesByIds(anyCollection())).thenReturn(Map.of());
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));

        service.notifyNewlyAssigned(grants);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void grantsAccumulator_deduplicatesTheSameHomeworkForTheSameStudent() {
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of(ANA));
        grants.add(HW1, List.of(ANA));

        assertThat(grants.byStudent().get(ANA)).containsExactly(HW1);
    }

    @Test
    void grantsAccumulator_ignoresEmptyAndNullInput() {
        NewHomeworkGrants grants = new NewHomeworkGrants();
        grants.add(HW1, List.of());
        grants.add(HW1, null);
        grants.add(null, List.of(ANA));

        assertThat(grants.isEmpty()).isTrue();
    }
}
