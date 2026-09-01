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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import picocli.CommandLine.Command;

class JCloudflareDdnsApplicationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void helpReturnsSuccessAndListsCommands() {
        CliResult result = execute("--help");

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains("check"));
        assertTrue(result.stdout().contains("update"));
        assertTrue(result.stdout().contains("validate"));
    }

    @Test
    void versionReturnsSuccess() {
        CliResult result = execute("--version");

        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(result.stdout().contains(JCloudflareDdnsApplication.VERSION));
    }

    @Test
    void checkRequiresAConfigurationPath() {
        CliResult result = execute("check");

        assertEquals(ExitCodes.USAGE_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Missing required option"));
    }

    @Test
    void updateRequiresAConfigurationPath() {
        CliResult result = execute("update");

        assertEquals(ExitCodes.USAGE_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Missing required option"));
    }

    @Test
    void validateRequiresAConfigurationPath() {
        CliResult result = execute("validate");

        assertEquals(ExitCodes.USAGE_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Missing required option"));
    }

    @Test
    void invalidOptionsReturnUsageError() {
        CliResult result = execute("--unknown");

        assertEquals(ExitCodes.USAGE_ERROR, result.exitCode());
        assertTrue(result.stderr().contains("Unknown option"));
    }

    @Test
    void configInitCreatesANonSecretTemplate() throws Exception {
        Path output = temporaryDirectory.resolve("config.yml");

        CliResult result = execute("config", "init", "--output", output.toString(),
                "--profile", "home", "--zone", "example.com", "--record", "home.example.com");

        String config = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(ExitCodes.SUCCESS, result.exitCode());
        assertTrue(config.contains("profiles:"));
        assertTrue(config.contains("tokenEnv: CLOUDFLARE_API_TOKEN"));
        assertTrue(!config.contains("apiToken:"));
    }

    @Test
    void configInitRejectsUnsafeYamlInputWithoutCreatingAFile() {
        Path output = temporaryDirectory.resolve("config.yml");

        CliResult result = execute("config", "init", "--output", output.toString(),
                "--profile", "home: injected");

        assertEquals(ExitCodes.FAILURE, result.exitCode());
        assertTrue(result.stderr().contains("profile must contain only"));
        assertTrue(Files.notExists(output));
    }

    @Test
    void unexpectedCommandFailureReturnsAControlledErrorWithoutItsMessage() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CommandLine commandLine = JCloudflareDdnsApplication.createCommandLine(
                new FailingCommand(),
                new PrintWriter(stdout, true), new PrintWriter(stderr, true));

        int exitCode = commandLine.execute();

        String error = stderr.toString(StandardCharsets.UTF_8);
        assertEquals(ExitCodes.FAILURE, exitCode);
        assertTrue(error.contains("Unexpected application error (IllegalStateException)."));
        assertTrue(!error.contains("sensitive diagnostic value"));
    }

    private static CliResult execute(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = JCloudflareDdnsApplication.execute(
                args,
                new PrintWriter(stdout, true),
                new PrintWriter(stderr, true));
        return new CliResult(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private record CliResult(int exitCode, String stdout, String stderr) {
    }

    @Command(name = "failing")
    static final class FailingCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            throw new IllegalStateException("sensitive diagnostic value");
        }
    }
}
