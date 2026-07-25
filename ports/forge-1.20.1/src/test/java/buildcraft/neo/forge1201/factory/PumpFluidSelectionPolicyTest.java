package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PumpFluidSelectionPolicyTest {
    @Test
    void lockedMatchingFluidIsAccepted() {
        assertTrue(PumpFluidSelectionPolicy.acceptsCandidate(true, true));
    }

    @Test
    void lockedMismatchedFluidIsRejected() {
        assertFalse(PumpFluidSelectionPolicy.acceptsCandidate(true, false));
    }

    @Test
    void unlockedPumpMaySelectAnyFluid() {
        assertTrue(PumpFluidSelectionPolicy.acceptsCandidate(false, false));
    }

    @Test
    void fallbackRequiresAnEmptyTankAndNoLockedFluidBelow() {
        assertTrue(PumpFluidSelectionPolicy.mayFallbackToAnyFluid(true, false));
        assertFalse(PumpFluidSelectionPolicy.mayFallbackToAnyFluid(false, false));
        assertFalse(PumpFluidSelectionPolicy.mayFallbackToAnyFluid(true, true));
        assertFalse(PumpFluidSelectionPolicy.mayFallbackToAnyFluid(false, true));
    }
}
