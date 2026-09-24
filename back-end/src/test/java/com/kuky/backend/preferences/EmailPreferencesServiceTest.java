package com.kuky.backend.preferences;

import com.kuky.backend.preferences.dto.EmailPreferenceResponse;
import com.kuky.backend.preferences.model.EmailPreferenceType;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository;
import com.kuky.backend.preferences.service.EmailPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPreferencesServiceTest {

    private static final String EMAIL = "alumno@example.com";

    @Mock private EmailPreferencesRepository repository;

    private EmailPreferencesService service;

    @BeforeEach
    void setUp() {
        service = new EmailPreferencesService(repository);
    }

    private void stored(boolean newHomeworkAssigned) {
        Map<EmailPreferenceType, Boolean> map = new EnumMap<>(EmailPreferenceType.class);
        map.put(EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, newHomeworkAssigned);
        when(repository.findByEmail(EMAIL)).thenReturn(map);
    }

    @Test
    void list_untouchedAccount_readsEveryOptionAsOff() {
        stored(false);

        List<EmailPreferenceResponse> prefs = service.list(EMAIL);

        assertThat(prefs).allSatisfy(p -> assertThat(p.enabled()).isFalse());
    }

    @Test
    void list_returnsEverySupportedOption_inDeclarationOrder() {
        stored(false);

        List<EmailPreferenceResponse> prefs = service.list(EMAIL);

        assertThat(prefs).extracting(EmailPreferenceResponse::type)
                .containsExactly(EmailPreferenceType.values());
    }

    @Test
    void list_reflectsStoredValue_whenOptedIn() {
        stored(true);

        List<EmailPreferenceResponse> prefs = service.list(EMAIL);

        assertThat(prefs).filteredOn(p -> p.type() == EmailPreferenceType.NEW_HOMEWORK_ASSIGNED)
                .singleElement()
                .satisfies(p -> assertThat(p.enabled()).isTrue());
    }

    @Test
    void list_missingUserRow_readsAsOff() {
        when(repository.findByEmail(EMAIL)).thenReturn(new EnumMap<>(EmailPreferenceType.class));

        assertThat(service.list(EMAIL)).allSatisfy(p -> assertThat(p.enabled()).isFalse());
    }

    @Test
    void set_writesThroughAndEchoesNewValue() {
        EmailPreferenceResponse result =
                service.set(EMAIL, "NEW_HOMEWORK_ASSIGNED", true);

        verify(repository).update(EMAIL, EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, true);
        assertThat(result.type()).isEqualTo(EmailPreferenceType.NEW_HOMEWORK_ASSIGNED);
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void set_isIdempotent_onOffOnRoundTrip() {
        service.set(EMAIL, "NEW_HOMEWORK_ASSIGNED", true);
        service.set(EMAIL, "NEW_HOMEWORK_ASSIGNED", false);
        service.set(EMAIL, "NEW_HOMEWORK_ASSIGNED", true);

        verify(repository, org.mockito.Mockito.times(2))
                .update(EMAIL, EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, true);
        verify(repository).update(EMAIL, EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, false);
    }

    @Test
    void set_acceptsLowercaseType() {
        service.set(EMAIL, "new_homework_assigned", true);

        verify(repository).update(EMAIL, EmailPreferenceType.NEW_HOMEWORK_ASSIGNED, true);
    }

    @Test
    void set_unknownType_rejectedAndNeverWritten() {
        assertThatThrownBy(() -> service.set(EMAIL, "NOT_A_REAL_OPTION", true))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void set_nullType_rejectedAndNeverWritten() {
        assertThatThrownBy(() -> service.set(EMAIL, null, true))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, org.mockito.Mockito.never())
                .update(anyString(), any(EmailPreferenceType.class), anyBoolean());
    }
}
