package com.lucasemanuel.monitorbackups.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Lê logs simulados no formato CSV. Não há dependência de biblioteca externa. */
public final class CsvParser {
    private static final List<String> HEADER =
            List.of("job", "executadoEm", "status", "duracaoMin", "detalhes");

    public List<BackupRecord> parse(Reader reader) throws IOException {
        var buffered = reader instanceof BufferedReader
                ? (BufferedReader) reader
                : new BufferedReader(reader);

        String firstLine = buffered.readLine();
        if (firstLine == null) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        List<String> header = parseLine(stripBom(firstLine)).stream()
                .map(String::trim)
                .toList();
        if (!HEADER.equals(header)) {
            throw new IllegalArgumentException(
                    "Cabeçalho esperado: " + String.join(",", HEADER)
            );
        }

        List<BackupRecord> records = new ArrayList<>();
        String line;
        int lineNumber = 1;
        while ((line = buffered.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            List<String> cells = parseLine(line);
            if (cells.size() != HEADER.size()) {
                throw new IllegalArgumentException(
                        "Linha " + lineNumber + ": esperava " + HEADER.size()
                                + " colunas, encontrou " + cells.size()
                );
            }

            String job = cells.get(0).trim();
            LocalDateTime executedAt;
            try {
                executedAt = LocalDateTime.parse(cells.get(1).trim());
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "Linha " + lineNumber + ": data inválida '" + cells.get(1) + "'"
                );
            }

            BackupStatus status = BackupStatus.parse(cells.get(2));
            int durationMinutes;
            try {
                durationMinutes = Integer.parseInt(cells.get(3).trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Linha " + lineNumber + ": duração inválida '" + cells.get(3) + "'"
                );
            }

            records.add(new BackupRecord(job, executedAt, status, durationMinutes, cells.get(4).trim()));
        }
        return records;
    }

    /** Parser mínimo com suporte a aspas e vírgula dentro do texto. */
    static List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(current);
                }
            } else if (current == '"' && cell.isEmpty()) {
                quoted = true;
            } else if (current == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(current);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("Aspa não fechada: " + line);
        }
        cells.add(cell.toString());
        return cells;
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
