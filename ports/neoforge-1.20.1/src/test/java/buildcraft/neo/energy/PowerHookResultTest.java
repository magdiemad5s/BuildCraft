package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PowerHookResultTest {
    @Test
    void retainsLongPrecisionForHookAccounting() {
        long amount = (long) Integer.MAX_VALUE + 100L;
        assertEquals(amount, PowerHookResult.handled(amount).acceptedFrom(amount));
    }

    @Test
    void rejectsImpossibleHookAccounting() {
        assertThrows(IllegalArgumentException.class, () -> PowerHookResult.handled(11).acceptedFrom(10));
        assertThrows(IllegalArgumentException.class, () -> new PowerHookResult(false, 1));
    }
}
