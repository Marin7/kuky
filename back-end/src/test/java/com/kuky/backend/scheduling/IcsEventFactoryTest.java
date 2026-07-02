package com.kuky.backend.scheduling;

import com.kuky.backend.scheduling.service.IcsEventFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IcsEventFactoryTest {

    private final IcsEventFactory factory = new IcsEventFactory();
    private final UUID bookingId = UUID.randomUUID();
    private final Instant slotStart = Instant.parse("2026-08-15T10:00:00Z");

    @Test
    void requestPayloadContainsExpectedFields() {
        String ics = new String(factory.build(IcsEventFactory.Method.REQUEST, bookingId, slotStart, 50,
                "Clase de español con Paula", "Enlace de Zoom: https://zoom.us/j/123", "https://zoom.us/j/123",
                "paula@kuky.es", "student@example.com"), StandardCharsets.UTF_8);

        assertThat(ics).contains("METHOD:REQUEST");
        assertThat(ics).contains("SEQUENCE:0");
        assertThat(ics).contains("STATUS:CONFIRMED");
        assertThat(ics).contains("UID:booking-" + bookingId + "@kuky.es");
        assertThat(ics).contains("DTSTART:20260815T100000Z");
        assertThat(ics).contains("DTEND:20260815T105000Z");
        assertThat(ics).contains("ORGANIZER;CN=Paula:mailto:paula@kuky.es");
        assertThat(ics).contains("mailto:student@example.com");
    }

    @Test
    void cancelPayloadUsesHigherSequenceAndCancelledStatus() {
        String ics = new String(factory.build(IcsEventFactory.Method.CANCEL, bookingId, slotStart, 50,
                "Clase de español con Paula", "Cancelada", "https://zoom.us/j/123",
                "paula@kuky.es", "student@example.com"), StandardCharsets.UTF_8);

        assertThat(ics).contains("METHOD:CANCEL");
        assertThat(ics).contains("SEQUENCE:1");
        assertThat(ics).contains("STATUS:CANCELLED");
    }

    @Test
    void requestAndCancelShareTheSameUidForTheSameBooking() {
        String uid = "UID:booking-" + bookingId + "@kuky.es";

        String request = new String(factory.build(IcsEventFactory.Method.REQUEST, bookingId, slotStart, 50,
                "s", "d", "l", "paula@kuky.es", "student@example.com"), StandardCharsets.UTF_8);
        String cancel = new String(factory.build(IcsEventFactory.Method.CANCEL, bookingId, slotStart, 50,
                "s", "d", "l", "paula@kuky.es", "student@example.com"), StandardCharsets.UTF_8);

        assertThat(request).contains(uid);
        assertThat(cancel).contains(uid);
    }
}
