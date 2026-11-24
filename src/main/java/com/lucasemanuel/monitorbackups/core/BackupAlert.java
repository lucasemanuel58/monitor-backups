package com.lucasemanuel.monitorbackups.core;

public record BackupAlert(String job, AlertLevel level, String title, String message) {
}
