# Monitor de backups

CLI em **Java 17** que lê logs simulados de backups e gera um relatório com **alertas**, **resumo por job** e **histórico por dia**. É outra rotina de analista de TI transformada em projeto: detectar falha, perceber backup parado e deixar tudo documentado.

> **Nenhum dado de ambiente corporativo real é usado.** Jobs, prazos, horários e detalhes são fictícios e servem apenas para demonstração.

## O que ele detecta

| Situação | Nível |
| --- | --- |
| Job crítico sem nenhum sucesso registrado | `CRÍTICO` |
| Falha depois do último sucesso em job crítico | `CRÍTICO` |
| Último sucesso mais antigo que o prazo em job crítico | `CRÍTICO` |
| Mesmas situações em job não crítico | `ATENÇÃO` |
| Sucesso dentro do prazo | `OK` (sem alerta) |

Um status `SKIPPED` **não anula** um sucesso recente, mas continua aparecendo no histórico do dia.

## Políticas fictícias

| Job | Prazo máximo sem sucesso | Criticidade |
| --- | --- | --- |
| `financeiro` | 24h | Crítico |
| `clientes` | 24h | Crítico |
| `sistema-chamados` | 48h | Não crítico |
| `inventario` | 72h | Não crítico |
| `logs-aplicacao` | 24h | Não crítico |
| qualquer outro job | 24h | Não crítico |

As políticas ficam em `PolicyCatalog`. Para testar, basta trocar um prazo ou marcar outro job como crítico.

## Formato de entrada

Sem `--input`, o programa usa o exemplo embutido `src/main/resources/example-backups.csv`:

```csv
job,executadoEm,status,duracaoMin,detalhes
financeiro,2026-11-23T02:15,SUCCESS,38,concluido
financeiro,2026-11-24T02:12,FAILED,5,timeout no destino
clientes,2026-11-24T01:30,SUCCESS,51,concluido
```

- `executadoEm` usa ISO local: `AAAA-MM-DDTHH:mm`.
- `status` aceito: `SUCCESS`, `FAILED`, `SKIPPED` (sem distinção de caixa).
- O parser tem suporte a aspas, então `detalhes` pode conter vírgulas: `"concluido, sem pendencias"`.

## Como rodar

Requisito: **JDK 17**.

```bash
./gradlew jar                       # no Windows: gradlew.bat jar

# relatório no terminal, com data de referência fixa (reproduzível)
java -jar build/libs/MonitorBackups-1.0.0.jar --now 2026-11-24T06:00

# relatório em Markdown, a partir de um CSV próprio
java -jar build/libs/MonitorBackups-1.0.0.jar \
  --input backups.csv --format markdown --output relatorio.md

# painel HTML para abrir no navegador ou salvar como PDF
java -jar build/libs/MonitorBackups-1.0.0.jar \
  --format html --output relatorio.html
```

Opções:

| Opção | Descrição |
| --- | --- |
| `--input` | CSV com os registros; padrão: exemplo embutido |
| `--now` | Data/hora de referência no formato ISO; padrão: hora atual |
| `--format` | `console` (padrão), `markdown` ou `html` |
| `--output` | Arquivo de saída em UTF-8; padrão: terminal |
| `-h`, `--help` | Mostra a ajuda |

Use `--now` para reproduzir exatamente o relatório de exemplo. No Windows, se acentos aparecerem estranhos no terminal, prefira `--output` e abra o arquivo no editor.

## Exemplo de relatório (console)

```text
RELATÓRIO DE BACKUPS — 2026-11-24T06:00
Jobs: 5 · Alertas: 3 · Críticos: 1 · Atenção: 2

ALERTAS
  [CRÍTICO] financeiro — Falha na última execução
      Última execução falhou em 2026-11-24T02:12: timeout no destino.
  [ATENÇÃO] inventario — Backup fora do prazo
      Último sucesso há 98h; prazo definido: 72h.
```

## Decisões

- **`core/` não faz I/O:** recebe uma lista de registros e devolve um relatório. Por isso a análise é testável sem arquivos e sem terminal.
- **`--now` explícito:** o relatório de demonstração é determinístico; não muda conforme o dia da execução.
- **CSV sem dependências:** um parser mínimo cobre aspas e vírgulas dentro dos detalhes, que é o suficiente para o formato publicado.
- **Escape nas saídas:** `|` não quebra tabelas Markdown e `<script>` não vira HTML executável. Isso está coberto por testes.
- **Alertas ordenados por criticidade:** o que precisa de ação aparece primeiro.

## Testes e CI

**25 testes JUnit 5** cobrem leitura de CSV, normalização de status, regras de alerta, prazos, agrupamento por dia, renderizadores e CLI.

O workflow em `.github/workflows/ci.yml` roda `./gradlew build` em cada push e PR e anexa o relatório de testes.

## Estrutura

```
src/main/java/com/lucasemanuel/monitorbackups/
├── Main.java
├── cli/Cli.java
├── core/
│   ├── AlertLevel.java       BackupAlert.java     BackupAnalyzer.java
│   ├── BackupPolicy.java     BackupRecord.java    BackupReport.java
│   ├── BackupStatus.java     CsvParser.java       DailySummary.java
│   ├── JobSummary.java       PolicyCatalog.java
└── render/
    ├── ReportRenderer.java   ConsoleRenderer.java
    ├── MarkdownRenderer.java HtmlRenderer.java
```

## Licença

[Unlicense](LICENSE) — uso livre, sem restrições.
