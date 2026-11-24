package com.lucasemanuel.monitorbackups.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupAnalyzerTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-11-24T06:00");

    private final BackupAnalyzer analyzer = new BackupAnalyzer(NOW);

    @Test
    void falhaEmJobCriticoGeraAlertaCritico() {
        var report = analyzer.analyze(exampleRecords());

        var alert = alert(report, "financeiro");
        assertEquals(AlertLevel.CRITICAL, alert.level());
        assertEquals("Falha na última execução", alert.title());
        assertTrue(alert.message().contains("timeout no destino"));
    }

    @Test
    void backupRecenteNaoGeraAlerta() {
        var report = analyzer.analyze(exampleRecords());

        assertTrue(report.alerts().stream().noneMatch(alert -> alert.job().equals("clientes")));
        assertEquals(AlertLevel.OK, summary(report, "clientes").level());
        assertEquals(4, summary(report, "clientes").ageHours());
    }

    @Test
    void jobsForaDoPrazoGeramAtencao() {
        var report = analyzer.analyze(exampleRecords());

        var chamados = alert(report, "sistema-chamados");
        assertEquals(AlertLevel.WARNING, chamados.level());
        assertEquals("Backup fora do prazo", chamados.title());
        assertEquals(51, summary(report, "sistema-chamados").ageHours());

        assertEquals(AlertLevel.WARNING, alert(report, "inventario").level());
    }

    @Test
    void statusIgnoradoNaoAnulaSucessoRecente() {
        var report = analyzer.analyze(exampleRecords());

        assertTrue(report.alerts().stream().noneMatch(alert -> alert.job().equals("logs-aplicacao")));
        assertEquals(BackupStatus.SKIPPED, summary(report, "logs-aplicacao").lastStatus());
        assertEquals(AlertLevel.OK, summary(report, "logs-aplicacao").level());
    }

    @Test
    void jobSemRegistrosUsaPoliticaPadraoEApareceNoResumo() {
        var report = analyzer.analyze(List.of());

        var alert = alert(report, "financeiro");
        assertEquals(AlertLevel.CRITICAL, alert.level());
        assertEquals("Sem sucesso registrado", alert.title());
        assertTrue(alert.message().contains("Nenhuma execução encontrada"));
        assertEquals("Sem registro", summary(report, "financeiro").lastStatusLabel());
        assertEquals(5, report.jobs().size());
    }

    @Test
    void jobCriticoSemSucessoECriticoMesmoComFalha() {
        var records = List.of(
                record("financeiro", "2026-11-24T01:00", BackupStatus.FAILED, 3, "timeout")
        );

        var alert = alert(analyzer.analyze(records), "financeiro");

        assertEquals(AlertLevel.CRITICAL, alert.level());
        assertEquals("Sem sucesso registrado", alert.title());
    }

    @Test
    void jobDesconhecidoUsaPrazoPadraoDe24Horas() {
        var records = List.of(
                record("novo-job", "2026-11-22T06:00", BackupStatus.SUCCESS, 10, "concluido")
        );

        var report = analyzer.analyze(records);

        assertEquals(24, summary(report, "novo-job").policy().maxAgeHours());
        assertEquals(AlertLevel.WARNING, alert(report, "novo-job").level());
        assertEquals(6, report.jobs().size());
    }

    @Test
    void historicoAgrupaPorDiaEOrdenaDoMaisRecente() {
        var report = analyzer.analyze(exampleRecords());

        assertEquals(4, report.daily().size());
        var hoje = report.daily().get(0);
        assertEquals("2026-11-24", hoje.date().toString());
        assertEquals(2, hoje.success());
        assertEquals(1, hoje.failed());
        assertEquals(1, hoje.skipped());
        assertEquals("2026-11-23", report.daily().get(1).date().toString());
        assertEquals("2026-11-22", report.daily().get(2).date().toString());
        assertEquals("2026-11-24T05:45", report.records().get(0).executedAt().toString());
    }

    @Test
    void alertasSaoOrdenadosPorCriticidade() {
        var report = analyzer.analyze(exampleRecords());

        assertEquals(AlertLevel.CRITICAL, report.alerts().get(0).level());
        assertTrue(report.alerts().stream()
                .allMatch(alert -> alert.level() != AlertLevel.OK));
        assertEquals(3, report.alerts().size());
    }

    private static List<BackupRecord> exampleRecords() {
        return List.of(
                record("financeiro", "2026-11-23T02:15", BackupStatus.SUCCESS, 38, "concluido"),
                record("financeiro", "2026-11-24T02:12", BackupStatus.FAILED, 5, "timeout no destino"),
                record("clientes", "2026-11-24T01:30", BackupStatus.SUCCESS, 51, "concluido"),
                record("sistema-chamados", "2026-11-22T03:00", BackupStatus.SUCCESS, 22, "concluido"),
                record("inventario", "2026-11-20T04:00", BackupStatus.SUCCESS, 14, "concluido"),
                record("logs-aplicacao", "2026-11-24T00:30", BackupStatus.SUCCESS, 9, "concluido"),
                record("logs-aplicacao", "2026-11-24T05:45", BackupStatus.SKIPPED, 0, "janela compartilhada")
        );
    }

    private static BackupRecord record(String job, String when, BackupStatus status, int duration, String details) {
        return new BackupRecord(job, LocalDateTime.parse(when), status, duration, details);
    }

    private static BackupAlert alert(BackupReport report, String job) {
        return report.alerts().stream()
                .filter(candidate -> candidate.job().equals(job))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Alerta não encontrado: " + job));
    }

    private static JobSummary summary(BackupReport report, String job) {
        return report.jobs().stream()
                .filter(candidate -> candidate.job().equals(job))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Resumo não encontrado: " + job));
    }
}
