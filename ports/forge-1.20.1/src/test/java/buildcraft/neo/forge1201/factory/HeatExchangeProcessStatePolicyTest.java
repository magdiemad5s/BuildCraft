package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeatExchangeProcessStatePolicyTest {
    @Test
    void everyReleasedProcessStateRoundTripsWithinVanillaMenuDataRange() {
        for (int state = 0; state < HeatExchangeProcessStatePolicy.STATE_COUNT; state++) {
            for (int progress : new int[] {0, 60, 120}) {
                int packed = HeatExchangeProcessStatePolicy.pack(state, progress);
                assertTrue(packed <= Short.MAX_VALUE);
                assertEquals(state, HeatExchangeProcessStatePolicy.stateOrdinal(packed));
                assertEquals(progress, HeatExchangeProcessStatePolicy.progress(packed));
            }
        }
    }

    @Test
    void onlyTheOffStateIsInactive() {
        assertFalse(HeatExchangeProcessStatePolicy.isActive(HeatExchangeProcessStatePolicy.pack(0, 0)));
        assertTrue(HeatExchangeProcessStatePolicy.isActive(HeatExchangeProcessStatePolicy.pack(1, 1)));
        assertTrue(HeatExchangeProcessStatePolicy.isActive(HeatExchangeProcessStatePolicy.pack(2, 120)));
        assertTrue(HeatExchangeProcessStatePolicy.isActive(HeatExchangeProcessStatePolicy.pack(3, 1)));
    }

    @Test
    void malformedValuesAreClampedOrRejectedSafely() {
        assertEquals(0, HeatExchangeProcessStatePolicy.progress(
            HeatExchangeProcessStatePolicy.pack(1, -1)
        ));
        assertEquals(120, HeatExchangeProcessStatePolicy.progress(
            HeatExchangeProcessStatePolicy.pack(1, 121)
        ));
        assertEquals(0, HeatExchangeProcessStatePolicy.stateOrdinal(-1));
        assertEquals(3, HeatExchangeProcessStatePolicy.stateOrdinal(Integer.MAX_VALUE));
        assertThrows(
            IllegalArgumentException.class,
            () -> HeatExchangeProcessStatePolicy.pack(-1, 0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> HeatExchangeProcessStatePolicy.pack(4, 0)
        );
    }
}
