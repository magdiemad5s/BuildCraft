package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EngineStatePolicyTest {
    @Test
    void persistedPowerIsClampedToEngineCapacity() {
        assertEquals(0, EngineStatePolicy.clampStoredPower(-1, 40));
        assertEquals(20, EngineStatePolicy.clampStoredPower(20, 40));
        assertEquals(40, EngineStatePolicy.clampStoredPower(Long.MAX_VALUE, 40));
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EngineStatePolicy.clampStoredPower(0, -1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> EngineStatePolicy.addStoredPower(0, 1, -1)
        );
    }

    @Test
    void generatedPowerSaturatesWithoutOverflowOrNegativeMutation() {
        assertEquals(30, EngineStatePolicy.addStoredPower(20, 10, 40));
        assertEquals(40, EngineStatePolicy.addStoredPower(20, Long.MAX_VALUE, 40));
        assertEquals(Long.MAX_VALUE, EngineStatePolicy.addStoredPower(
            Long.MAX_VALUE - 5,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        ));
        assertEquals(20, EngineStatePolicy.addStoredPower(20, -10, 40));
        assertEquals(40, EngineStatePolicy.addStoredPower(50, 10, 40));
    }

    @Test
    void malformedAnimationAndHeatStateIsBounded() {
        assertEquals(20.0, EngineStatePolicy.clampHeat(Double.NaN, 20.0, 250.0));
        assertEquals(20.0, EngineStatePolicy.clampHeat(-100.0, 20.0, 250.0));
        assertEquals(250.0, EngineStatePolicy.clampHeat(500.0, 20.0, 250.0));
        assertEquals(0.0F, EngineStatePolicy.clampProgress(Float.NaN));
        assertEquals(0.0F, EngineStatePolicy.clampProgress(-1.0F));
        assertEquals(1.0F, EngineStatePolicy.clampProgress(2.0F));
        assertEquals(0, EngineStatePolicy.clampProgressPart(-1));
        assertEquals(2, EngineStatePolicy.clampProgressPart(3));
    }
}
