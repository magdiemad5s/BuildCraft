package buildcraft.robotics.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ZoneMapRequestPolicyTest {
    @Test
    void mapNamesAreBoundedAndControlCharactersAreRemoved() {
        assertEquals("", ZoneMapRequestPolicy.sanitizeMapName(null));
        assertEquals("BuildCraft Neo", ZoneMapRequestPolicy.sanitizeMapName("Build\nCraft\r Neo"));
        assertEquals(
            "x".repeat(ZoneMapRequestPolicy.MAX_MAP_NAME_LENGTH),
            ZoneMapRequestPolicy.sanitizeMapName("x".repeat(100))
        );
    }
    @Test
    void dimensionAndHeightMustMatchTheOpenWorld() {
        assertTrue(ZoneMapRequestPolicy.isExpectedDimension(123, 123));
        assertFalse(ZoneMapRequestPolicy.isExpectedDimension(123, 124));

        assertTrue(ZoneMapRequestPolicy.isValidLevel(0, -64, 320));
        assertTrue(ZoneMapRequestPolicy.isValidLevel(9, -64, 320));
        assertFalse(ZoneMapRequestPolicy.isValidLevel(-1, -64, 320));
        assertFalse(ZoneMapRequestPolicy.isValidLevel(10, -64, 320));
        assertFalse(ZoneMapRequestPolicy.isValidLevel(0, 64, 64));
    }

    @Test
    void chunkRequestsAreBoundedAroundThePlannerWithoutOverflow() {
        assertTrue(ZoneMapRequestPolicy.isWithinMapRadius(0, 0, 127, 127));
        assertFalse(ZoneMapRequestPolicy.isWithinMapRadius(0, 0, 128, 0));
        assertFalse(ZoneMapRequestPolicy.isWithinMapRadius(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE
        ));
    }

    @Test
    void limiterCapsBurstsAndRollingWindowsButRecovers() {
        ZoneMapRequestPolicy.RateLimiter limiter = new ZoneMapRequestPolicy.RateLimiter();
        UUID player = UUID.randomUUID();

        for (int i = 0; i < ZoneMapRequestPolicy.SERVER_REQUESTS_PER_TICK; i++) {
            assertTrue(limiter.allow(player, 100));
        }
        assertFalse(limiter.allow(player, 100));
        assertTrue(limiter.allow(player, 101));

        int accepted = ZoneMapRequestPolicy.SERVER_REQUESTS_PER_TICK + 1;
        long tick = 102;
        while (accepted < ZoneMapRequestPolicy.SERVER_REQUESTS_PER_WINDOW) {
            if (limiter.allow(player, tick)) {
                accepted++;
            } else {
                tick++;
            }
        }
        assertFalse(limiter.allow(player, tick));
        assertTrue(limiter.allow(player, 120));
    }
}
