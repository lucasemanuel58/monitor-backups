package com.lucasemanuel.monitorbackups.core;

public record BackupPolicy(String job, int maxAgeHours, boolean critical) {

    public BackupPolicy {
        if (job == null || job.isBlank()) {
            throw new IllegalArgumentException("Política sem job");
        }
        if (maxAgeHours <= 0) {
            throw new IllegalArgumentException("Prazo inválido para " + job);
        }
    }
}
