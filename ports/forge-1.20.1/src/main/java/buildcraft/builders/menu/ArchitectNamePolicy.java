/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.menu;

/** Server-authoritative validation for Architect Table blueprint names. */
public final class ArchitectNamePolicy {
    public static final int MAX_LENGTH = 32;

    private ArchitectNamePolicy() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder filtered = new StringBuilder(Math.min(value.length(), MAX_LENGTH));
        value.codePoints()
            .filter(codePoint -> !Character.isISOControl(codePoint))
            .forEach(filtered::appendCodePoint);
        String sanitized = filtered.toString().strip();
        if (sanitized.length() <= MAX_LENGTH) {
            return sanitized;
        }

        int end = MAX_LENGTH;
        if (Character.isHighSurrogate(sanitized.charAt(end - 1))) {
            end--;
        }
        return sanitized.substring(0, end);
    }
}
