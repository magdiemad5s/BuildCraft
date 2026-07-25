package buildcraft.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TankManagerCapabilityTest {
    @Test
    void acceptsEveryRealTankIndex() {
        assertTrue(TankManagerIndexPolicy.contains(0, 2));
        assertTrue(TankManagerIndexPolicy.contains(1, 2));
    }

    @Test
    void rejectsNegativeAndPastEndTankIndexes() {
        assertFalse(TankManagerIndexPolicy.contains(-1, 2));
        assertFalse(TankManagerIndexPolicy.contains(2, 2));
        assertFalse(TankManagerIndexPolicy.contains(0, 0));
    }
}