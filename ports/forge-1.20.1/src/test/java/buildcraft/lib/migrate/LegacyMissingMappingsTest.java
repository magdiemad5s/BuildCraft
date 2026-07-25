package buildcraft.lib.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import buildcraft.energy.BCEnergyFluids;
import buildcraft.lib.fluid.LegacyFluidCompat;
import net.minecraft.resources.ResourceLocation;

class LegacyMissingMappingsTest {
    @Test
    void containsEveryManifestAliasByRegistryType() {
        assertEquals(24, LegacyMissingMappings.itemAliasCount());
        assertEquals(55, LegacyMissingMappings.blockAliasCount());
        assertEquals(6, LegacyMissingMappings.blockEntityAliasCount());
    }

    @Test
    void resolvesLegacyPathsCaseInsensitively() {
        assertEquals(
            new ResourceLocation("buildcraftcore", "wrench"),
            LegacyMissingMappings.resolveItemAlias("WRENCHITEM")
        );
        assertEquals(
            new ResourceLocation("buildcraftfactory", "mining_well"),
            LegacyMissingMappings.resolveBlockAlias("miningWellBlock")
        );
        assertEquals(
            new ResourceLocation("buildcraftcore", "marker.volume"),
            LegacyMissingMappings.resolveBlockEntityAlias("BuildCraft.Builders.Marker")
        );
    }


    @Test
    void restoresReleasedConstructionMarkerAliases() {
        ResourceLocation marker = new ResourceLocation("buildcraftbuilders", "marker_construction");
        ResourceLocation markerEntity = new ResourceLocation("buildcraftbuilders", "entity_marker_construction");
        assertEquals(marker, LegacyMissingMappings.resolveItemAlias("constructionMarkerBlock"));
        assertEquals(marker, LegacyMissingMappings.resolveBlockAlias("constructionMarkerBlock"));
        assertEquals(
            markerEntity,
            LegacyMissingMappings.resolveBlockEntityAlias("buildcraft.builders.ConstructionMarker")
        );
        assertEquals(
            markerEntity,
            LegacyMissingMappings.resolveBlockEntityAlias("net.minecraft.src.builders.TileConstructionMarker")
        );
    }
    @Test
    void includesExplicitOilAndFuelMigrationAliases() {
        assertEquals(
            new ResourceLocation("buildcraftenergy", "oil"),
            LegacyMissingMappings.resolveBlockAlias("fluid_block_oil")
        );
        assertEquals(
            new ResourceLocation("buildcraftenergy", "fuel_light"),
            LegacyMissingMappings.resolveBlockAlias("fluid_block_fuel")
        );
    }

    @Test
    void everyReleasedFluidBlockAliasTargetsADeclaredLiquidBlock() throws ReflectiveOperationException {
        buildcraft.test.MinecraftTestBootstrap.bootStrap();
        if (BCEnergyFluids.OIL_BLOCK.isEmpty()) {
            Method registryFluid = BCEnergyFluids.class.getDeclaredMethod("registryFluid");
            assertTrue(
                registryFluid.trySetAccessible(),
                "Unable to inspect BuildCraft Energy's deferred fluid declarations"
            );
            registryFluid.invoke(null);
        }

        Set<ResourceLocation> declaredLiquidBlocks = BCEnergyFluids.OIL_BLOCK.stream()
            .map(registryObject -> registryObject.getId())
            .collect(Collectors.toSet());

        assertEquals(30, BCEnergyFluids.OIL_BLOCK.size());
        assertEquals(30, declaredLiquidBlocks.size());
        for (String family : LegacyFluidCompat.familyNames()) {
            for (int heat = 0; heat < LegacyFluidCompat.HEAT_STATE_COUNT; heat++) {
                String legacyPath = LegacyFluidCompat.legacyFluidBlockPath(family, heat);
                ResourceLocation expectedTarget = LegacyFluidCompat.modernFluidId(family, heat);
                ResourceLocation aliasTarget = LegacyMissingMappings.resolveBlockAlias(legacyPath);

                assertEquals(expectedTarget, aliasTarget, legacyPath);
                assertTrue(declaredLiquidBlocks.contains(aliasTarget), aliasTarget.toString());
            }
        }
        assertTrue(
            declaredLiquidBlocks.contains(LegacyMissingMappings.resolveBlockAlias("fluid_block_oil"))
        );
        assertTrue(
            declaredLiquidBlocks.contains(LegacyMissingMappings.resolveBlockAlias("fluid_block_fuel"))
        );
    }

    @Test
    void unknownPathsAreNotSilentlyRedirected() {
        assertNull(LegacyMissingMappings.resolveItemAlias("unknown"));
        assertNull(LegacyMissingMappings.resolveBlockAlias("unknown"));
        assertNull(LegacyMissingMappings.resolveBlockEntityAlias("unknown"));
    }
}