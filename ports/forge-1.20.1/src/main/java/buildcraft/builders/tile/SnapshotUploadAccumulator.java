/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.List;

final class SnapshotUploadAccumulator {
    static final int MAX_PART_BYTES = 4 * 1024;
    static final int MAX_TOTAL_BYTES = 16 * 1024 * 1024;
    static final int MAX_PARTS = MAX_TOTAL_BYTES / MAX_PART_BYTES;
    static final long SESSION_TIMEOUT_TICKS = 20L * 30L;

    private final List<byte[]> parts = new ArrayList<>();
    private int totalBytes;
    private long lastActivityTick;

    boolean append(byte[] part, long currentTick) {
        if (part == null
            || part.length > MAX_PART_BYTES
            || parts.size() >= MAX_PARTS
            || totalBytes > MAX_TOTAL_BYTES - part.length) {
            return false;
        }
        parts.add(part);
        totalBytes += part.length;
        lastActivityTick = currentTick;
        return true;
    }

    boolean isExpired(long currentTick) {
        return currentTick - lastActivityTick > SESSION_TIMEOUT_TICKS;
    }

    byte[] finish() {
        byte[] combined = new byte[totalBytes];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, combined, offset, part.length);
            offset += part.length;
        }
        return combined;
    }

    int getTotalBytes() {
        return totalBytes;
    }

    int getPartCount() {
        return parts.size();
    }
}
