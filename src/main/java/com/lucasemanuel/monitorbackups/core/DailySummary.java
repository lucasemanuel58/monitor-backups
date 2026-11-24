package com.lucasemanuel.monitorbackups.core;

import java.time.LocalDate;

public record DailySummary(LocalDate date, long success, long failed, long skipped) {
}
