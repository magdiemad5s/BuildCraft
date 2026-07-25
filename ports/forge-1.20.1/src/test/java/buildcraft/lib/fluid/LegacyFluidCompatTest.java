package buildcraft.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

class LegacyFluidCompatTest {
    @Test
    void qualifiesReleasedUnnamespacedOilWithoutMutatingSavedData() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("FluidName", "oil");
        legacy.putInt("Amount", 1_000);
        CompoundTag payload = new CompoundTag();
        payload.putString("marker", "preserved");
        legacy.put("Tag", payload);

        CompoundTag normalized = LegacyFluidCompat.normalizeForLoad(legacy);

        assertNotSame(legacy, normalized);
        assertEquals("oil", legacy.getString("FluidName"));
        assertEquals("buildcraftenergy:oil", normalized.getString("FluidName"));
        assertEquals(1_000, normalized.getInt("Amount"));
        assertEquals("preserved", normalized.getCompound("Tag").getString("marker"));
    }

    @Test
    void coversEveryReleasedFluidFamilyAndHeatState() {
        Set<ResourceLocation> modernIds = new HashSet<>();
        int normalizedCount = 0;
        for (String family : LegacyFluidCompat.familyNames()) {
            for (int heat = 0; heat < LegacyFluidCompat.HEAT_STATE_COUNT; heat++) {
                String legacyName = LegacyFluidCompat.modernFluidPath(family, heat);
                CompoundTag legacy = new CompoundTag();
                legacy.putString("FluidName", legacyName);
                legacy.putInt("Amount", 250);

                CompoundTag normalized = LegacyFluidCompat.normalizeForLoad(legacy);
                ResourceLocation expected = LegacyFluidCompat.modernFluidId(family, heat);

                assertEquals(expected.toString(), normalized.getString("FluidName"));
                modernIds.add(expected);
                normalizedCount++;
            }
        }

        assertEquals(30, normalizedCount);
        assertEquals(30, modernIds.size());
    }

    @Test
    void leavesNamespacedVanillaAndUnknownNamesUntouched() {
        CompoundTag namespaced = fluidTag("buildcraftenergy:oil");
        CompoundTag vanilla = fluidTag("water");
        CompoundTag unknown = fluidTag("other_mod_oil");

        assertSame(namespaced, LegacyFluidCompat.normalizeForLoad(namespaced));
        assertSame(vanilla, LegacyFluidCompat.normalizeForLoad(vanilla));
        assertSame(unknown, LegacyFluidCompat.normalizeForLoad(unknown));
    }

    @Test
    void rejectsUnknownFamiliesAndHeatIndexes() {
        assertThrows(IllegalArgumentException.class, () -> LegacyFluidCompat.modernFluidId("unknown", 0));
        assertThrows(IllegalArgumentException.class, () -> LegacyFluidCompat.modernFluidId("oil", -1));
        assertThrows(
            IllegalArgumentException.class,
            () -> LegacyFluidCompat.modernFluidId("oil", LegacyFluidCompat.HEAT_STATE_COUNT)
        );
    }

    private static CompoundTag fluidTag(String name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("FluidName", name);
        tag.putInt("Amount", 1_000);
        return tag;
    }
}
