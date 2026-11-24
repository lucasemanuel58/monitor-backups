package com.lucasemanuel.monitorbackups.cli;

import com.lucasemanuel.monitorbackups.core.BackupAnalyzer;
import com.lucasemanuel.monitorbackups.core.BackupRecord;
import com.lucasemanuel.monitorbackups.core.BackupReport;
import com.lucasemanuel.monitorbackups.core.CsvParser;
import com.lucasemanuel.monitorbackups.render.ConsoleRenderer;
import com.lucasemanuel.monitorbackups.render.HtmlRenderer;
import com.lucasemanuel.monitorbackups.render.MarkdownRenderer;
import com.lucasemanuel.monitorbackups.render.ReportRenderer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Cli {
    public static final String USAGE = """
            Uso:
              monitor-backups [opções]

            Opções:
              --input     Arquivo CSV com os registros (opcional; padrão: exemplo embutido)
              --now       Data/hora de referência no formato ISO (ex.: 2026-11-24T06:00)
              --format    console (padrão), markdown ou html
              --output    Arquivo de saída (opcional; padrão: terminal)
              -h, --help  Mostra esta ajuda

            O CSV deve ter o cabeçalho:
              job,executadoEm,status,duracaoMin,detalhes

            Status aceitos: SUCCESS, FAILED, SKIPPED.
            Todos os jobs, prazos e registros são fictícios.
            """;

    private static final Set<String> OPTIONS = Set.of("--input", "--now", "--format", "--output");
    private static final Set<String> FORMATS = Set.of("console", "markdown", "html");

    private Cli() {
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || isHelp(args[0])) {
            out.print(USAGE);
            return 0;
        }

        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (isHelp(option)) {
                out.print(USAGE);
                return 0;
            }
            if (!OPTIONS.contains(option)) {
                err.println("Opção desconhecida: " + option);
                return 2;
            }
            if (i + 1 >= args.length) {
                err.println("Falta valor para " + option);
                return 2;
            }
            values.put(option, args[++i]);
        }

        String format = values.getOrDefault("--format", "console");
        if (!FORMATS.contains(format)) {
            err.println("Formato inválido: " + format + ". Use console, markdown ou html.");
            return 2;
        }

        LocalDateTime referenceTime = LocalDateTime.now();
        if (values.containsKey("--now")) {
            try {
                referenceTime = LocalDateTime.parse(values.get("--now"));
            } catch (DateTimeParseException exception) {
                err.println("Data inválida em --now: " + values.get("--now"));
                return 2;
            }
        }

        List<BackupRecord> records;
        try {
            records = readRecords(values.get("--input"));
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Não foi possível ler os registros: " + exception.getMessage());
            return 1;
        }

        BackupReport report = new BackupAnalyzer(referenceTime).analyze(records);
        ReportRenderer renderer = switch (format) {
            case "markdown" -> new MarkdownRenderer();
            case "html" -> new HtmlRenderer();
            default -> new ConsoleRenderer();
        };
        String content = renderer.render(report);

        String output = values.get("--output");
        if (output == null) {
            out.println(content);
            return 0;
        }

        try {
            Files.writeString(Path.of(output), content, StandardCharsets.UTF_8);
            out.println("Relatório gravado em " + output);
            return 0;
        } catch (IOException | RuntimeException exception) {
            err.println("Não foi possível gravar " + output + ": " + exception.getMessage());
            return 1;
        }
    }

    private static List<BackupRecord> readRecords(String input)
            throws IOException {
        var parser = new CsvParser();
        if (input == null) {
            var stream = Cli.class.getResourceAsStream("/example-backups.csv");
            if (stream == null) {
                throw new IOException("Exemplo embutido não encontrado");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parser.parse(reader);
            }
        }
        try (Reader reader = Files.newBufferedReader(Path.of(input), StandardCharsets.UTF_8)) {
            return parser.parse(reader);
        }
    }

    private static boolean isHelp(String value) {
        return value.equals("-h") || value.equals("--help");
    }
}
