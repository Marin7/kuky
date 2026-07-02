package com.kuky.backend.scheduling.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Builds minimal RFC 5545/5546-compliant iCalendar payloads for a single booking event.
 * No recurrence, no VTIMEZONE — all times are emitted in UTC, which is unambiguous for
 * every recipient regardless of their calendar app's configured time zone.
 */
public class IcsEventFactory {

    public enum Method {
        REQUEST(0, "CONFIRMED"),
        CANCEL(1, "CANCELLED");

        final int sequence;
        final String status;

        Method(int sequence, String status) {
            this.sequence = sequence;
            this.status = status;
        }
    }

    private static final DateTimeFormatter UTC_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);
    private static final int FOLD_LIMIT = 75;

    public byte[] build(Method method, UUID bookingId, Instant slotStart, int durationMinutes,
                        String summary, String description, String location,
                        String organizerEmail, String attendeeEmail) {
        Instant dtEnd = slotStart.plus(durationMinutes, ChronoUnit.MINUTES);
        String uid = "booking-" + bookingId + "@kuky.es";

        StringBuilder sb = new StringBuilder();
        appendLine(sb, "BEGIN:VCALENDAR");
        appendLine(sb, "PRODID:-//Kuky//Espanol con Paula//ES");
        appendLine(sb, "VERSION:2.0");
        appendLine(sb, "METHOD:" + method.name());
        appendLine(sb, "CALSCALE:GREGORIAN");
        appendLine(sb, "BEGIN:VEVENT");
        appendLine(sb, "UID:" + uid);
        appendLine(sb, "DTSTAMP:" + UTC_STAMP.format(Instant.now()));
        appendLine(sb, "DTSTART:" + UTC_STAMP.format(slotStart));
        appendLine(sb, "DTEND:" + UTC_STAMP.format(dtEnd));
        appendLine(sb, "SEQUENCE:" + method.sequence);
        appendLine(sb, "STATUS:" + method.status);
        appendLine(sb, "SUMMARY:" + escape(summary));
        appendLine(sb, "DESCRIPTION:" + escape(description));
        appendLine(sb, "LOCATION:" + escape(location));
        appendLine(sb, "ORGANIZER;CN=Paula:mailto:" + organizerEmail);
        appendLine(sb, "ATTENDEE;CN=" + escape(attendeeEmail) + ":mailto:" + attendeeEmail);
        appendLine(sb, "END:VEVENT");
        appendLine(sb, "END:VCALENDAR");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /** Appends a content line, folding at 75 octets per RFC 5545 §3.1, terminated with CRLF. */
    private static void appendLine(StringBuilder sb, String line) {
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= FOLD_LIMIT) {
            sb.append(line).append("\r\n");
            return;
        }
        int offset = 0;
        boolean first = true;
        while (offset < bytes.length) {
            int limit = first ? FOLD_LIMIT : FOLD_LIMIT - 1;
            int end = Math.min(offset + limit, bytes.length);
            sb.append(new String(bytes, offset, end - offset, StandardCharsets.UTF_8));
            sb.append("\r\n");
            offset = end;
            if (offset < bytes.length) {
                sb.append(' ');
            }
            first = false;
        }
    }
}
