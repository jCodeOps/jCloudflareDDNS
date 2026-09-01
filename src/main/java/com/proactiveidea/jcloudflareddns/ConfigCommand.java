/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Configuration-related commands. */
@Command(name = "config", description = "Create and inspect configuration files.",
        subcommands = ConfigCommand.ConfigInitCommand.class)
public final class ConfigCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.USAGE_ERROR;
    }

    @Command(name = "init", description = "Create a non-secret configuration template.")
    static final class ConfigInitCommand implements Callable<Integer> {

        @Option(names = {"-o", "--output"}, arity = "0..1", fallbackValue = "config.yml",
                description = "Output YAML path (default: config.yml).")
        private Path output = Path.of("config.yml");

        @Option(names = "--profile", defaultValue = "home", description = "Initial profile name (default: home).")
        private String profile;

        @Option(names = "--zone", defaultValue = "example.com", description = "Initial Cloudflare zone.")
        private String zone;

        @Option(names = "--record", defaultValue = "host.example.com", description = "Initial DNS record.")
        private String record;

        @Option(names = "--token-env", defaultValue = "CLOUDFLARE_API_TOKEN",
                description = "Environment variable name for the API Token.")
        private String tokenEnv;

        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            try {
                validateInput();
                Files.writeString(output, template(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                restrictPermissionsWhenSupported();
                spec.commandLine().getOut().printf("Created configuration template: %s%n", output);
                spec.commandLine().getOut().printf(
                        "Set the API Token in the environment variable '%s' before running the application.%n",
                        tokenEnv);
                return ExitCodes.SUCCESS;
            } catch (java.nio.file.FileAlreadyExistsException exception) {
                spec.commandLine().getErr().println("Configuration file already exists: " + output);
                return ExitCodes.FAILURE;
            } catch (IOException | IllegalArgumentException exception) {
                spec.commandLine().getErr().println("Configuration template could not be created: "
                        + exception.getMessage());
                return ExitCodes.FAILURE;
            }
        }

        private void validateInput() {
            requireSingleLine(profile, "profile");
            requireSingleLine(zone, "zone");
            requireSingleLine(record, "record");
            requireSingleLine(tokenEnv, "token-env");
        }

        private static void requireSingleLine(String value, String name) {
            if (value == null || value.isBlank() || value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException(name + " must be a non-empty single-line value.");
            }
        }

        private String template() {
            return """
                    execution:
                      mode: sequential
                    defaults:
                      ttl: 300
                      proxied: false
                      ipVersion: ipv4
                      useDefaultIpProviders: true
                    profiles:
                      %s:
                        zone: %s
                        record: %s
                        tokenEnv: %s
                    """.formatted(profile, zone, record, tokenEnv);
        }

        private void restrictPermissionsWhenSupported() {
            try {
                Files.setPosixFilePermissions(output, java.util.Set.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException exception) {
                // File permissions are not available on non-POSIX systems.
            } catch (IOException exception) {
                spec.commandLine().getErr().println(
                        "Warning: could not restrict configuration file permissions: " + exception.getMessage());
            }
        }
    }
}
