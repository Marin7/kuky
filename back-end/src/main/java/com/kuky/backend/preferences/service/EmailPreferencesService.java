package com.kuky.backend.preferences.service;

import com.kuky.backend.preferences.dto.EmailPreferenceResponse;
import com.kuky.backend.preferences.model.EmailPreferenceType;
import com.kuky.backend.preferences.repository.EmailPreferencesRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmailPreferencesService {

    private final EmailPreferencesRepository repository;

    public EmailPreferencesService(EmailPreferencesRepository repository) {
        this.repository = repository;
    }

    /**
     * Every supported option with this account's effective value, in enum declaration order.
     * Never omits a supported option, so the client can render the list the server returns
     * rather than hardcoding one that could drift.
     */
    public List<EmailPreferenceResponse> list(String email) {
        Map<EmailPreferenceType, Boolean> stored = repository.findByEmail(email);
        List<EmailPreferenceResponse> out = new ArrayList<>();
        for (EmailPreferenceType type : EmailPreferenceType.values()) {
            out.add(new EmailPreferenceResponse(type, Boolean.TRUE.equals(stored.get(type))));
        }
        return out;
    }

    /** Sets one option. Idempotent — writing the value already held is a success. */
    public EmailPreferenceResponse set(String email, String rawType, boolean enabled) {
        EmailPreferenceType type = parse(rawType);
        repository.update(email, type, enabled);
        return new EmailPreferenceResponse(type, enabled);
    }

    /**
     * An unrecognised option is a client error, never a silent no-op. Surfaces as the
     * existing VALIDATION_ERROR via GlobalExceptionHandler's IllegalArgumentException handler.
     */
    private EmailPreferenceType parse(String rawType) {
        if (rawType == null) {
            throw new IllegalArgumentException("Preferencia de correo desconocida.");
        }
        try {
            return EmailPreferenceType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Preferencia de correo desconocida: " + rawType);
        }
    }
}
