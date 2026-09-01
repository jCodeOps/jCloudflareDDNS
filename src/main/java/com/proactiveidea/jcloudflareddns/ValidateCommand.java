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

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Validates a local application configuration file. */
@Command(name = "validate", description = "Validate the application configuration.")
public final class ValidateCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true, description = "Path to the YAML configuration file.")
    private Path configPath;

    @Option(names = "--profile", description = "Named configuration profile to validate.")
    private String profile;

    @Option(names = "--all", description = "Validate every named profile sequentially.")
    private boolean all;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        try {
            if (all && profile != null) {
                spec.commandLine().getErr().println("Options --all and --profile cannot be used together.");
                return ExitCodes.USAGE_ERROR;
            }
            if (all) {
                warnIfHighConcurrency();
                return validateAll();
            }
            Configuration configuration = new ConfigurationLoader().load(configPath, profile);
            var errors = new ConfigurationValidator().validate(configuration);
            if (!errors.isEmpty()) {
                errors.forEach(error -> spec.commandLine().getErr().println("Error: " + error));
                return ExitCodes.VALIDATION_ERROR;
            }
            spec.commandLine().getOut().println("Configuration is valid.");
            return ExitCodes.SUCCESS;
        } catch (ConfigurationException exception) {
            spec.commandLine().getErr().println("Error: " + exception.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }
    }

    private void warnIfHighConcurrency() throws ConfigurationException {
        ExecutionConfiguration execution = new ConfigurationLoader().loadExecution(configPath);
        if (execution.exceedsRecommendedConcurrency()) {
            spec.commandLine().getErr().println(
                    "Warning: execution.maxConcurrency above 8 may increase resource usage.");
        }
    }

    private int validateAll() throws ConfigurationException {
        Map<String, Configuration> profiles = new ConfigurationLoader().loadAll(configPath);
        boolean valid = true;
        for (Map.Entry<String, Configuration> entry : profiles.entrySet()) {
            var errors = new ConfigurationValidator().validate(entry.getValue());
            if (errors.isEmpty()) {
                spec.commandLine().getOut().printf("Profile '%s' is valid.%n", entry.getKey());
            } else {
                valid = false;
                errors.forEach(error -> spec.commandLine().getErr().printf(
                        "Profile '%s': Error: %s%n", entry.getKey(), error));
            }
        }
        return valid ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
    }
}
