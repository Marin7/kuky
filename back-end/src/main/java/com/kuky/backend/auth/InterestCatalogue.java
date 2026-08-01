package com.kuky.backend.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Product-fixed interest catalogue — not editable at runtime. */
public final class InterestCatalogue {

    public static final int MAX_SELECTIONS = 10;
    public static final int MAX_NOTE_LENGTH = 280;

    public static final Set<String> CODES = Set.of(
            "TRAVEL", "MUSIC", "SPORTS", "FOOD", "CINEMA", "READING",
            "TECHNOLOGY", "NATURE", "ART", "WORK", "FAMILY", "CULTURE"
    );

    private InterestCatalogue() {}

    public static boolean isAllowed(String code) {
        return code != null && CODES.contains(code);
    }

    /** Drops unknown/retired codes; preserves encounter order, unique. */
    public static List<String> filterKnown(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> known = new LinkedHashSet<>();
        for (String code : codes) {
            if (isAllowed(code)) {
                known.add(code);
            }
        }
        return List.copyOf(known);
    }
}
