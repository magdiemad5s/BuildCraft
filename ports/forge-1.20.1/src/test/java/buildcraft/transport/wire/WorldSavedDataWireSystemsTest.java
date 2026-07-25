package buildcraft.transport.wire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldSavedDataWireSystemsTest {
    @Test
    void gateChangesAreConsumedExactlyOnce() {
        WorldSavedDataWireSystems data = new WorldSavedDataWireSystems();

        assertTrue(data.consumeGatesChanged(), "new systems need one initial evaluation");
        assertFalse(data.gatesChanged);
        assertFalse(data.consumeGatesChanged(), "unchanged gates must not recalculate every tick");

        data.gatesChanged = true;
        assertTrue(data.consumeGatesChanged());
        assertFalse(data.consumeGatesChanged());
    }

    @Test
    void structuralChangesRequestOneGateEvaluation() {
        WorldSavedDataWireSystems data = new WorldSavedDataWireSystems();
        data.consumeGatesChanged();

        data.markStructureChanged();

        assertTrue(data.consumeGatesChanged());
        assertFalse(data.consumeGatesChanged());
    }

    @Test
    void fullSnapshotsAreSentEvenWhenTheLastWireWasRemoved() {
        assertTrue(WorldSavedDataWireSystems.shouldSendSnapshot(true, false));
        assertTrue(WorldSavedDataWireSystems.shouldSendSnapshot(true, true));
        assertTrue(WorldSavedDataWireSystems.shouldSendSnapshot(false, true));
        assertFalse(WorldSavedDataWireSystems.shouldSendSnapshot(false, false));
    }
}
