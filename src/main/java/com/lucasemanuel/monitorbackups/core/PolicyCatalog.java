package com.lucasemanuel.monitorbackups.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Políticas fictícias: prazo máximo desde o último sucesso e criticidade de cada job. */
public final class PolicyCatalog {
    public static final BackupPolicy DEFAULT = new BackupPolicy("padrao", 24, false);

    private static final Map<String, BackupPolicy> POLICIES = new LinkedHashMap<>();

    static {
        add(new BackupPolicy("financeiro", 24, true));
        add(new BackupPolicy("clientes", 24, true));
        add(new BackupPolicy("sistema-chamados", 48, false));
        add(new BackupPolicy("inventario", 72, false));
        add(new BackupPolicy("logs-aplicacao", 24, false));
    }

    private static void add(BackupPolicy policy) {
        POLICIES.put(policy.job(), policy);
    }

    public List<String> jobs() {
        return List.copyOf(POLICIES.keySet());
    }

    public BackupPolicy policyFor(String job) {
        BackupPolicy found = POLICIES.get(job);
        return found == null ? new BackupPolicy(job, DEFAULT.maxAgeHours(), DEFAULT.critical()) : found;
    }
}
