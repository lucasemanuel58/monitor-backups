package com.lucasemanuel.monitorbackups.render;

import com.lucasemanuel.monitorbackups.core.BackupReport;

public interface ReportRenderer {
    String render(BackupReport report);
}
