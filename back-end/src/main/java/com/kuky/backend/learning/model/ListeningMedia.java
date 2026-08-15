package com.kuky.backend.learning.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helpers for listening homework media completeness and YouTube URL parsing. */
public final class ListeningMedia {

    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([\\w-]{11})");

    private ListeningMedia() {}

    public static boolean isComplete(HomeworkAssignment a) {
        if (a == null || a.getHomeworkType() != HomeworkType.AUDIO) {
            return true;
        }
        return isComplete(a.getMediaSourceKind(), a.getAudioUrl(), a.getAudioFileId());
    }

    public static boolean isComplete(MediaSourceKind kind, String audioUrl, java.util.UUID audioFileId) {
        if (kind == null) {
            return false;
        }
        return switch (kind) {
            case UPLOADED_FILE -> audioFileId != null;
            case AUDIO_URL, VIDEO_PAGE -> audioUrl != null && !audioUrl.isBlank();
            case YOUTUBE -> extractYouTubeId(audioUrl) != null;
        };
    }

    public static String extractYouTubeId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher m = YOUTUBE_ID.matcher(url.strip());
        return m.find() ? m.group(1) : null;
    }
}
