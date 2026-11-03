package com.riceoil.platform.imports.application;

public record ImportPreviewResult(
        Long jobId,
        int totalRows,
        int validRows,
        int invalidRows
) {
}
