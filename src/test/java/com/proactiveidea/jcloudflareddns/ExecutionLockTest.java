/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutionLockTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsAnOverlappingExecution() throws Exception {
        Path configuration = Files.createFile(temporaryDirectory.resolve("config.yml"));

        try (ExecutionLock executionLock = ExecutionLock.acquire(configuration)) {
            org.junit.jupiter.api.Assertions.assertTrue(executionLock.isHeld());
            assertThrows(ConfigurationException.class, () -> ExecutionLock.acquire(configuration));
        }
    }
}
