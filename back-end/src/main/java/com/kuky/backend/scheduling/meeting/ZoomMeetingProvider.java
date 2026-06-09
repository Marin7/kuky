package com.kuky.backend.scheduling.meeting;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.MeetingProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ZoomMeetingProvider implements MeetingProvider {

    private static final Logger log = LoggerFactory.getLogger(ZoomMeetingProvider.class);
    private static final String TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String API_BASE = "https://api.zoom.us/v2";
    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final SchedulingProperties props;
    private final RestClient restClient;
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

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
        String token = getAccessToken();
        String userId = props.getZoom().getUserId();
        Map<String, Object> body = Map.of(
                "type", 2,
                "topic", topic,
                "start_time", ISO_UTC.format(start),
                "duration", durationMinutes,
                "timezone", "UTC"
        );
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(API_BASE + "/users/" + userId + "/meetings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !response.containsKey("join_url")) {
                throw new MeetingProvisioningException("Respuesta inesperada de Zoom.");
            }
            return new MeetingDetails(
                    String.valueOf(response.get("id")),
                    (String) response.get("join_url")
            );
        } catch (RestClientException e) {
            throw new MeetingProvisioningException("No se pudo crear la reunión de Zoom.", e);
        }
    }

    @Override
    public void cancel(String meetingId) {
        try {
            String token = getAccessToken();
            restClient.delete()
                    .uri(API_BASE + "/meetings/" + meetingId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo cancelar la reunión de Zoom {}: {}", meetingId, e.getMessage());
        }
    }
}
