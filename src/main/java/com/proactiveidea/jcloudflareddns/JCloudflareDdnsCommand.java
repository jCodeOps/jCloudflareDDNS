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

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** Root command for the jCloudflareDDNS CLI. */
@Command(
        name = "jcloudflareddns",
        mixinStandardHelpOptions = true,
        versionProvider = JCloudflareDdnsVersionProvider.class,
        description = "A secure, lightweight Cloudflare Dynamic DNS client.",
        footer = {
                "Report bugs: " + JCloudflareDdnsApplication.BUG_REPORT_EMAIL,
                "Project: " + JCloudflareDdnsApplication.PROJECT_URL
        },
        subcommands = {
                CheckCommand.class,
                UpdateCommand.class,
                ValidateCommand.class,
                ConfigCommand.class
        }
)
public final class JCloudflareDdnsCommand implements Callable<Integer> {

    enum DiagnosticLevel {
        NORMAL,
        VERBOSE,
        DEBUG
    }

    @Option(names = {"-v", "--verbose"}, description = "Show safe diagnostic details on unexpected errors.")
    private boolean verbose;

    @Option(names = "--debug", description = "Show extended safe diagnostic details without secrets or stack traces.")
    private boolean debug;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.USAGE_ERROR;
    }

    DiagnosticLevel diagnosticLevel() {
        return debug ? DiagnosticLevel.DEBUG : verbose ? DiagnosticLevel.VERBOSE : DiagnosticLevel.NORMAL;
    }
}
