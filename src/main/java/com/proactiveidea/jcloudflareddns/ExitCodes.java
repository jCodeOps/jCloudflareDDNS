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

/** Exit codes used by the command-line application. */
public final class ExitCodes {

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int USAGE_ERROR = 2;
    public static final int NOT_IMPLEMENTED = 3;

    private ExitCodes() {
    }
}
