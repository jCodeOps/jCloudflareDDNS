/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

class ProfileExecutorTest {

    private final ProfileExecutor executor = new ProfileExecutor();
    private final Map<String, Configuration> profiles = profiles();

    @Test
    void sequentialExecutionPreservesProfileOrder() throws ConfigurationException {
        var processed = new CopyOnWriteArrayList<String>();

        int result = executor.execute(profiles, new ExecutionConfiguration("sequential", null),
                (name, configuration) -> {
                    processed.add(name);
                    return ExitCodes.SUCCESS;
                });

        assertEquals(ExitCodes.SUCCESS, result);
        assertEquals(java.util.List.of("first", "second", "third"), processed);
    }

    @Test
    void parallelExecutionProcessesAllProfilesAndCombinesFailure() throws ConfigurationException {
        var processed = new CopyOnWriteArrayList<String>();

        int result = executor.execute(profiles, new ExecutionConfiguration("parallel", 2),
                (name, configuration) -> {
                    processed.add(name);
                    return name.equals("second") ? ExitCodes.NETWORK_ERROR : ExitCodes.SUCCESS;
                });

        assertEquals(ExitCodes.NETWORK_ERROR, result);
        assertEquals(java.util.Set.of("first", "second", "third"), java.util.Set.copyOf(processed));
    }

    @Test
    void unexpectedTaskFailureBecomesConfigurationException() {
        assertThrows(ConfigurationException.class, () -> executor.execute(
                Map.of("profile", configuration()), new ExecutionConfiguration("parallel", 1),
                (name, configuration) -> {
                    throw new IllegalStateException("test failure");
                }));
    }

    private static Configuration configuration() {
        return new Configuration("example.com", "host.example.com", 300, false,
                "CLOUDFLARE_API_TOKEN", java.util.List.of(), true, "ipv4");
    }

    private static Map<String, Configuration> profiles() {
        Map<String, Configuration> result = new LinkedHashMap<>();
        result.put("first", configuration());
        result.put("second", configuration());
        result.put("third", configuration());
        return result;
    }
}
