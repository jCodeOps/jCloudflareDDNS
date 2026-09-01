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

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        try {
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
}
