package com.lucasemanuel.monitorbackups.render;

import com.lucasemanuel.monitorbackups.core.BackupAlert;
import com.lucasemanuel.monitorbackups.core.BackupReport;
import com.lucasemanuel.monitorbackups.core.DailySummary;
import com.lucasemanuel.monitorbackups.core.JobSummary;

public final class MarkdownRenderer implements ReportRenderer {

    @Override
    public String render(BackupReport report) {
        var out = new StringBuilder();

        out.append("# Relatório de backups\n\n")
                .append("**Gerado em:** ").append(escape(report.generatedAt().toString())).append("  \n")
                .append("**Jobs monitorados:** ").append(report.jobs().size()).append("  \n")
                .append("**Alertas:** ").append(report.alerts().size())
                .append(" (").append(report.criticalCount()).append(" ")
                .append(plural(report.criticalCount(), "crítico", "críticos"))
                .append(", ").append(report.warningCount()).append(" de atenção)\n\n");

        out.append("## Alertas\n\n");
        if (report.alerts().isEmpty()) {
            out.append("Nenhum alerta. Todos os jobs estão dentro do prazo.\n\n");
        } else {
            out.append("| Nível | Job | Situação | Detalhe |\n")
                    .append("| --- | --- | --- | --- |\n");
            for (BackupAlert alert : report.alerts()) {
                out.append("| ").append(alert.level().label())
                        .append(" | ").append(escape(alert.job()))
                        .append(" | ").append(escape(alert.title()))
                        .append(" | ").append(escape(alert.message()))
                        .append(" |\n");
            }
            out.append('\n');
        }

        out.append("## Resumo por job\n\n")
                .append("| Job | Última execução | Último sucesso | Idade | Prazo | Nível |\n")
                .append("| --- | --- | --- | --- | --- | --- |\n");
        for (JobSummary job : report.jobs()) {
            out.append("| ").append(escape(job.job()))
                    .append(" | ").append(escape(String.valueOf(job.lastRunAt() == null ? "—" : job.lastRunAt())))
                    .append(" | ").append(escape(String.valueOf(job.lastSuccessAt() == null ? "—" : job.lastSuccessAt())))
                    .append(" | ").append(job.ageLabel())
                    .append(" | ").append(job.policy().maxAgeHours()).append("h")
                    .append(" | ").append(job.level().label())
                    .append(" |\n");
        }

        out.append("\n## Histórico por dia\n\n")
                .append("| Data | Sucesso | Falha | Ignorado |\n")
                .append("| --- | --- | --- | --- |\n");
        for (DailySummary day : report.daily()) {
            out.append("| ").append(day.date())
                    .append(" | ").append(day.success())
                    .append(" | ").append(day.failed())
                    .append(" | ").append(day.skipped())
                    .append(" |\n");
        }

        out.append("\n---\n\nRegistros e políticas fictícios, usados apenas para demonstração.\n");
        return out.toString();
    }

    static String escape(String value) {
        return value == null ? "" : value
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private static String plural(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}
