/*
 * Copyright 2026 Proactive Idea
 * Author: Jenny Cabrera Varona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.proactiveidea.jcloudflareddns;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Prevents overlapping executions that use the same configuration file. */
public final class ExecutionLock implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;

    private ExecutionLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static ExecutionLock acquire(Path configurationPath) throws ConfigurationException {
        if (configurationPath == null) {
            throw new ConfigurationException("Configuration path is required.");
        }
        Path lockPath = configurationPath.toAbsolutePath().normalize()
                .resolveSibling(configurationPath.getFileName() + ".jcloudflareddns.lock");
        try {
            FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException exception) {
                channel.close();
                throw new ConfigurationException("Another jCloudflareDDNS execution is already running.");
            }
            if (lock == null) {
                channel.close();
                throw new ConfigurationException("Another jCloudflareDDNS execution is already running.");
            }
            return new ExecutionLock(channel, lock);
        } catch (ConfigurationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ConfigurationException("Could not acquire the execution lock.", exception);
        }
    }

    public boolean isHeld() {
        return lock.isValid();
    }

    @Override
    public void close() throws ConfigurationException {
        try {
            lock.release();
            channel.close();
        } catch (IOException exception) {
            throw new ConfigurationException("Could not release the execution lock.", exception);
        }
    }
}
