/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.robotics.zone;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Security and pacing policy for Zone Planner map requests. */
public final class ZoneMapRequestPolicy {
    public static final int MAX_MAP_RADIUS_BLOCKS = 2048;
    public static final int MAX_CLIENT_PENDING_REQUESTS = 2048;
    public static final int CLIENT_REQUESTS_PER_TICK = 4;
    public static final int MAX_ZONE_CHUNKS_PER_EDIT = 2048;
    public static final int MAX_ZONE_CHUNK_BYTES = 32;
    public static final int MAX_ZONE_PACKET_BYTES = 30 * 1024;
    public static final int MAX_MENU_MUTATIONS_PER_TICK = 4;
    public static final int MAX_MAP_NAME_LENGTH = 32;
    public static final int SERVER_REQUESTS_PER_TICK = 8;
    public static final int SERVER_REQUESTS_PER_WINDOW = 80;
    public static final int SERVER_WINDOW_TICKS = 20;

    private ZoneMapRequestPolicy() {
    }

    public static String sanitizeMapName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder clean = new StringBuilder(Math.min(name.length(), MAX_MAP_NAME_LENGTH));
        name.codePoints().filter(codePoint -> !Character.isISOControl(codePoint)).limit(MAX_MAP_NAME_LENGTH)
            .forEach(clean::appendCodePoint);
        return clean.toString();
    }

    public static boolean isExpectedDimension(int requestedDimension, int expectedDimension) {
        return requestedDimension == expectedDimension;
    }

    public static boolean isValidLevel(int requestedLevel, int minBuildHeight, int maxBuildHeight) {
        if (maxBuildHeight <= minBuildHeight) {
            return false;
        }
        int maxLevel = Math.max(0, Math.floorDiv(maxBuildHeight - 1, ZonePlannerMapChunkKey.LEVEL_HEIGHT));
        return requestedLevel >= 0 && requestedLevel <= maxLevel;
    }

    public static boolean isWithinMapRadius(
            int originBlockX,
            int originBlockZ,
            int requestedChunkX,
            int requestedChunkZ) {
        long requestedCenterX = ((long) requestedChunkX << 4) + 8L;
        long requestedCenterZ = ((long) requestedChunkZ << 4) + 8L;
        return Math.abs(requestedCenterX - originBlockX) <= MAX_MAP_RADIUS_BLOCKS
            && Math.abs(requestedCenterZ - originBlockZ) <= MAX_MAP_RADIUS_BLOCKS;
    }

    public static final class RateLimiter {
        private final Map<UUID, Window> windows = new HashMap<>();

        public boolean allow(UUID playerId, long gameTick) {
            Window window = windows.computeIfAbsent(playerId, ignored -> new Window(gameTick));
            if (gameTick < window.windowStart || gameTick - window.windowStart >= SERVER_WINDOW_TICKS) {
                window.windowStart = gameTick;
                window.windowCount = 0;
            }
            if (gameTick != window.lastTick) {
                window.lastTick = gameTick;
                window.tickCount = 0;
            }
            if (window.tickCount >= SERVER_REQUESTS_PER_TICK
                    || window.windowCount >= SERVER_REQUESTS_PER_WINDOW) {
                return false;
            }
            window.tickCount++;
            window.windowCount++;
            if (windows.size() > 1024 && Math.floorMod(gameTick, SERVER_WINDOW_TICKS) == 0) {
                prune(gameTick);
            }
            return true;
        }

        public void forget(UUID playerId) {
            windows.remove(playerId);
        }

        private void prune(long gameTick) {
            Iterator<Map.Entry<UUID, Window>> iterator = windows.entrySet().iterator();
            while (iterator.hasNext()) {
                Window window = iterator.next().getValue();
                if (gameTick - window.lastTick > SERVER_WINDOW_TICKS * 2L) {
                    iterator.remove();
                }
            }
        }
    }

    private static final class Window {
        private long windowStart;
        private long lastTick;
        private int tickCount;
        private int windowCount;

        private Window(long gameTick) {
            windowStart = gameTick;
            lastTick = gameTick;
        }
    }
}
