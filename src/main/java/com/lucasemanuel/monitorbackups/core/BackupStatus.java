package com.lucasemanuel.monitorbackups.core;

public enum BackupStatus {
    SUCCESS("Sucesso"),
    FAILED("Falhou"),
    SKIPPED("Ignorado");

    private final String label;

    BackupStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static BackupStatus parse(String value) {
        for (BackupStatus status : values()) {
            if (status.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status desconhecido: " + value);
    }
}
