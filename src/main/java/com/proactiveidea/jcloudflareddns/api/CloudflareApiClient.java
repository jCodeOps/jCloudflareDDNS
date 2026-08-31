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

import java.util.List;

/** Small boundary for the Cloudflare DNS operations needed by future stages. */
public interface CloudflareApiClient {

    Zone findZone(String name) throws CloudflareApiException, AuthenticationException;

    List<DnsRecord> listRecords(String zoneId, String name, String type)
            throws CloudflareApiException, AuthenticationException;

    DnsRecord updateRecord(String zoneId, String recordId, DnsRecordUpdate update)
            throws CloudflareApiException, AuthenticationException;
}
