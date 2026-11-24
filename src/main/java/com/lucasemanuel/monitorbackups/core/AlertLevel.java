package com.lucasemanuel.monitorbackups.core;

public enum AlertLevel {
    OK(0, "OK"),
    WARNING(1, "ATENÇÃO"),
    CRITICAL(2, "CRÍTICO");

    private final int rank;
    private final String label;

    AlertLevel(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int rank() {
        return rank;
    }

    public String label() {
        return label;
    }
}
