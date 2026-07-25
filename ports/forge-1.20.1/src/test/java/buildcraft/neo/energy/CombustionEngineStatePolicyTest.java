package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class CombustionEngineStatePolicyTest {
    @Test
    void fractionalDirtyFuelResidueRoundTripsUnderLegacyKey() {
        CompoundTag nbt = new CompoundTag();

        CombustionEngineStatePolicy.writeResidueAmount(nbt, 0.625);

        assertTrue(nbt.contains("residueAmount"));
        assertEquals(0.625, nbt.getDouble("residueAmount"));
        assertEquals(0.625, CombustionEngineStatePolicy.readResidueAmount(nbt));
    }

    @Test
    void missingOrMalformedResidueCannotCreateInvalidEngineState() {
        assertEquals(0.0, CombustionEngineStatePolicy.readResidueAmount(new CompoundTag()));
        assertEquals(0.0, CombustionEngineStatePolicy.sanitizeResidueAmount(-1.0));
        assertEquals(0.0, CombustionEngineStatePolicy.sanitizeResidueAmount(Double.NaN));
        assertEquals(0.0, CombustionEngineStatePolicy.sanitizeResidueAmount(Double.POSITIVE_INFINITY));
    }

    @Test
    void accumulatedResidueAboveOneIsPreservedWhenOutputTankWasBackPressured() {
        assertEquals(3.75, CombustionEngineStatePolicy.sanitizeResidueAmount(3.75));
    }

    @Test
    void releasedTankLayoutWinsWhenBothLayoutsExist() {
        CompoundTag root = new CompoundTag();
        CompoundTag modern = tankData(4_000);
        CompoundTag legacy = tankData(8_000);
        root.put(CombustionEngineStatePolicy.TANKS_NBT_KEY, modern);
        root.put(CombustionEngineStatePolicy.LEGACY_TANKS_NBT_KEY, legacy);

        CompoundTag selected = CombustionEngineStatePolicy.selectTankData(root);

        assertEquals(4_000, selected.getCompound("fuel").getInt("Amount"));
    }

    @Test
    void deliberatelyEmptyReleasedTankLayoutStillWinsOverStaleFallback() {
        CompoundTag root = new CompoundTag();
        root.put(CombustionEngineStatePolicy.TANKS_NBT_KEY, new CompoundTag());
        root.put(CombustionEngineStatePolicy.LEGACY_TANKS_NBT_KEY, tankData(8_000));

        CompoundTag selected = CombustionEngineStatePolicy.selectTankData(root);

        assertTrue(selected.isEmpty());
    }

    @Test
    void earlyCommunityEditionTankLayoutRemainsAReadFallback() {
        CompoundTag root = new CompoundTag();
        root.put(CombustionEngineStatePolicy.LEGACY_TANKS_NBT_KEY, tankData(8_000));

        CompoundTag selected = CombustionEngineStatePolicy.selectTankData(root);

        assertEquals(8_000, selected.getCompound("fuel").getInt("Amount"));
    }

    private static CompoundTag tankData(int fuelAmount) {
        CompoundTag fuel = new CompoundTag();
        fuel.putInt("Amount", fuelAmount);
        CompoundTag tanks = new CompoundTag();
        tanks.put("fuel", fuel);
        return tanks;
    }
}