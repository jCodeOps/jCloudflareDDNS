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

package com.proactiveidea.jcloudflareddns.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal Cloudflare DNS record representation. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DnsRecord(
        String id,
        String name,
        String type,
        String content,
        int ttl,
        boolean proxied) {
}
