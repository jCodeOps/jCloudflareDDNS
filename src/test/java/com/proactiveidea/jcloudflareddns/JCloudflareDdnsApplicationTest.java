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

import org.junit.jupiter.api.Test;

class JCloudflareDdnsApplicationTest {

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
    void stageOneCommandsAreRecognizedButNotImplemented() {
        for (String command : new String[]{"check", "update"}) {
            CliResult result = execute(command);

            assertEquals(ExitCodes.NOT_IMPLEMENTED, result.exitCode(), command);
            assertTrue(result.stderr().contains("not implemented"), command);
        }
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
}
