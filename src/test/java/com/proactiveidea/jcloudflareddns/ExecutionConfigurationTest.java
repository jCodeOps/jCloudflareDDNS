/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExecutionConfigurationTest {

    @Test
    void sequentialModeAlwaysUsesOneWorker() {
        ExecutionConfiguration execution = new ExecutionConfiguration("sequential", null);

        assertEquals(1, execution.workerCount());
        assertTrue(!execution.exceedsRecommendedConcurrency());
    }

    @Test
    void parallelModeWarnsOnlyAboveRecommendedLimit() {
        ExecutionConfiguration execution = new ExecutionConfiguration("parallel", 9);

        assertEquals(9, execution.workerCount());
        assertTrue(execution.exceedsRecommendedConcurrency());
    }
}
