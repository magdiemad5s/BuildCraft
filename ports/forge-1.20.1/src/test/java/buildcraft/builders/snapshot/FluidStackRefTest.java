package buildcraft.builders.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class FluidStackRefTest {
    @Test
    void resolvesReleasedUnnamespacedBuildCraftFluidFromSnapshotNbt() {
        assertEquals(
            new ResourceLocation("buildcraftenergy", "oil"),
            FluidStackRef.resolvePersistedFluidId("oil")
        );
        assertEquals(
            new ResourceLocation("buildcraftenergy", "fuel_dense_heat_2"),
            FluidStackRef.resolvePersistedFluidId("fuel_dense_heat_2")
        );
    }

    @Test
    void retainsNormalResolutionForVanillaUnknownAndNamespacedFluids() {
        assertEquals(
            new ResourceLocation("minecraft", "water"),
            FluidStackRef.resolvePersistedFluidId("water")
        );
        assertEquals(
            new ResourceLocation("minecraft", "third_party_oil"),
            FluidStackRef.resolvePersistedFluidId("third_party_oil")
        );
        assertEquals(
            new ResourceLocation("othermod", "oil"),
            FluidStackRef.resolvePersistedFluidId("othermod:oil")
        );
    }
}
