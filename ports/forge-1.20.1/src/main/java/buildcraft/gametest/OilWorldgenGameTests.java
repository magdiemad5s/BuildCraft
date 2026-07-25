/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.Random;

import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.BCEnergyWorldGen;
import buildcraft.energy.generation.features.OilGenerator;
import buildcraft.energy.generation.features.OilStructure;
import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime registry smoke gate for the data-driven Forge 1.20.1 oil generator. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class OilWorldgenGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final ResourceLocation OIL_FEATURE =
        new ResourceLocation("buildcraftenergy", "worldgen.feature.oil");
    private static final ResourceLocation OIL_CONFIGURED_FEATURE =
        new ResourceLocation("buildcraftenergy", "oil_configured_feature");
    private static final ResourceLocation OIL_PLACED_FEATURE =
        new ResourceLocation("buildcraftenergy", "oil_placed_feature");

    private OilWorldgenGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void oilFeatureAndDataDrivenRegistryEntriesLoad(GameTestHelper helper) {
        if (!BuiltInRegistries.FEATURE.containsKey(OIL_FEATURE)) {
            helper.fail("Missing registered oil feature " + OIL_FEATURE);
            return;
        }
        if (!BCEnergyWorldGen.OIL_FEATURE.isPresent()) {
            helper.fail("The Forge registry object for the oil feature was not populated");
            return;
        }

        Registry<ConfiguredFeature<?, ?>> configuredFeatures =
            helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        if (!configuredFeatures.containsKey(OIL_CONFIGURED_FEATURE)) {
            helper.fail("Missing configured oil feature " + OIL_CONFIGURED_FEATURE);
            return;
        }

        Registry<PlacedFeature> placedFeatures =
            helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        if (!placedFeatures.containsKey(OIL_PLACED_FEATURE)) {
            helper.fail("Missing placed oil feature " + OIL_PLACED_FEATURE);
            return;
        }

        if (!BCEnergyConfig.enableOilGeneration
            || BCEnergyConfig.oilWellGenerationRate <= 0.0
            || BCEnergyConfig.smallOilGenProb <= 0.0
            || BCEnergyConfig.mediumOilGenProb <= 0.0
            || BCEnergyConfig.largeOilGenProb <= 0.0) {
            helper.fail("Default oil-generation config was not loaded before GameTest startup");
            return;
        }
        helper.succeed();
    }

    /**
     * Regression for the released complaint that small deposits could be selected but never appear. The deterministic
     * tendril is the same surface structure used by the small-deposit branch, and it must place the registered crude
     * oil fluid into real world blocks rather than returning an empty/placeholder fluid state.
     */
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void smallOilSurfaceStructurePlacesRegisteredCrudeOil(GameTestHelper helper) {
        OilGenerator.bottomY = helper.getLevel().getMinBuildHeight();
        OilGenerator.worldHeight = helper.getLevel().getMaxBuildHeight() - OilGenerator.bottomY;
        OilGenerator.seaLevel = helper.getLevel().getSeaLevel();

        // GameTests are laid out near min build height in a normal generated world. A short local stone fixture is not
        // the WORLD_SURFACE queried by PatternTerrainHeight; record and stabilize the actual surface column instead.
        BlockPos absoluteColumn = helper.absolutePos(new BlockPos(2, 0, 2));
        BlockPos absoluteCenter = helper.getLevel()
            .getHeightmapPos(Heightmap.Types.WORLD_SURFACE, absoluteColumn)
            .below();
        helper.getLevel().setBlockAndUpdate(absoluteCenter, Blocks.STONE.defaultBlockState());

        OilStructure structure = OilGenerator.createTendril(absoluteCenter, 1, 2, new Random(0xBC1201L));
        structure.generate(helper.getLevel(), new Box(structure.box.min(), structure.box.max()));

        if (helper.getLevel().getFluidState(absoluteCenter).getType() != BCEnergyFluids.crudeOil[0]) {
            helper.fail("Small oil surface structure completed without placing registered crude oil");
            return;
        }
        helper.succeed();
    }
}
