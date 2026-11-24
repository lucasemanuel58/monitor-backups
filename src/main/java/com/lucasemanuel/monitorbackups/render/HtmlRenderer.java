package com.lucasemanuel.monitorbackups.render;

import com.lucasemanuel.monitorbackups.core.AlertLevel;
import com.lucasemanuel.monitorbackups.core.BackupAlert;
import com.lucasemanuel.monitorbackups.core.BackupReport;
import com.lucasemanuel.monitorbackups.core.DailySummary;
import com.lucasemanuel.monitorbackups.core.JobSummary;

public final class HtmlRenderer implements ReportRenderer {

    private static final String CSS = """
            :root { color-scheme: dark; }
            body { font-family: system-ui, sans-serif; background: #0b1220; color: #e8eef7;
                   max-width: 980px; margin: 32px auto; padding: 0 20px; line-height: 1.55; }
            h1 { font-size: 1.9rem; margin-bottom: 4px; }
            h2 { font-size: 1.15rem; margin-top: 36px; color: #7dd3fc; }
            p.meta { color: #a7b6cb; margin-top: 0; }
            table { width: 100%; border-collapse: collapse; font-size: .88rem; }
            th, td { text-align: left; padding: 9px 12px; border-bottom: 1px solid #253344; }
            th { color: #7dd3fc; font-size: .75rem; letter-spacing: .08em; text-transform: uppercase; }
            .alert { padding: 14px 16px; margin-bottom: 10px; border-radius: 12px;
                     border-left: 4px solid #64748b; background: #111d30; }
            .alert.CRITICAL { border-left-color: #f87171; }
            .alert.WARNING { border-left-color: #fbbf24; }
            .alert.OK { border-left-color: #34d399; }
            .badge { font-size: .72rem; font-weight: 700; letter-spacing: .08em; }
            .title { font-weight: 600; margin-left: 8px; }
            .message { color: #a7b6cb; font-size: .86rem; margin-top: 4px; }
            footer { border-top: 1px solid #253344; margin-top: 36px; padding-top: 14px;
                     color: #8ea1b8; font-size: .8rem; }
            @media print { body { background: #fff; color: #0f172a; } .alert { background: #f8fafc; } }
            """;

    @Override
    public String render(BackupReport report) {
        var out = new StringBuilder();

        out.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Relatório de backups</title>\n")
                .append("<style>\n").append(CSS).append("\n</style>\n")
                .append("</head>\n<body>\n");

        out.append("<h1>Relatório de backups</h1>\n")
                .append("<p class=\"meta\">Gerado em ").append(escape(report.generatedAt().toString()))
                .append(" · ").append(report.jobs().size()).append(" jobs · ")
                .append(report.alerts().size()).append(" alertas (")
                .append(report.criticalCount()).append(" críticos, ")
                .append(report.warningCount()).append(" de atenção)</p>\n");

        out.append("<h2>Alertas</h2>\n");
        if (report.alerts().isEmpty()) {
            out.append("<p>Nenhum alerta. Todos os jobs estão dentro do prazo.</p>\n");
        } else {
            for (BackupAlert alert : report.alerts()) {
                out.append("<div class=\"alert ").append(alert.level().name()).append("\">\n")
                        .append("<span class=\"badge\">").append(alert.level().label()).append("</span>")
                        .append("<span class=\"title\">").append(escape(alert.job()))
                        .append(" — ").append(escape(alert.title())).append("</span>\n")
                        .append("<div class=\"message\">").append(escape(alert.message())).append("</div>\n")
                        .append("</div>\n");
            }
        }

        out.append("<h2>Resumo por job</h2>\n")
                .append("<table><thead><tr><th>Job</th><th>Última execução</th><th>Último sucesso</th>")
                .append("<th>Idade</th><th>Prazo</th><th>Nível</th></tr></thead><tbody>\n");
        for (JobSummary job : report.jobs()) {
            out.append("<tr><td>").append(escape(job.job())).append("</td>")
                    .append("<td>").append(escape(String.valueOf(job.lastRunAt() == null ? "—" : job.lastRunAt()))).append("</td>")
                    .append("<td>").append(escape(String.valueOf(job.lastSuccessAt() == null ? "—" : job.lastSuccessAt()))).append("</td>")
                    .append("<td>").append(escape(job.ageLabel())).append("</td>")
                    .append("<td>").append(job.policy().maxAgeHours()).append("h</td>")
                    .append("<td>").append(levelClass(job.level())).append("</td></tr>\n");
        }
        out.append("</tbody></table>\n");

        out.append("<h2>Histórico por dia</h2>\n")
                .append("<table><thead><tr><th>Data</th><th>Sucesso</th><th>Falha</th><th>Ignorado</th>")
                .append("</tr></thead><tbody>\n");
        for (DailySummary day : report.daily()) {
            out.append("<tr><td>").append(day.date()).append("</td>")
                    .append("<td>").append(day.success()).append("</td>")
                    .append("<td>").append(day.failed()).append("</td>")
                    .append("<td>").append(day.skipped()).append("</td></tr>\n");
        }
        out.append("</tbody></table>\n");

        out.append("<footer>Registros e políticas fictícios, usados apenas para demonstração.</footer>\n")
                .append("</body>\n</html>\n");
        return out.toString();
    }

    private static String levelClass(AlertLevel level) {
        return "<span class=\"badge\">" + level.label() + "</span>";
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
