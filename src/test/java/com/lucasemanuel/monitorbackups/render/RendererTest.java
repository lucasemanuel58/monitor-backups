package com.lucasemanuel.monitorbackups.render;

import com.lucasemanuel.monitorbackups.core.BackupAnalyzer;
import com.lucasemanuel.monitorbackups.core.BackupRecord;
import com.lucasemanuel.monitorbackups.core.BackupStatus;
import com.lucasemanuel.monitorbackups.core.BackupReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendererTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-11-24T06:00");
    private final BackupAnalyzer analyzer = new BackupAnalyzer(NOW);

    @Test
    void consoleMostraAlertasEHistorico() {
        String console = new ConsoleRenderer().render(report(exampleRecords()));

        assertTrue(console.contains("RELATÓRIO DE BACKUPS"));
        assertTrue(console.contains("[CRÍTICO] financeiro"));
        assertTrue(console.contains("HISTÓRICO POR DIA"));
        assertTrue(console.contains("sucesso=1"));
    }

    @Test
    void markdownEscapaPipes() {
        var records = List.of(new BackupRecord(
                "clientes|qualidade", LocalDateTime.parse("2026-11-24T05:00"),
                BackupStatus.SUCCESS, 10, "concluido"
        ));

        String markdown = new MarkdownRenderer().render(analyzer.analyze(records));

        assertTrue(markdown.contains("clientes\\|qualidade"));
        assertFalse(markdown.contains("clientes|qualidade"));
        assertTrue(markdown.contains("## Resumo por job"));
    }

    @Test
    void htmlEscapaTagsEAspas() {
        var records = List.of(
                new BackupRecord("financeiro", LocalDateTime.parse("2026-11-23T02:00"), BackupStatus.SUCCESS, 38, "concluido"),
                new BackupRecord("financeiro", LocalDateTime.parse("2026-11-24T02:00"), BackupStatus.FAILED, 5, "<script>alert(\"x\")</script>")
        );

        String html = new HtmlRenderer().render(analyzer.analyze(records));

        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&quot;x&quot;"));
        assertTrue(html.contains("class=\"alert CRITICAL\""));
        assertTrue(html.contains("<table>"));
    }

    @Test
    void htmlMarcaNivelDeAtencao() {
        var records = List.of(new BackupRecord(
                "inventario", LocalDateTime.parse("2026-11-20T04:00"),
                BackupStatus.SUCCESS, 14, "concluido"
        ));

        String html = new HtmlRenderer().render(analyzer.analyze(records));

        assertTrue(html.contains("class=\"alert WARNING\""));
        assertTrue(html.contains("Backup fora do prazo"));
    }

    private static BackupReport report(List<BackupRecord> records) {
        return analyzer().analyze(records);
    }

    private static BackupAnalyzer analyzer() {
        return new BackupAnalyzer(NOW);
    }

    private static List<BackupRecord> exampleRecords() {
        return List.of(
                new BackupRecord("financeiro", LocalDateTime.parse("2026-11-23T02:15"), BackupStatus.SUCCESS, 38, "concluido"),
                new BackupRecord("financeiro", LocalDateTime.parse("2026-11-24T02:12"), BackupStatus.FAILED, 5, "timeout no destino"),
                new BackupRecord("clientes", LocalDateTime.parse("2026-11-24T01:30"), BackupStatus.SUCCESS, 51, "concluido"),
                new BackupRecord("logs-aplicacao", LocalDateTime.parse("2026-11-24T05:45"), BackupStatus.SKIPPED, 0, "janela compartilhada")
        );
    }
}
