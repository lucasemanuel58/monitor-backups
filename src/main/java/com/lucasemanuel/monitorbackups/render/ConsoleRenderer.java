package com.lucasemanuel.monitorbackups.render;

import com.lucasemanuel.monitorbackups.core.BackupAlert;
import com.lucasemanuel.monitorbackups.core.BackupReport;
import com.lucasemanuel.monitorbackups.core.DailySummary;
import com.lucasemanuel.monitorbackups.core.JobSummary;

public final class ConsoleRenderer implements ReportRenderer {

    @Override
    public String render(BackupReport report) {
        var out = new StringBuilder();

        out.append("RELATÓRIO DE BACKUPS — ")
                .append(report.generatedAt())
                .append('\n');
        out.append("Jobs: ").append(report.jobs().size())
                .append(" · Alertas: ").append(report.alerts().size())
                .append(" · Críticos: ").append(report.criticalCount())
                .append(" · Atenção: ").append(report.warningCount())
                .append("\n\n");

        out.append("ALERTAS\n");
        if (report.alerts().isEmpty()) {
            out.append("  Nenhum alerta. Todos os jobs estão dentro do prazo.\n");
        } else {
            for (BackupAlert alert : report.alerts()) {
                out.append("  [").append(alert.level().label()).append("] ")
                        .append(alert.job()).append(" — ").append(alert.title()).append('\n')
                        .append("      ").append(alert.message()).append('\n');
            }
        }

        out.append("\nRESUMO POR JOB\n");
        out.append(row("JOB", "ÚLTIMA EXEC.", "ÚLT. SUCESSO", "IDADE", "NÍVEL"))
                .append('\n');
        for (JobSummary job : report.jobs()) {
            out.append(row(
                    job.job(),
                    String.valueOf(job.lastRunAt() == null ? "—" : job.lastRunAt()),
                    String.valueOf(job.lastSuccessAt() == null ? "—" : job.lastSuccessAt()),
                    job.ageLabel(),
                    job.level().label()
            )).append('\n');
        }

        out.append("\nHISTÓRICO POR DIA (mais recente primeiro)\n");
        for (DailySummary day : report.daily()) {
            out.append("  ").append(day.date())
                    .append("  sucesso=").append(day.success())
                    .append("  falha=").append(day.failed())
                    .append("  ignorado=").append(day.skipped())
                    .append('\n');
        }

        out.append("\nDados fictícios, usados apenas para demonstração.\n");
        return out.toString();
    }

    private static String row(String job, String lastRun, String lastSuccess, String age, String level) {
        return pad(job, 18) + pad(lastRun, 20) + pad(lastSuccess, 20) + pad(age, 8) + level;
    }

    private static String pad(String value, int width) {
        String text = value == null ? "" : value;
        if (text.length() >= width) {
            return text.substring(0, width - 1) + " ";
        }
        return text + " ".repeat(width - text.length());
    }
}
