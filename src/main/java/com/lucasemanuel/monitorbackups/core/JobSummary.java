package com.lucasemanuel.monitorbackups.core;

import java.time.LocalDateTime;

public record JobSummary(
        String job,
        BackupPolicy policy,
        LocalDateTime lastRunAt,
        BackupStatus lastStatus,
        LocalDateTime lastSuccessAt,
        long ageHours,
        AlertLevel level
) {
    public String lastStatusLabel() {
        return lastStatus == null ? "Sem registro" : lastStatus.label();
    }

    public String ageLabel() {
        return lastSuccessAt == null ? "—" : ageHours + "h";
    }
}
