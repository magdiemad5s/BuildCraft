package buildcraft.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class LegacyFluidIdResolutionTest {
    @Test
    void lenientResolverMigratesOnlyKnownBuildCraftNames() {
        assertEquals(
            new ResourceLocation("buildcraftenergy", "oil"),
            LegacyFluidCompat.tryResolveFluidIdForLoad("oil")
        );
        assertEquals(
            new ResourceLocation("minecraft", "water"),
            LegacyFluidCompat.tryResolveFluidIdForLoad("water")
        );
        assertEquals(
            new ResourceLocation("othermod", "oil"),
            LegacyFluidCompat.tryResolveFluidIdForLoad("othermod:oil")
        );
    }

    @Test
    void lenientResolverRejectsAbsentOrMalformedOptionalFields() {
        assertNull(LegacyFluidCompat.tryResolveFluidIdForLoad(null));
        assertNull(LegacyFluidCompat.tryResolveFluidIdForLoad(""));
        assertNull(LegacyFluidCompat.tryResolveFluidIdForLoad("not a fluid"));
    }
}
