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

import java.io.PrintWriter;
import java.util.Arrays;

import picocli.CommandLine;

/**
 * Minimal entry point for the Stage 0 project foundation.
 */
public final class JCloudflareDdnsApplication {

    public static final String VERSION = "0.1.0-SNAPSHOT";

    private JCloudflareDdnsApplication() {
    }

    public static void main(String[] args) {
        System.exit(execute(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    /** Executes the CLI without terminating the hosting process. */
    public static int execute(String[] args, PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(new JCloudflareDdnsCommand());
        commandLine.setOut(out);
        commandLine.setErr(err);
        return commandLine.execute(Arrays.copyOf(args, args.length));
    }
}
