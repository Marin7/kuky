package com.kuky.backend.learning.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses exact {@code ___} blank tokens in a passage (three underscores not
 * adjacent to another underscore).
 */
public final class BlankPassageParser {

    private static final Pattern BLANK = Pattern.compile("(?<!_)___(?!_)");

    private BlankPassageParser() {}

    /** Number of blank tokens in {@code prompt}. */
    public static int countBlanks(String prompt) {
        if (prompt == null || prompt.isEmpty()) return 0;
        Matcher m = BLANK.matcher(prompt);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /**
     * Splits the passage into alternating text segments and blank placeholders.
     * Result always starts with a text segment (possibly empty); blanks are
     * represented as {@code null} entries between text parts.
     * <p>
     * Example: {@code "a ___ b ___ c"} → ["a ", null, " b ", null, " c"]
     */
    public static List<String> splitSegments(String prompt) {
        List<String> parts = new ArrayList<>();
        if (prompt == null) {
            parts.add("");
            return parts;
        }
        Matcher m = BLANK.matcher(prompt);
        int last = 0;
        while (m.find()) {
            parts.add(prompt.substring(last, m.start()));
            parts.add(null); // blank marker
            last = m.end();
        }
        parts.add(prompt.substring(last));
        return parts;
    }
}
