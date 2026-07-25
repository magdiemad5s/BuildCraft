package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PowerPipeBufferBoundsTest {
    @Test
    void activeAndStagedEnergyShareOneFiniteCapacity() {
        assertEquals(25, PowerPipeBufferBounds.acceptable(80, 40, 35, 100));
        assertEquals(55, PowerPipeBufferBounds.excess(80, 40, 35, 100));
        assertEquals(0, PowerPipeBufferBounds.acceptable(1, 40, 60, 100));
    }

    @Test
    void simulationCanUseTheSameCalculationWithoutChangingState() {
        long active = 10;
        long staged = 20;

        long simulatedExcess = PowerPipeBufferBounds.excess(90, active, staged, 100);
        long executedExcess = PowerPipeBufferBounds.excess(90, active, staged, 100);

        assertEquals(20, simulatedExcess);
        assertEquals(simulatedExcess, executedExcess);
        assertEquals(10, active);
        assertEquals(20, staged);
    }

    @Test
    void handlesLongMaximumCapacityWithoutOverflow() {
        assertEquals(
            2,
            PowerPipeBufferBounds.acceptable(
                Long.MAX_VALUE,
                Long.MAX_VALUE - 3,
                1,
                Long.MAX_VALUE
            )
        );
    }

    @Test
    void rejectsNonPositiveOrAlreadyOverCapacityOffers() {
        assertEquals(0, PowerPipeBufferBounds.acceptable(0, 0, 0, 100));
        assertEquals(-1, PowerPipeBufferBounds.excess(-1, 0, 0, 100));
        assertEquals(0, PowerPipeBufferBounds.acceptable(10, 101, 0, 100));
    }
}
