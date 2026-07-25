package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AutoWorkbenchStatePolicyTest {
    @Test
    void persistedProgressIsBoundedAcrossReloads() {
        assertEquals(0, AutoWorkbenchStatePolicy.clampStoredPower(-1, 40));
        assertEquals(20, AutoWorkbenchStatePolicy.clampStoredPower(20, 40));
        assertEquals(40, AutoWorkbenchStatePolicy.clampStoredPower(Long.MAX_VALUE, 40));
    }

    @Test
    void receiveAccountingCannotOverfillOrAcceptNegativePower() {
        assertEquals(30, AutoWorkbenchStatePolicy.requestedPower(10, 40));
        assertEquals(30, AutoWorkbenchStatePolicy.acceptedPower(10, 100, 40));
        assertEquals(5, AutoWorkbenchStatePolicy.acceptedPower(10, 5, 40));
        assertEquals(0, AutoWorkbenchStatePolicy.acceptedPower(10, -5, 40));
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AutoWorkbenchStatePolicy.clampStoredPower(0, -1)
        );
    }
}