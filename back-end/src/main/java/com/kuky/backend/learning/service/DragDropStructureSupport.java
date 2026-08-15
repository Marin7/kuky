package com.kuky.backend.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kuky.backend.learning.ExerciseStructureLimits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dual-read helper for DRAG_DROP {@code structure_json}: canonical
 * {@code blanks[].correctBankIds} or legacy positional {@code bank[i] → blank i}.
 */
public final class DragDropStructureSupport {

    private DragDropStructureSupport() {}

    public record ResolvedBlank(List<String> correctBankIds) {
        public ResolvedBlank {
            correctBankIds = List.copyOf(correctBankIds);
        }
    }

    public record Resolved(JsonNode bank, List<ResolvedBlank> blanks) {
        public Resolved {
            blanks = List.copyOf(blanks);
        }

        public int blankCount() {
            return blanks.size();
        }
    }

    /**
     * Resolves correct bank ids per blank. Prefer canonical {@code blanks} when
     * present and non-empty; otherwise treat bank order as the answer key
     * (legacy). Does not validate caps — callers that author must validate.
     */
    public static Resolved resolve(JsonNode structure) {
        if (structure == null || structure.isNull() || structure.isMissingNode()) {
            return new Resolved(null, List.of());
        }
        JsonNode bank = structure.path("bank");
        JsonNode blanksNode = structure.path("blanks");
        if (blanksNode.isArray() && !blanksNode.isEmpty()) {
            List<ResolvedBlank> blanks = new ArrayList<>(blanksNode.size());
            for (JsonNode blank : blanksNode) {
                List<String> ids = new ArrayList<>();
                JsonNode idsNode = blank.path("correctBankIds");
                if (idsNode.isArray()) {
                    for (JsonNode idNode : idsNode) {
                        if (idNode != null && idNode.isTextual()) {
                            String id = idNode.asText();
                            if (id != null && !id.isBlank()) {
                                ids.add(id.strip());
                            }
                        }
                    }
                }
                blanks.add(new ResolvedBlank(ids));
            }
            return new Resolved(bank, blanks);
        }
        if (!bank.isArray() || bank.isEmpty()) {
            return new Resolved(bank, List.of());
        }
        List<ResolvedBlank> blanks = new ArrayList<>(bank.size());
        for (JsonNode item : bank) {
            String id = item.path("id").asText(null);
            if (id == null || id.isBlank()) {
                blanks.add(new ResolvedBlank(List.of()));
            } else {
                blanks.add(new ResolvedBlank(List.of(id.strip())));
            }
        }
        return new Resolved(bank, blanks);
    }

    /**
     * True when any bank item id is designated correct for two or more blanks.
     * Legacy positional keys never share an id, so they are exclusive.
     */
    public static boolean isBankReusable(Resolved resolved) {
        if (resolved == null) return false;
        Set<String> seen = new HashSet<>();
        for (ResolvedBlank blank : resolved.blanks()) {
            for (String id : blank.correctBankIds()) {
                if (id == null || id.isBlank()) continue;
                if (!seen.add(id)) return true;
            }
        }
        return false;
    }

    /** Labels for the given bank item ids, in the same order as {@code ids}. */
    public static List<String> labelsForIds(JsonNode bank, List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        if (bank == null || !bank.isArray()) return Collections.nCopies(ids.size(), null);
        List<String> labels = new ArrayList<>(ids.size());
        for (String id : ids) {
            labels.add(labelForId(bank, id));
        }
        return labels;
    }

    public static String labelForId(JsonNode bank, String id) {
        if (id == null || bank == null || !bank.isArray()) return null;
        for (JsonNode item : bank) {
            if (id.equals(item.path("id").asText(null))) {
                String label = item.path("label").asText(null);
                return label;
            }
        }
        return null;
    }

    public static int maxAcceptedPerBlank() {
        return ExerciseStructureLimits.MAX_ACCEPTED_PER_BLANK;
    }

    public static int maxBankItems() {
        return ExerciseStructureLimits.MAX_BANK_ITEMS;
    }
}
