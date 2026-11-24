package com.lucasemanuel.monitorbackups;

import com.lucasemanuel.monitorbackups.cli.Cli;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = Cli.run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
