package com.lucasemanuel.monitorbackups.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliTest {
    @TempDir
    Path temporario;

    @Test
    void helpRetornaZeroEExibeUso() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{"--help"}, print(out), print(err));

        assertEquals(0, code);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Uso:"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("--format"));
    }

    @Test
    void opcaoDesconhecidaRetornaErro() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{"--deletar"}, print(out), print(err));

        assertEquals(2, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Opção desconhecida"));
    }

    @Test
    void formatoInvalidoRetornaErro() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{"--format", "pdf"}, print(out), print(err));

        assertEquals(2, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Formato inválido"));
    }

    @Test
    void dataDeReferenciaInvalidaRetornaErro() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{"--now", "24-11-2026"}, print(out), print(err));

        assertEquals(2, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Data inválida"));
    }

    @Test
    void exemploEmbutidoImprimeRelatorioNoTerminal() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{
                "--now", "2026-11-24T06:00"
        }, print(out), print(err));

        assertEquals(0, code);
        String texto = out.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("RELATÓRIO DE BACKUPS"));
        assertTrue(texto.contains("[CRÍTICO] financeiro"));
        assertTrue(texto.contains("HISTÓRICO POR DIA"));
    }

    @Test
    void gravaHtmlEmArquivo() throws Exception {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        Path entrada = temporario.resolve("backups.csv");
        Path saida = temporario.resolve("relatorio.html");
        Files.writeString(entrada, """
                job,executadoEm,status,duracaoMin,detalhes
                financeiro,2026-11-23T02:15,SUCCESS,38,concluido
                financeiro,2026-11-24T02:12,FAILED,5,"<script>alert(""x"")</script>"
                """, StandardCharsets.UTF_8);

        int code = Cli.run(new String[]{
                "--input", entrada.toString(),
                "--now", "2026-11-24T06:00",
                "--format", "html",
                "--output", saida.toString()
        }, print(out), print(err));

        assertEquals(0, code);
        String html = Files.readString(saida, StandardCharsets.UTF_8);
        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("class=\"alert CRITICAL\""));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Relatório gravado em"));
    }

    @Test
    void arquivoInexistenteRetornaErro() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Cli.run(new String[]{
                "--input", temporario.resolve("nao-existe.csv").toString()
        }, print(out), print(err));

        assertEquals(1, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Não foi possível ler"));
    }

    private static PrintStream print(ByteArrayOutputStream stream) {
        return new PrintStream(stream, true, StandardCharsets.UTF_8);
    }
}
