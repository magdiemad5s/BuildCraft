package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TankAmountPolicyTest {
    @Test
    void clampsMalformedAndOversizedSavedAmounts() {
        assertEquals(0, TankAmountPolicy.clampStoredAmount(-1, 16_000));
        assertEquals(0, TankAmountPolicy.clampStoredAmount(0, 16_000));
        assertEquals(4_000, TankAmountPolicy.clampStoredAmount(4_000, 16_000));
        assertEquals(16_000, TankAmountPolicy.clampStoredAmount(99_999, 16_000));
    }

    @Test
    void rejectsAnInvalidTankCapacity() {
        assertThrows(IllegalArgumentException.class, () -> TankAmountPolicy.clampStoredAmount(1, -1));
    }
}
