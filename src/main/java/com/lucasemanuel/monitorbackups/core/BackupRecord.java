package com.lucasemanuel.monitorbackups.core;

import java.time.LocalDateTime;
import java.util.Objects;

public record BackupRecord(
        String job,
        LocalDateTime executedAt,
        BackupStatus status,
        int durationMinutes,
        String details
) {
    public BackupRecord {
        Objects.requireNonNull(job, "job");
        Objects.requireNonNull(executedAt, "executedAt");
        Objects.requireNonNull(status, "status");
        if (job.isBlank()) {
            throw new IllegalArgumentException("Job sem nome");
        }
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Duração não pode ser negativa: " + job);
        }
    }
}
