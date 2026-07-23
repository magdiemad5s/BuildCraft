// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VerticalTankPolicyTest {
    @Test
    void fillsLiquidsBottomUpAndDrainsThemTopDown() {
        assertArrayEquals(new int[] {0, 1, 2}, VerticalTankPolicy.traversal(3, false, true));
        assertArrayEquals(new int[] {2, 1, 0}, VerticalTankPolicy.traversal(3, false, false));
    }

    @Test
    void fillsGasesTopDownAndDrainsThemBottomUp() {
        assertArrayEquals(new int[] {2, 1, 0}, VerticalTankPolicy.traversal(3, true, true));
        assertArrayEquals(new int[] {0, 1, 2}, VerticalTankPolicy.traversal(3, true, false));
    }

    @Test
    void comparatorUsesTheLegacyOneToFifteenRange() {
        assertEquals(0, VerticalTankPolicy.comparatorLevel(0, 16_000));
        assertEquals(1, VerticalTankPolicy.comparatorLevel(1, 16_000));
        assertEquals(8, VerticalTankPolicy.comparatorLevel(8_000, 16_000));
        assertEquals(15, VerticalTankPolicy.comparatorLevel(16_000, 16_000));
        assertEquals(15, VerticalTankPolicy.comparatorLevel(Integer.MAX_VALUE, 16_000));
    }

    @Test
    void rejectsNegativeTankCounts() {
        assertThrows(IllegalArgumentException.class, () -> VerticalTankPolicy.traversal(-1, false, true));
    }
}
