package com.lucasemanuel.monitorbackups.core;

import java.time.LocalDateTime;
import java.util.List;

public record BackupReport(
        LocalDateTime generatedAt,
        List<BackupRecord> records,
        List<JobSummary> jobs,
        List<BackupAlert> alerts,
        List<DailySummary> daily
) {
    public BackupReport {
        records = List.copyOf(records);
        jobs = List.copyOf(jobs);
        alerts = List.copyOf(alerts);
        daily = List.copyOf(daily);
    }

    public long criticalCount() {
        return alerts.stream().filter(alert -> alert.level() == AlertLevel.CRITICAL).count();
    }

    public long warningCount() {
        return alerts.stream().filter(alert -> alert.level() == AlertLevel.WARNING).count();
    }
}
