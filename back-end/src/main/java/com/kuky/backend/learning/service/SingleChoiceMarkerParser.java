package com.kuky.backend.learning.service;

import com.kuky.backend.learning.ExerciseStructureLimits;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses numbered opción única markers {@code (1)}, {@code (2)}, … in a prompt.
 * {@code (01)} ≡ {@code (1)}; non-numeric parentheses such as {@code (ser)} are ignored.
 */
public final class SingleChoiceMarkerParser {

    public static final int MAX_ITEMS = ExerciseStructureLimits.MAX_SINGLE_CHOICE_ITEMS;

    private static final Pattern MARKER = Pattern.compile("\\((\\d+)\\)");

    private SingleChoiceMarkerParser() {}

    /** Distinct marker numbers ≥ 1, in numeric order. Empty when the prompt is classic. */
    public static SortedSet<Integer> distinctNumbers(String prompt) {
        SortedSet<Integer> numbers = new TreeSet<>();
        if (prompt == null || prompt.isEmpty()) return numbers;
        Matcher m = MARKER.matcher(prompt);
        while (m.find()) {
            try {
                int n = Integer.parseInt(m.group(1));
                if (n >= 1) numbers.add(n);
            } catch (NumberFormatException ignored) {
                // Overflow / unusable token — ignore, same as non-numeric parentheses.
            }
        }
        return numbers;
    }

    public static boolean hasMarkers(String prompt) {
        return !distinctNumbers(prompt).isEmpty();
    }

    /**
     * Classic (no markers) or numbered {@code 1…N}. Invalid when numbers are gapped,
     * do not start at 1, or {@code N} exceeds {@link #MAX_ITEMS}.
     */
    public static ParseResult parse(String prompt) {
        SortedSet<Integer> numbers = distinctNumbers(prompt);
        if (numbers.isEmpty()) {
            return ParseResult.classic();
        }
        int n = numbers.last();
        if (n > MAX_ITEMS) {
            return ParseResult.invalid(
                    "Una pregunta de opción única numerada no puede tener más de "
                            + MAX_ITEMS + " ítems.");
        }
        for (int i = 1; i <= n; i++) {
            if (!numbers.contains(i)) {
                return ParseResult.invalid(
                        "Los números entre paréntesis deben ser consecutivos desde (1), sin huecos.");
            }
        }
        return ParseResult.numbered(n);
    }

    public record ParseResult(boolean numbered, boolean valid, int n, String errorMessage) {
        static ParseResult classic() {
            return new ParseResult(false, true, 0, null);
        }

        static ParseResult numbered(int n) {
            return new ParseResult(true, true, n, null);
        }

        static ParseResult invalid(String message) {
            return new ParseResult(true, false, 0, message);
        }

        public SortedSet<Integer> expectedNumbers() {
            if (!numbered || !valid || n < 1) return Collections.emptySortedSet();
            SortedSet<Integer> set = new TreeSet<>();
            for (int i = 1; i <= n; i++) set.add(i);
            return set;
        }
    }
}
