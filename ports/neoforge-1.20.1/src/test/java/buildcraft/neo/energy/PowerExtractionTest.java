package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PowerExtractionTest {
    @Test
    void extractsOnlyWhatThePassiveProviderCanSupply() {
        MicroJouleBuffer provider = new MicroJouleBuffer(100);
        provider.receive(45, false);

        assertEquals(40, PowerExtraction.fromPassiveProvider(provider, 40));
        assertEquals(5, provider.stored());
    }

    @Test
    void extractsOnlyWhatTheFeStoreCanSupply() {
        MutableFeStore store = new MutableFeStore(30);

        assertEquals(20, PowerExtraction.fromFeStorage(store, 20));
        assertEquals(10, store.stored);
    }

    @Test
    void rejectsProviderValuesOutsideRequestedBounds() {
        PassivePowerProvider invalid = (minimum, maximum, simulate) -> maximum + 1;
        assertThrows(IllegalStateException.class, () -> PowerExtraction.fromPassiveProvider(invalid, 10));
    }

    @Test
    void rejectsStorageValuesOutsideRequestedBounds() {
        FeEnergyStorage invalid = (maximum, simulate) -> maximum + 1;
        assertThrows(IllegalStateException.class, () -> PowerExtraction.fromFeStorage(invalid, 10));
    }

    private static final class MutableFeStore implements FeEnergyStorage {
        private int stored;

        private MutableFeStore(int stored) {
            this.stored = stored;
        }

        @Override
        public int extractEnergy(int maximum, boolean simulate) {
            int extracted = Math.min(stored, maximum);
            if (!simulate) {
                stored -= extracted;
            }
            return extracted;
        }
    }
}
