package com.lucasemanuel.monitorbackups.core;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/** Aplica políticas fictícias aos registros e transforma isso em alertas e resumos. */
public final class BackupAnalyzer {
    private final PolicyCatalog catalog;
    private final LocalDateTime referenceTime;

    public BackupAnalyzer() {
        this(new PolicyCatalog(), LocalDateTime.now());
    }

    public BackupAnalyzer(LocalDateTime referenceTime) {
        this(new PolicyCatalog(), referenceTime);
    }

    public BackupAnalyzer(PolicyCatalog catalog, LocalDateTime referenceTime) {
        this.catalog = catalog;
        this.referenceTime = referenceTime;
    }

    public BackupReport analyze(List<BackupRecord> records) {
        Map<String, List<BackupRecord>> byJob = new LinkedHashMap<>();
        records.forEach(record ->
                byJob.computeIfAbsent(record.job(), key -> new ArrayList<>()).add(record)
        );

        TreeSet<String> jobs = new TreeSet<>(catalog.jobs());
        jobs.addAll(byJob.keySet());

        List<JobSummary> summaries = new ArrayList<>();
        List<BackupAlert> alerts = new ArrayList<>();

        for (String job : jobs) {
            BackupPolicy policy = catalog.policyFor(job);
            List<BackupRecord> ascending = byJob.getOrDefault(job, List.of()).stream()
                    .sorted(Comparator.comparing(BackupRecord::executedAt))
                    .toList();

            Optional<BackupRecord> lastRun = ascending.stream().max(Comparator.comparing(BackupRecord::executedAt));
            Optional<BackupRecord> lastSuccess = ascending.stream()
                    .filter(record -> record.status() == BackupStatus.SUCCESS)
                    .max(Comparator.comparing(BackupRecord::executedAt));

            LocalDateTime successAt = lastSuccess.map(BackupRecord::executedAt).orElse(null);
            long ageHours = successAt == null
                    ? -1
                    : Math.max(0, Duration.between(successAt, referenceTime).toHours());

            boolean failedAfterSuccess = successAt != null
                    && lastRun.isPresent()
                    && lastRun.get().status() == BackupStatus.FAILED
                    && lastRun.get().executedAt().isAfter(successAt);
            boolean stale = successAt != null && ageHours > policy.maxAgeHours();

            AlertLevel level = AlertLevel.OK;
            String title = "Dentro do prazo";
            String message = successAt == null
                    ? ""
                    : "Último sucesso em " + successAt + " (" + ageHours + "h atrás).";

            if (successAt == null) {
                level = policy.critical() ? AlertLevel.CRITICAL : AlertLevel.WARNING;
                title = "Sem sucesso registrado";
                message = ascending.isEmpty()
                        ? "Nenhuma execução encontrada para este job."
                        : "Foram encontradas " + ascending.size()
                        + " execução(ões), mas nenhuma com status SUCESSO.";
            } else if (failedAfterSuccess) {
                level = policy.critical() ? AlertLevel.CRITICAL : AlertLevel.WARNING;
                title = "Falha na última execução";
                message = "Última execução falhou em " + lastRun.get().executedAt()
                        + ": " + lastRun.get().details()
                        + " Último sucesso em " + successAt + ".";
            } else if (stale) {
                level = policy.critical() ? AlertLevel.CRITICAL : AlertLevel.WARNING;
                title = "Backup fora do prazo";
                message = "Último sucesso há " + ageHours
                        + "h; prazo definido: " + policy.maxAgeHours() + "h.";
            }

            summaries.add(new JobSummary(
                    job,
                    policy,
                    lastRun.map(BackupRecord::executedAt).orElse(null),
                    lastRun.map(BackupRecord::status).orElse(null),
                    successAt,
                    ageHours,
                    level
            ));

            if (level != AlertLevel.OK) {
                alerts.add(new BackupAlert(job, level, title, message));
            }
        }

        alerts.sort(Comparator.comparingInt((BackupAlert alert) -> alert.level().rank()).reversed()
                .thenComparing(BackupAlert::job));

        List<BackupRecord> recentFirst = records.stream()
                .sorted(Comparator.comparing(BackupRecord::executedAt).reversed())
                .toList();

        return new BackupReport(
                referenceTime,
                recentFirst,
                summaries,
                alerts,
                dailySummary(records)
        );
    }

    private static List<DailySummary> dailySummary(List<BackupRecord> records) {
        Map<LocalDate, long[]> counts = new TreeMap<>(Comparator.reverseOrder());
        for (BackupRecord record : records) {
            long[] bucket = counts.computeIfAbsent(
                    record.executedAt().toLocalDate(), date -> new long[3]
            );
            switch (record.status()) {
                case SUCCESS -> bucket[0]++;
                case FAILED -> bucket[1]++;
                case SKIPPED -> bucket[2]++;
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new DailySummary(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]
                ))
                .toList();
    }
}
