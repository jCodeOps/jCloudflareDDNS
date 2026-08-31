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

/** Raised when the configured API token cannot be supplied safely. */
public final class AuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
