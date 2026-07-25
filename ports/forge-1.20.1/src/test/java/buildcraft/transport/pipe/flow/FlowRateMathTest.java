package buildcraft.transport.pipe.flow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlowRateMathTest {
    @Test
    void emptyOrDisabledFlowHasFiniteZeroShare() {
        assertEquals(0, FlowRateMath.equalShareFactor(20, 0, 100));
        assertEquals(0, FlowRateMath.equalShareFactor(0, 2, 100));
        assertEquals(0, FlowRateMath.equalShareFactor(20, 2, 0));
        assertTrue(Double.isFinite(FlowRateMath.equalShareFactor(20, 0, 100)));
    }

    @Test
    void shareUsesOnlyEnabledParticipantsAndNeverExceedsCapacity() {
        assertEquals(1.0, FlowRateMath.equalShareFactor(20, 2, 100));
        assertEquals(0.5, FlowRateMath.equalShareFactor(20, 2, 20));
        assertEquals(
            1.0 / 6.0,
            FlowRateMath.equalShareFactor(Integer.MAX_VALUE, 6, Integer.MAX_VALUE),
            1.0e-12
        );
    }

    @Test
    void eventOffersAreClampedBeforeDistribution() {
        int[] offers = {-1, 0, 4, 50};
        int[] maximums = {8, 8, 3, 12};
        assertEquals(2, FlowRateMath.clampOffersAndCountPositive(offers, maximums));
        assertArrayEquals(new int[] {0, 0, 3, 12}, offers);
        assertThrows(
            IllegalArgumentException.class,
            () -> FlowRateMath.clampOffersAndCountPositive(new int[1], new int[2])
        );
    }
}
