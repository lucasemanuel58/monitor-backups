package com.lucasemanuel.monitorbackups.core;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvParserTest {
    private final CsvParser parser = new CsvParser();

    @Test
    void leExemploEmbutido() throws Exception {
        String csv = new String(
                CsvParserTest.class.getResourceAsStream("/example-backups.csv").readAllBytes(),
                StandardCharsets.UTF_8
        );

        List<BackupRecord> records = parser.parse(new StringReader(csv));

        assertEquals(7, records.size());
        assertEquals("financeiro", records.get(0).job());
        assertEquals(BackupStatus.SUCCESS, records.get(0).status());
        assertEquals(38, records.get(0).durationMinutes());
    }

    @Test
    void aceitaVirgulaEDentroDeAspas() throws Exception {
        String csv = """
                job,executadoEm,status,duracaoMin,detalhes
                clientes,2026-11-24T01:30,SUCCESS,51,"concluido, sem pendencias"
                """;

        var records = parser.parse(new StringReader(csv));

        assertEquals(1, records.size());
        assertEquals("concluido, sem pendencias", records.get(0).details());
    }

    @Test
    void rejeitaCabecalhoErrado() {
        String csv = "job,data,status\nclientes,2026-11-24T01:30,SUCCESS\n";

        var exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("Cabeçalho esperado"));
    }

    @Test
    void rejeitaStatusDesconhecido() {
        String csv = """
                job,executadoEm,status,duracaoMin,detalhes
                clientes,2026-11-24T01:30,UNKNOWN,51,detalhe
                """;

        var exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("Status desconhecido"));
    }

    @Test
    void rejeitaDataInvalidaComNumeroDaLinha() {
        String csv = """
                job,executadoEm,status,duracaoMin,detalhes
                clientes,24-11-2026,SUCCESS,51,detalhe
                """;

        var exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new StringReader(csv)));

        assertTrue(exception.getMessage().contains("Linha 2"));
    }
}
