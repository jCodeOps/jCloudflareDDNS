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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/** Loads non-secret configuration from YAML. */
public final class ConfigurationLoader {

    private final ObjectMapper mapper;

    public ConfigurationLoader() {
        mapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Configuration load(Path path) throws ConfigurationException {
        if (path == null) {
            throw new ConfigurationException("Configuration path is required.");
        }
        if (!Files.isRegularFile(path)) {
            throw new ConfigurationException("Configuration file does not exist: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new ConfigurationException("Configuration file is not readable: " + path);
        }

        try {
            String yaml = Files.readString(path, StandardCharsets.UTF_8);
            rejectSecretKeys(yaml);
            Configuration configuration = mapper.readValue(yaml, Configuration.class);
            if (configuration == null) {
                throw new ConfigurationException("Configuration file is empty.");
            }
            return configuration;
        } catch (ConfigurationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ConfigurationException("Configuration file could not be parsed.", exception);
        }
    }

    private static void rejectSecretKeys(String yaml) throws ConfigurationException {
        String[] lines = yaml.split("\\R");
        for (String line : lines) {
            String key = line.stripLeading().split(":", 2)[0].trim();
            if (key.equalsIgnoreCase("token")
                    || key.equalsIgnoreCase("apiToken")
                    || key.equalsIgnoreCase("globalApiKey")
                    || key.equalsIgnoreCase("password")
                    || key.equalsIgnoreCase("secret")) {
                throw new ConfigurationException(
                        "Configuration must not contain secret values; use tokenEnv instead.");
            }
        }
    }
}
