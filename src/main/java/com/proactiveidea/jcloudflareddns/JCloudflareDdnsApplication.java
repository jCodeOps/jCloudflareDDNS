/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.proactiveidea.jcloudflareddns;

import java.io.PrintWriter;
import java.util.Arrays;

import picocli.CommandLine;

/**
 * Entry point for the jCloudflareDDNS command-line application.
 */
public final class JCloudflareDdnsApplication {

    public static final String VERSION = "0.1.1-SNAPSHOT";
    public static final String BUG_REPORT_EMAIL = "jcabrerav@proactiveidea.com";
    public static final String PROJECT_URL = "https://github.com/jCodeOps/jCloudflareDDNS";

    private JCloudflareDdnsApplication() {
    }

    public static void main(String[] args) {
        System.exit(execute(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    /** Executes the CLI without terminating the hosting process. */
    public static int execute(String[] args, PrintWriter out, PrintWriter err) {
        return createCommandLine(new JCloudflareDdnsCommand(), out, err)
                .execute(Arrays.copyOf(args, args.length));
    }

    static CommandLine createCommandLine(Object command, PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.setOut(out);
        commandLine.setErr(err);
        commandLine.setExecutionExceptionHandler((exception, line, parseResult) -> {
            CommandLine rootLine = line.getCommandSpec().root().commandLine();
            rootLine.getErr().printf("Unexpected application error. Re-run with --verbose for safe diagnostic "
                            + "details or report it to %s.%n", BUG_REPORT_EMAIL);
            writeSafeDiagnostics(exception, rootLine);
            return ExitCodes.FAILURE;
        });
        return commandLine;
    }

    private static void writeSafeDiagnostics(Throwable exception, CommandLine line) {
        JCloudflareDdnsCommand.DiagnosticLevel level = diagnosticLevel(line);
        if (level == JCloudflareDdnsCommand.DiagnosticLevel.NORMAL) {
            return;
        }
        line.getErr().printf("Diagnostic: exception=%s%n", exception.getClass().getSimpleName());
        if (level == JCloudflareDdnsCommand.DiagnosticLevel.DEBUG && exception.getCause() != null) {
            line.getErr().printf("Diagnostic: cause=%s%n",
                    exception.getCause().getClass().getSimpleName());
        }
    }

    private static JCloudflareDdnsCommand.DiagnosticLevel diagnosticLevel(CommandLine line) {
        Object rootCommand = line.getCommandSpec().root().userObject();
        if (rootCommand instanceof JCloudflareDdnsCommand command) {
            return command.diagnosticLevel();
        }
        return JCloudflareDdnsCommand.DiagnosticLevel.NORMAL;
    }
}
