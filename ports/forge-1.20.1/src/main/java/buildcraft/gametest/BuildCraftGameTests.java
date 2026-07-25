/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.core.BCCore;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.blockEntity.TileEngineRedstone_BC8;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileTank;
import buildcraft.lib.CreativeTabManager;
import buildcraft.robotics.BCRobotics;
import buildcraft.silicon.BCSilicon;
import buildcraft.transport.BCTransport;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Runtime smoke gates for systems that compilation and JSON validation cannot prove.
 *
 * <p>These tests intentionally use a tiny isolated structure and do not touch user worlds.</p>
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class BuildCraftGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final Set<String> PLAYER_FACING_ITEM_NAMESPACES = Set.of(
        "buildcraftlib",
        "buildcraftcore",
        "buildcraftbuilders",
        "buildcraftenergy",
        "buildcraftfactory",
        "buildcrafttransport",
        "buildcraftsilicon",
        "buildcraftrobotics"
    );
    private static final Set<ResourceLocation> INTENTIONALLY_HIDDEN_ITEMS = Set.of(
        new ResourceLocation("buildcraftlib", "guide_note")
    );

    private BuildCraftGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void legacyRegistriesAndRepresentativeRecipesLoad(GameTestHelper helper) {
        assertRegistryEntry(helper, ForgeRegistries.BLOCKS, "buildcraftcore:engine");
        assertRegistryEntry(helper, ForgeRegistries.BLOCKS, "buildcraftbuilders:quarry");
        assertRegistryEntry(helper, ForgeRegistries.BLOCKS, "buildcraftfactory:tank");
        assertRegistryEntry(helper, ForgeRegistries.BLOCKS, "buildcrafttransport:pipe_holder");
        assertRegistryEntry(helper, ForgeRegistries.ITEMS, "buildcraftlib:guide");
        assertRegistryEntry(helper, ForgeRegistries.ITEMS, "buildcraftlib:guide_note");

        String[] recipes = {
            "buildcraftcore:gear_wood",
            "buildcraftbuilders:quarry",
            "buildcraftfactory:tank",
            "buildcrafttransport:diamond_wood_fluid",
            "buildcraftsilicon:assembly/diamond_chipset",
            "buildcraftlib:guide_book"
        };
        for (String recipeId : recipes) {
            if (helper.getLevel().getRecipeManager().byKey(new ResourceLocation(recipeId)).isEmpty()) {
                helper.fail("Missing representative BuildCraft recipe " + recipeId);
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void coreMachineBlocksCreateTheirExpectedBlockEntities(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos quarryPos = new BlockPos(2, 1, 1);
        BlockPos pipePos = new BlockPos(3, 1, 1);
        helper.setBlock(enginePos, BCCoreBlocks.ENGINE_BC8.get());
        helper.setBlock(quarryPos, BCBuildersBlocks.QUARRY.get());
        helper.setBlock(pipePos, BCTransportBlocks.pipeHolder.get());

        helper.runAfterDelay(2, () -> {
            assertBlockEntity(helper, enginePos, TileEngineRedstone_BC8.class);
            assertBlockEntity(helper, quarryPos, TileQuarry.class);
            assertBlockEntity(helper, pipePos, TilePipeHolder.class);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void tankExposesForgeFluidCapabilityAndStoresWater(GameTestHelper helper) {
        BlockPos lowerPos = new BlockPos(2, 1, 2);
        BlockPos upperPos = lowerPos.above();
        helper.setBlock(lowerPos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(upperPos, BCFactoryBlocks.TANK_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            BlockEntity upperEntity = helper.getBlockEntity(upperPos);
            if (!(upperEntity instanceof TileTank upperTank)) {
                helper.fail("Upper BuildCraft tank block entity was not created");
                return;
            }
            int accepted = upperTank.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .map(handler -> handler.fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE))
                .orElse(0);
            if (accepted != 1_000) {
                helper.fail("BuildCraft tank fluid capability accepted " + accepted + " mB instead of 1000 mB");
                return;
            }

            BlockEntity lowerEntity = helper.getBlockEntity(lowerPos);
            if (!(lowerEntity instanceof TileTank lowerTank)) {
                helper.fail("Lower BuildCraft tank block entity was not created");
                return;
            }
            int stored = upperTank.tank.getFluidAmount() + lowerTank.tank.getFluidAmount();
            if (stored != 1_000) {
                helper.fail("Stacked BuildCraft tanks stored " + stored + " mB instead of 1000 mB");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void creativeTabsExposeEveryPlayerFacingItemAndVariant(GameTestHelper helper) {
        List<ItemStack> displayed = new ArrayList<>();
        BCCore.BUILDCRAFT_TAB.accept(List.of(), displayed::add);
        BCCore.tabFluids.accept(List.of(), displayed::add);
        BCTransport.tabPipes.accept(List.of(), displayed::add);
        BCTransport.tabPlugs.accept(List.of(), displayed::add);
        BCSilicon.tabFacades.accept(List.of(), displayed::add);
        BCRobotics.TAB_ROBOTICS.accept(List.of(), displayed::add);

        List<String> missing = new ArrayList<>();
        for (ResourceLocation itemId : ForgeRegistries.ITEMS.getKeys()) {
            if (!PLAYER_FACING_ITEM_NAMESPACES.contains(itemId.getNamespace())
                || INTENTIONALLY_HIDDEN_ITEMS.contains(itemId)) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                missing.add(itemId + " (registry value is null)");
                continue;
            }
            List<ItemStack> expectedVariants = new ArrayList<>();
            CreativeTabManager.addItemVariants(item, expectedVariants::add);
            if (expectedVariants.isEmpty()) {
                missing.add(itemId + " (no creative variant)");
                continue;
            }
            for (int variant = 0; variant < expectedVariants.size(); variant++) {
                ItemStack expected = expectedVariants.get(variant);
                if (displayed.stream().noneMatch(actual -> ItemStack.isSameItemSameTags(actual, expected))) {
                    missing.add(itemId + " variant " + variant);
                }
            }
        }

        if (!missing.isEmpty()) {
            helper.fail(
                "BuildCraft creative tabs are missing " + missing.size() + " item variant(s): "
                    + String.join(", ", missing.subList(0, Math.min(missing.size(), 20)))
            );
            return;
        }
        helper.succeed();
    }

    private static <T> void assertRegistryEntry(
        GameTestHelper helper,
        net.minecraftforge.registries.IForgeRegistry<T> registry,
        String id
    ) {
        ResourceLocation location = new ResourceLocation(id);
        if (!registry.containsKey(location)) {
            helper.fail("Missing legacy registry entry " + id);
        }
    }

    private static void assertBlockEntity(
        GameTestHelper helper,
        BlockPos relativePos,
        Class<? extends BlockEntity> expectedType
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(relativePos);
        if (!expectedType.isInstance(blockEntity)) {
            helper.fail(
                "Expected " + expectedType.getSimpleName() + " at " + relativePos + " but found " + blockEntity
            );
        }
    }
}
