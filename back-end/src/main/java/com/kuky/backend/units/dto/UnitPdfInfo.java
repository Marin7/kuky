package com.kuky.backend.units.dto;

import java.util.List;
import java.util.UUID;

/** The unit's PDF: the owned presentation and its single file. */
public record UnitPdfInfo(
        UUID presentationId,
        UUID fileId,
        String originalName,
        int byteSize,
        int pageCount,
        /** Index 0 = page 1; one title per page. */
        List<String> pageTitles
) {}
