/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import java.util.List;

/** Nullable YAML values used before profile/default inheritance is resolved. */
record ConfigurationOverride(
        String zone,
        String record,
        Integer ttl,
        Boolean proxied,
        String tokenEnv,
        List<String> ipProviderUrls,
        Boolean useDefaultIpProviders,
        String ipVersion) {
}
