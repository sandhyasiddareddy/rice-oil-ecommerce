package com.riceoil.platform.imports.api.admin;

public record ImportPreviewResponse(
        Long jobId,
        int totalRows,
        int validRows,
        int invalidRows
) {
}
