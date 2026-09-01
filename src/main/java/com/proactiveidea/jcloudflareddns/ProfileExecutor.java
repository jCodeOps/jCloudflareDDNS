/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executes named profiles using the configured sequential or bounded-parallel mode. */
public final class ProfileExecutor {

    /** Work performed for one profile. */
    @FunctionalInterface
    public interface ProfileTask {
        int run(String profileName, Configuration configuration);
    }

    /** Executes every profile and returns the first non-success exit code. */
    public int execute(Map<String, Configuration> profiles,
            ExecutionConfiguration execution, ProfileTask task) throws ConfigurationException {
        Objects.requireNonNull(profiles, "profiles");
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(task, "task");

        if (execution.mode().equals("sequential")) {
            return executeSequentially(profiles, task);
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(execution.workerCount())) {
            List<Future<Integer>> futures = new ArrayList<>(profiles.size());
            for (Map.Entry<String, Configuration> entry : profiles.entrySet()) {
                futures.add(executor.submit(() -> task.run(entry.getKey(), entry.getValue())));
            }
            return collectResults(futures);
        }
    }

    private int executeSequentially(Map<String, Configuration> profiles, ProfileTask task)
            throws ConfigurationException {
        int result = ExitCodes.SUCCESS;
        for (Map.Entry<String, Configuration> entry : profiles.entrySet()) {
            try {
                result = combine(result, task.run(entry.getKey(), entry.getValue()));
            } catch (RuntimeException exception) {
                throw new ConfigurationException("Profile execution failed unexpectedly.", exception);
            }
        }
        return result;
    }

    private int collectResults(List<Future<Integer>> futures) throws ConfigurationException {
        int result = ExitCodes.SUCCESS;
        try {
            for (Future<Integer> future : futures) {
                result = combine(result, future.get());
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConfigurationException("Profile execution was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new ConfigurationException("Profile execution failed unexpectedly.", exception.getCause());
        }
    }

    private static int combine(int current, int next) {
        return current == ExitCodes.SUCCESS ? next : current;
    }
}
