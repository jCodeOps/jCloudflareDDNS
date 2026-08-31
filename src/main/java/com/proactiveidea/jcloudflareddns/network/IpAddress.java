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

package com.proactiveidea.jcloudflareddns.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A validated and normalized IP address without DNS resolution. */
public record IpAddress(IpVersion version, String value) {

    public IpAddress(String value) {
        this(IpVersion.IPV4, value);
    }

    public IpAddress {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(value, "value");
        value = normalize(value.trim(), version);
    }

    public static IpAddress parse(String value) {
        return new IpAddress(value);
    }

    public static IpAddress parse(String value, IpVersion version) {
        return new IpAddress(version, value);
    }

    private static String normalize(String value, IpVersion version) {
        return switch (version) {
            case IPV4 -> normalizeIpv4(value);
            case IPV6 -> normalizeIpv6(value);
        };
    }

    private static String normalizeIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address.");
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3
                    || (part.length() > 1 && part.charAt(0) == '0')) {
                throw new IllegalArgumentException("Invalid IPv4 address.");
            }
            try {
                if (Integer.parseInt(part) > 255) {
                    throw new IllegalArgumentException("Invalid IPv4 address.");
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid IPv4 address.", exception);
            }
        }
        return value;
    }

    private static String normalizeIpv6(String value) {
        if (value.isEmpty() || value.contains("%") || value.contains(".")) {
            throw new IllegalArgumentException("Invalid IPv6 address.");
        }
        int compression = value.indexOf("::");
        if (compression != value.lastIndexOf("::")) {
            throw new IllegalArgumentException("Invalid IPv6 address.");
        }
        boolean compressed = compression >= 0;
        String left = compressed ? value.substring(0, compression) : value;
        String right = compressed ? value.substring(compression + 2) : "";
        List<Integer> groups = new ArrayList<>();
        addGroups(left, groups);
        int leftCount = groups.size();
        List<Integer> rightGroups = new ArrayList<>();
        addGroups(right, rightGroups);
        int missing = 8 - leftCount - rightGroups.size();
        if ((!compressed && missing != 0) || (compressed && missing < 1)) {
            throw new IllegalArgumentException("Invalid IPv6 address.");
        }
        groups.addAll(java.util.Collections.nCopies(missing, 0));
        groups.addAll(rightGroups);
        return canonicalIpv6(groups);
    }

    private static void addGroups(String part, List<Integer> groups) {
        if (part.isEmpty()) {
            return;
        }
        for (String group : part.split(":", -1)) {
            if (group.isEmpty() || group.length() > 4
                    || !group.chars().allMatch(IpAddress::isHexDigit)) {
                throw new IllegalArgumentException("Invalid IPv6 address.");
            }
            groups.add(Integer.parseInt(group, 16));
        }
    }

    private static boolean isHexDigit(int value) {
        return value >= '0' && value <= '9'
                || value >= 'a' && value <= 'f'
                || value >= 'A' && value <= 'F';
    }

    private static String canonicalIpv6(List<Integer> groups) {
        int bestStart = -1;
        int bestLength = 0;
        for (int i = 0; i < groups.size();) {
            if (groups.get(i) != 0) {
                i++;
                continue;
            }
            int end = i;
            while (end < groups.size() && groups.get(end) == 0) {
                end++;
            }
            if (end - i > bestLength && end - i >= 2) {
                bestStart = i;
                bestLength = end - i;
            }
            i = end;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            if (i == bestStart) {
                result.append("::");
                i += bestLength - 1;
            } else {
                if (i > 0 && i != bestStart + bestLength) {
                    result.append(':');
                }
                result.append(Integer.toHexString(groups.get(i)));
            }
        }
        return result.toString();
    }
}
