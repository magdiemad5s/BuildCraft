package buildcraft.neo.neoforge1201.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

class NeoForgeEnergyStorageAdapterTest {
    @Test
    void delegatesSimulationAndExecutionWithoutChangingTheAdapterContract() {
        RecordingStorage storage = new RecordingStorage(12);
        NeoForgeEnergyStorageAdapter adapter = new NeoForgeEnergyStorageAdapter(storage);

        assertEquals(7, adapter.extractEnergy(7, true));
        assertEquals(12, storage.stored);
        assertTrue(storage.lastSimulation);

        assertEquals(7, adapter.extractEnergy(7, false));
        assertEquals(5, storage.stored);
        assertFalse(storage.lastSimulation);
    }

    @Test
    void rejectsNegativeRequestsBeforeCallingForge() {
        RecordingStorage storage = new RecordingStorage(12);
        NeoForgeEnergyStorageAdapter adapter = new NeoForgeEnergyStorageAdapter(storage);

        assertThrows(IllegalArgumentException.class, () -> adapter.extractEnergy(-1, false));
        assertEquals(12, storage.stored);
    }

    private static final class RecordingStorage implements IEnergyStorage {
        private int stored;
        private boolean lastSimulation;

        private RecordingStorage(int stored) {
            this.stored = stored;
        }

        @Override
        public int receiveEnergy(int maximum, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maximum, boolean simulate) {
            lastSimulation = simulate;
            int extracted = Math.min(stored, maximum);
            if (!simulate) {
                stored -= extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return 12;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
