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

import picocli.CommandLine.IVersionProvider;

/** Provides version and public support information for the CLI. */
public final class JCloudflareDdnsVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        return new String[] {
                "jCloudflareDDNS " + JCloudflareDdnsApplication.VERSION,
                "Apache License 2.0",
                "Report bugs: " + JCloudflareDdnsApplication.BUG_REPORT_EMAIL,
                "Project: " + JCloudflareDdnsApplication.PROJECT_URL
        };
    }
}
