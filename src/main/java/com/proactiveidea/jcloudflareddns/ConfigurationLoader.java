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
import java.util.Iterator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
            JsonNode document = mapper.readTree(yaml);
            rejectSecretKeys(document);
            if (document == null || document.isNull()) {
                throw new ConfigurationException("Configuration file is empty.");
            }
            return mapper.treeToValue(document, Configuration.class);
        } catch (ConfigurationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ConfigurationException("Configuration file could not be parsed.", exception);
        }
    }

    private static void rejectSecretKeys(JsonNode node) throws ConfigurationException {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (isSecretKey(field)) {
                    throw new ConfigurationException(
                            "Configuration must not contain secret values; use tokenEnv instead.");
                }
                rejectSecretKeys(node.get(field));
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                rejectSecretKeys(item);
            }
        }
    }

    private static boolean isSecretKey(String key) {
        return key.equalsIgnoreCase("token")
                || key.equalsIgnoreCase("apiToken")
                || key.equalsIgnoreCase("globalApiKey")
                || key.equalsIgnoreCase("password")
                || key.equalsIgnoreCase("secret");
    }
}
