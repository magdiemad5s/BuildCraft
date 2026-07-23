// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TankAmountPolicyTest {
    @Test
    void clampsMalformedAndOverCapacitySavedAmounts() {
        assertEquals(0, TankAmountPolicy.clampStoredAmount(-1, 16_000));
        assertEquals(8_000, TankAmountPolicy.clampStoredAmount(8_000, 16_000));
        assertEquals(16_000, TankAmountPolicy.clampStoredAmount(Integer.MAX_VALUE, 16_000));
    }

    @Test
    void rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> TankAmountPolicy.clampStoredAmount(1, -1));
    }
}
