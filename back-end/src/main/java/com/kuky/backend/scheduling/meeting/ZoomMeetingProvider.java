package com.kuky.backend.scheduling.meeting;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.MeetingProvisioningException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ZoomMeetingProvider implements MeetingProvider {

    private static final String TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String API_BASE = "https://api.zoom.us/v2";

    private final SchedulingProperties props;
    private final RestClient restClient;
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();
    private final AtomicReference<MeetingDetails> personalMeetingCache = new AtomicReference<>();

    public ZoomMeetingProvider(SchedulingProperties props) {
        this.props = props;
        this.restClient = RestClient.create();
    }

    private record CachedToken(String accessToken, long expiresAt) {}

    private String getAccessToken() {
        CachedToken cached = tokenCache.get();
        if (cached != null && System.currentTimeMillis() < cached.expiresAt()) {
            return cached.accessToken();
        }
        String accountId = props.getZoom().getAccountId();
        String clientId = props.getZoom().getClientId();
        String clientSecret = props.getZoom().getClientSecret();
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(TOKEN_URL + "?grant_type=account_credentials&account_id=" + accountId)
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !response.containsKey("access_token")) {
                throw new MeetingProvisioningException("No se pudo obtener el token de Zoom.");
            }
            String token = (String) response.get("access_token");
            int expiresIn = (Integer) response.getOrDefault("expires_in", 3600);
            // Cache with 60s buffer
            tokenCache.set(new CachedToken(token, System.currentTimeMillis() + (expiresIn - 60) * 1000L));
            return token;
        } catch (RestClientException e) {
            throw new MeetingProvisioningException("Error de autenticación con Zoom.", e);
        }
    }

    @Override
    public MeetingDetails create(Instant start, int durationMinutes, String topic) {
        MeetingDetails cached = personalMeetingCache.get();
        if (cached != null) {
            return cached;
        }
        String token = getAccessToken();
        String userId = props.getZoom().getUserId();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(API_BASE + "/users/" + userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !response.containsKey("personal_meeting_url")) {
                throw new MeetingProvisioningException("Respuesta inesperada de Zoom.");
            }
            MeetingDetails details = new MeetingDetails(
                    String.valueOf(response.get("pmi")),
                    (String) response.get("personal_meeting_url")
            );
            // The teacher's Personal Meeting Room is stable, so it's cached for the lifetime of
            // the application instead of being looked up on every booking.
            personalMeetingCache.set(details);
            return details;
        } catch (RestClientException e) {
            throw new MeetingProvisioningException("No se pudo obtener la sala personal de Zoom.", e);
        }
    }

    @Override
    public void cancel(String meetingId) {
        // No-op: bookings share the teacher's Personal Meeting Room, which isn't tied to any
        // single booking and must never be deleted from Zoom.
    }
}
