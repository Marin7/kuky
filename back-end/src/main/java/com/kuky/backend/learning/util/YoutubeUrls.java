package com.kuky.backend.learning.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and validates public YouTube watch / short / embed URLs. */
public final class YoutubeUrls {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern WATCH = Pattern.compile(
            "(?:youtube\\.com|youtube-nocookie\\.com)/watch\\?(?:[^#]*&)?v=([A-Za-z0-9_-]{11})");
    private static final Pattern SHORT = Pattern.compile(
            "(?:youtube\\.com|youtube-nocookie\\.com)/shorts/([A-Za-z0-9_-]{11})");
    private static final Pattern EMBED = Pattern.compile(
            "(?:youtube\\.com|youtube-nocookie\\.com)/embed/([A-Za-z0-9_-]{11})");
    private static final Pattern YOUTU_BE = Pattern.compile(
            "youtu\\.be/([A-Za-z0-9_-]{11})");

    private YoutubeUrls() {}

    public static Optional<String> extractVideoId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String url = raw.strip();
        for (Pattern p : new Pattern[] {WATCH, SHORT, EMBED, YOUTU_BE}) {
            Matcher m = p.matcher(url);
            if (m.find()) {
                String id = m.group(1);
                if (ID_PATTERN.matcher(id).matches()) {
                    return Optional.of(id);
                }
            }
        }
        if (ID_PATTERN.matcher(url).matches()) {
            return Optional.of(url);
        }
        return Optional.empty();
    }

    public static String embedUrl(String videoId) {
        return "https://www.youtube-nocookie.com/embed/" + videoId;
    }
}
