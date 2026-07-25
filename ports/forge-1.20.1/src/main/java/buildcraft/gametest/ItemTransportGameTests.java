/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IAction;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.pipe.PipeEventActionActivate;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.transport.BCTransportStatements;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportConfig;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.item.ItemPipeHolder;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourClay;
import buildcraft.transport.pipe.behaviour.PipeBehaviourCobble;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDaizuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamondItem;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourGold;
import buildcraft.transport.pipe.behaviour.PipeBehaviourIron;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLapis;
import buildcraft.transport.pipe.behaviour.PipeBehaviourObsidian;
import buildcraft.transport.pipe.behaviour.PipeBehaviourQuartz;
import buildcraft.transport.pipe.behaviour.PipeBehaviourSandstone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;
import buildcraft.transport.pipe.behaviour.PipeBehaviourVoid;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Tick-driven runtime contracts for the legacy BuildCraft item-transport families. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class ItemTransportGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final double LOOSE_ITEM_SEARCH_RADIUS = 3.0;

    private ItemTransportGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void everyRegisteredItemPipeInstallsItsExpectedDefinitionAndBehaviour(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(2, 1, 2);
        helper.runAfterDelay(1, () -> {
            List<ItemPipeCase> cases = List.of(
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_WOOD.get(), "pipe_wood_item", BCTransportPipes.woodItem,
                    PipeBehaviourWood.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_COBBLE.get(), "pipe_cobble_item",
                    BCTransportPipes.cobbleItem, PipeBehaviourCobble.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_STONE.get(), "pipe_stone_item", BCTransportPipes.stoneItem,
                    PipeBehaviourStone.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_QUARTZ.get(), "pipe_quartz_item",
                    BCTransportPipes.quartzItem, PipeBehaviourQuartz.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_IRON.get(), "pipe_iron_item", BCTransportPipes.ironItem,
                    PipeBehaviourIron.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_GOLD.get(), "pipe_gold_item", BCTransportPipes.goldItem,
                    PipeBehaviourGold.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_CLAY.get(), "pipe_clay_item", BCTransportPipes.clayItem,
                    PipeBehaviourClay.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_SAND_STONE.get(), "pipe_sandstone_item",
                    BCTransportPipes.sandstoneItem, PipeBehaviourSandstone.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_VOID.get(), "pipe_void_item", BCTransportPipes.voidItem,
                    PipeBehaviourVoid.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_OBSIDIAN.get(), "pipe_obsidian_item",
                    BCTransportPipes.obsidianItem, PipeBehaviourObsidian.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_DIAMOND.get(), "pipe_diamond_item",
                    BCTransportPipes.diamondItem, PipeBehaviourDiamondItem.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_DIAWOOD.get(), "pipe_diamond_wood_item",
                    BCTransportPipes.diaWoodItem, PipeBehaviourWoodDiamond.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_LAPIS.get(), "pipe_lapis_item", BCTransportPipes.lapisItem,
                    PipeBehaviourLapis.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_DAIZULI.get(), "pipe_daizuli_item",
                    BCTransportPipes.daizuliItem, PipeBehaviourDaizuli.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_EMZULI.get(), "pipe_emzuli_item",
                    BCTransportPipes.emzuliItem, PipeBehaviourEmzuli.class),
                new ItemPipeCase(BCTransportItems.PIPE_ITEM_STRIPES.get(), "pipe_stripes_item",
                    BCTransportPipes.stripesItem, PipeBehaviourStripes.class)
            );

            Set<PipeDefinition> audited = new HashSet<>();
            for (ItemPipeCase pipeCase : cases) {
                ResourceLocation actualId = ForgeRegistries.ITEMS.getKey(pipeCase.item());
                ResourceLocation expectedId = new ResourceLocation("buildcrafttransport", pipeCase.registryPath());
                if (!expectedId.equals(actualId) || pipeCase.item().getDefinition() != pipeCase.definition()) {
                    helper.fail("Item pipe identity mismatch: expected " + expectedId + " but found " + actualId);
                    return;
                }
                TilePipeHolder holder = installPipe(helper, pipePos, pipeCase.item(), actualId.toString());
                if (holder == null) return;
                if (holder.getPipe().getDefinition() != pipeCase.definition()
                    || !pipeCase.behaviourType().isInstance(holder.getPipe().getBehaviour())
                    || !(holder.getPipe().getFlow() instanceof PipeFlowItems)) {
                    helper.fail(actualId + " installed the wrong definition, behaviour, or flow");
                    return;
                }
                audited.add(pipeCase.definition());
            }
            for (PipeDefinition definition : BCTransportItems.PIPE_MAP.keySet()) {
                if (definition.flowType == PipeApi.flowItems && !audited.contains(definition)) {
                    helper.fail("Runtime audit omitted registered item pipe " + definition.identifier);
                    return;
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void stoneCobblestoneAndQuartzNetworksRemainSeparated(GameTestHelper helper) {
        BlockPos stoneWestPos = new BlockPos(1, 1, 2);
        BlockPos stoneCenterPos = new BlockPos(2, 1, 2);
        BlockPos cobblePos = new BlockPos(3, 1, 2);
        BlockPos quartzPos = new BlockPos(2, 1, 1);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, stoneWestPos, BCTransportItems.PIPE_ITEM_STONE.get(), "west stone item pipe");
            installPipe(helper, stoneCenterPos, BCTransportItems.PIPE_ITEM_STONE.get(), "center stone item pipe");
            installPipe(helper, cobblePos, BCTransportItems.PIPE_ITEM_COBBLE.get(), "cobblestone item pipe");
            installPipe(helper, quartzPos, BCTransportItems.PIPE_ITEM_QUARTZ.get(), "quartz item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder west = requireInstalledPipe(helper, stoneWestPos, "west stone item pipe");
            TilePipeHolder center = requireInstalledPipe(helper, stoneCenterPos, "center stone item pipe");
            TilePipeHolder cobble = requireInstalledPipe(helper, cobblePos, "cobblestone item pipe");
            TilePipeHolder quartz = requireInstalledPipe(helper, quartzPos, "quartz item pipe");
            if (west == null || center == null || cobble == null || quartz == null) return;
            if (!west.getPipe().isConnected(Direction.EAST) || !center.getPipe().isConnected(Direction.WEST)) {
                helper.fail("Two stone item pipes failed to join their network");
                return;
            }
            if (center.getPipe().isConnected(Direction.EAST) || cobble.getPipe().isConnected(Direction.WEST)) {
                helper.fail("Stone and cobblestone item pipes incorrectly joined each other");
                return;
            }
            if (center.getPipe().isConnected(Direction.NORTH) || quartz.getPipe().isConnected(Direction.SOUTH)) {
                helper.fail("Stone and quartz item pipes incorrectly joined each other");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void poweredWoodenPipeExtractsPartialSourceStackAndConservesEveryItem(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos woodPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        int sourceCount = 17;
        int requestedCount = 64;
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.IRON_INGOT, sourceCount));
            installPipe(helper, woodPos, BCTransportItems.PIPE_ITEM_WOOD.get(), "wooden item pipe");
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "stone item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder wood = requireInstalledPipe(helper, woodPos, "wooden item pipe");
            ChestBlockEntity source = requireChest(helper, sourcePos, "source chest");
            ChestBlockEntity destination = requireChest(helper, destinationPos, "destination chest");
            if (wood == null || source == null || destination == null) return;
            if (!(wood.getPipe().getBehaviour() instanceof PipeBehaviourWood behaviour)
                || !(wood.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Wooden item pipe did not install its extraction behaviour and item flow");
                return;
            }
            if (behaviour.getCurrentDir() != Direction.WEST) {
                helper.fail("Wooden item pipe faced " + behaviour.getCurrentDir() + " instead of its source chest");
                return;
            }
            long offered = requestedCount * BCTransportConfig.mjPerItem;
            long unused = behaviour.receivePower(offered, FluidAction.EXECUTE);
            long expectedUnused = offered - sourceCount * BCTransportConfig.mjPerItem;
            if (unused != expectedUnused) {
                helper.fail("Wooden item pipe left " + unused + " microjoules instead of " + expectedUnused);
                return;
            }
            if (countItem(source, Items.IRON_INGOT) != 0 || !flow.doesContainItems()) {
                helper.fail("Wooden item pipe did not extract the available partial source stack");
                return;
            }
            helper.succeedWhen(() -> {
                int inSource = countItem(source, Items.IRON_INGOT);
                int inDestination = countItem(destination, Items.IRON_INGOT);
                int inPipes = countTravellingItem(flow, Items.IRON_INGOT);
                int dropped = countItemEntities(helper, woodPos, Items.IRON_INGOT, LOOSE_ITEM_SEARCH_RADIUS);
                int total = inSource + inDestination + inPipes + dropped;
                if (inDestination != sourceCount || inSource != 0 || inPipes != 0 || dropped != 0) {
                    helper.fail("Expected all " + sourceCount + " iron ingots in the destination; source/destination/"
                        + "pipes/dropped=" + inSource + "/" + inDestination + "/" + inPipes + "/" + dropped);
                }
                if (total != sourceCount) helper.fail("Item network conserved " + total + " of " + sourceCount);
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void travellingItemsSurvivePipeBlockEntityNbtReload(GameTestHelper helper) {
        BlockPos sourceSidePos = new BlockPos(1, 1, 2);
        BlockPos pipePos = new BlockPos(2, 1, 2);
        BlockPos destinationPos = new BlockPos(3, 1, 2);
        helper.setBlock(sourceSidePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () ->
            installPipe(helper, pipePos, BCTransportItems.PIPE_ITEM_STONE.get(), "reload stone item pipe")
        );
        helper.runAfterDelay(6, () -> {
            PipeFlowItems flow = requireItemFlow(helper, pipePos, "reload stone item pipe");
            if (flow == null) return;
            ItemStack remainder = flow.injectItem(
                new ItemStack(Items.DIAMOND, 9), true, Direction.WEST, null, 0.02
            );
            if (!remainder.isEmpty() || !flow.doesContainItems()) {
                helper.fail("Stone item pipe did not accept the stack before save/load");
            }
        });
        helper.runAfterDelay(9, () -> {
            TilePipeHolder original = requireInstalledPipe(helper, pipePos, "reload stone item pipe");
            if (original == null) return;
            CompoundTag saved = original.saveWithFullMetadata();
            ListTag savedItems = saved.getCompound("pipe").getCompound("flow")
                .getList("items", Tag.TAG_COMPOUND);
            if (savedItems.isEmpty()) {
                helper.fail("Pipe NBT omitted its in-flight item stack");
                return;
            }
            BlockPos absolutePos = helper.absolutePos(pipePos);
            BlockState state = helper.getBlockState(pipePos);
            original.onChunkUnloaded();
            helper.getLevel().removeBlockEntity(absolutePos);
            BlockEntity loaded = BlockEntity.loadStatic(absolutePos, state, saved);
            if (!(loaded instanceof TilePipeHolder restored)) {
                helper.fail("Pipe NBT did not recreate a TilePipeHolder");
                return;
            }
            restored.setLevel(helper.getLevel());
            restored.clearRemoved();
            helper.getLevel().setBlockEntity(restored);
            restored.onLoad();
            if (restored.getPipe().getDefinition() != BCTransportPipes.stoneItem
                || !(restored.getPipe().getFlow() instanceof PipeFlowItems restoredFlow)
                || !restoredFlow.doesContainItems()) {
                helper.fail("Reloaded pipe lost its definition or in-flight item");
            }
        });
        helper.runAfterDelay(11, () -> helper.succeedWhen(() -> {
            ChestBlockEntity destination = requireChest(helper, destinationPos, "reload destination chest");
            PipeFlowItems flow = requireItemFlow(helper, pipePos, "reloaded stone item pipe");
            if (destination == null || flow == null) return;
            int delivered = countItem(destination, Items.DIAMOND);
            int dropped = countItemEntities(helper, pipePos, Items.DIAMOND, LOOSE_ITEM_SEARCH_RADIUS);
            if (delivered != 9 || flow.doesContainItems() || dropped != 0) {
                helper.fail("Reloaded item pipe delivered " + delivered + "/9 diamonds with " + dropped
                    + " dropped and flow-active=" + flow.doesContainItems());
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void itemWithNoDestinationDropsAsAnExactFallbackStack(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos woodPos = new BlockPos(2, 1, 2);
        int expected = 5;
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "fallback source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.EMERALD, expected));
            installPipe(helper, woodPos, BCTransportItems.PIPE_ITEM_WOOD.get(), "fallback wooden item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder wood = requireInstalledPipe(helper, woodPos, "fallback wooden item pipe");
            ChestBlockEntity source = requireChest(helper, sourcePos, "fallback source chest");
            if (wood == null || source == null) return;
            if (!(wood.getPipe().getBehaviour() instanceof PipeBehaviourWood behaviour)
                || !(wood.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Fallback fixture did not install a wooden item pipe");
                return;
            }
            long unused = behaviour.receivePower(expected * BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            if (unused != 0 || countItem(source, Items.EMERALD) != 0) {
                helper.fail("Wooden pipe failed to extract the fallback stack");
                return;
            }
            helper.succeedWhen(() -> {
                int dropped = countItemEntities(helper, woodPos, Items.EMERALD, LOOSE_ITEM_SEARCH_RADIUS);
                if (dropped != expected || flow.doesContainItems()) {
                    helper.fail("No-destination fallback produced " + dropped + "/" + expected
                        + " emeralds and flow-active=" + flow.doesContainItems());
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void diamondFiltersRouteItemsToTheirConfiguredInventories(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos diamondPos = new BlockPos(2, 1, 2);
        BlockPos northChestPos = new BlockPos(2, 1, 1);
        BlockPos southChestPos = new BlockPos(2, 1, 3);
        helper.setBlock(northChestPos, Blocks.CHEST);
        helper.setBlock(southChestPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "diamond entry pipe");
            installPipe(helper, diamondPos, BCTransportItems.PIPE_ITEM_DIAMOND.get(), "diamond item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, diamondPos, "diamond item pipe");
            if (holder == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourDiamondItem behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Diamond item pipe did not install its filter behaviour and item flow");
                return;
            }
            behaviour.filters.setStackInSlot(
                Direction.NORTH.ordinal() * PipeBehaviourDiamondItem.FILTERS_PER_SIDE,
                new ItemStack(Items.IRON_INGOT)
            );
            behaviour.filters.setStackInSlot(
                Direction.SOUTH.ordinal() * PipeBehaviourDiamondItem.FILTERS_PER_SIDE,
                new ItemStack(Items.GOLD_INGOT)
            );
            ItemStack ironRemainder = flow.injectItem(
                new ItemStack(Items.IRON_INGOT, 4), true, Direction.WEST, null, 0.08
            );
            ItemStack goldRemainder = flow.injectItem(
                new ItemStack(Items.GOLD_INGOT, 3), true, Direction.WEST, null, 0.08
            );
            if (!ironRemainder.isEmpty() || !goldRemainder.isEmpty()) {
                helper.fail("Diamond item pipe refused a connected ingress stack");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity north = requireChest(helper, northChestPos, "north filtered chest");
                ChestBlockEntity south = requireChest(helper, southChestPos, "south filtered chest");
                if (north == null || south == null) return;
                int northIron = countItem(north, Items.IRON_INGOT);
                int northGold = countItem(north, Items.GOLD_INGOT);
                int southIron = countItem(south, Items.IRON_INGOT);
                int southGold = countItem(south, Items.GOLD_INGOT);
                if (northIron != 4 || northGold != 0 || southIron != 0 || southGold != 3) {
                    helper.fail("Diamond routing north(iron/gold)=" + northIron + "/" + northGold
                        + ", south(iron/gold)=" + southIron + "/" + southGold);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void ironPipeUsesOnlyItsSelectedOutput(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos ironPos = new BlockPos(2, 1, 2);
        BlockPos northChestPos = new BlockPos(2, 1, 1);
        BlockPos southChestPos = new BlockPos(2, 1, 3);
        helper.setBlock(northChestPos, Blocks.CHEST);
        helper.setBlock(southChestPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "iron entry pipe");
            installPipe(helper, ironPos, BCTransportItems.PIPE_ITEM_IRON.get(), "iron item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, ironPos, "iron item pipe");
            if (holder == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourIron behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Iron item pipe did not install its directional behaviour and item flow");
                return;
            }
            for (int i = 0; i < Direction.values().length && behaviour.getCurrentDir() != Direction.NORTH; i++) {
                behaviour.advanceFacing();
            }
            if (behaviour.getCurrentDir() != Direction.NORTH) {
                helper.fail("Iron item pipe could not select its connected north output");
                return;
            }
            ItemStack remainder = flow.injectItem(
                new ItemStack(Items.REDSTONE, 8), true, Direction.WEST, null, 0.08
            );
            if (!remainder.isEmpty()) {
                helper.fail("Iron item pipe refused a connected ingress stack");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity north = requireChest(helper, northChestPos, "iron north chest");
                ChestBlockEntity south = requireChest(helper, southChestPos, "iron south chest");
                if (north == null || south == null) return;
                int northCount = countItem(north, Items.REDSTONE);
                int southCount = countItem(south, Items.REDSTONE);
                if (northCount != 8 || southCount != 0) {
                    helper.fail("Iron item pipe delivered north/south=" + northCount + "/" + southCount);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void clayPipePrioritizesInventoryOverPipeBranch(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos clayPos = new BlockPos(2, 1, 2);
        BlockPos branchPos = new BlockPos(3, 1, 2);
        BlockPos preferredChestPos = new BlockPos(2, 1, 1);
        BlockPos branchChestPos = new BlockPos(4, 1, 2);
        helper.setBlock(preferredChestPos, Blocks.CHEST);
        helper.setBlock(branchChestPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "clay entry pipe");
            installPipe(helper, clayPos, BCTransportItems.PIPE_ITEM_CLAY.get(), "clay item pipe");
            installPipe(helper, branchPos, BCTransportItems.PIPE_ITEM_STONE.get(), "clay branch pipe");
        });
        helper.runAfterDelay(6, () -> {
            PipeFlowItems flow = requireItemFlow(helper, clayPos, "clay item pipe");
            if (flow == null) return;
            ItemStack remainder = flow.injectItem(new ItemStack(Items.CLAY_BALL, 6), true, Direction.WEST, null, 0.08);
            if (!remainder.isEmpty()) {
                helper.fail("Clay item pipe refused a connected ingress stack");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity preferred = requireChest(helper, preferredChestPos, "preferred clay chest");
                ChestBlockEntity branch = requireChest(helper, branchChestPos, "clay branch chest");
                if (preferred == null || branch == null) return;
                int preferredCount = countItem(preferred, Items.CLAY_BALL);
                int branchCount = countItem(branch, Items.CLAY_BALL);
                if (preferredCount != 6 || branchCount != 0) {
                    helper.fail("Clay item pipe delivered preferred/branch=" + preferredCount + "/" + branchCount);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void sandstonePipeRejectsInventoriesButTransportsThroughPipes(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos sandstonePos = new BlockPos(2, 1, 2);
        BlockPos exitPos = new BlockPos(3, 1, 2);
        BlockPos rejectedChestPos = new BlockPos(2, 1, 1);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(rejectedChestPos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "sandstone entry pipe");
            installPipe(helper, sandstonePos, BCTransportItems.PIPE_ITEM_SAND_STONE.get(), "sandstone item pipe");
            installPipe(helper, exitPos, BCTransportItems.PIPE_ITEM_STONE.get(), "sandstone exit pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder sandstone = requireInstalledPipe(helper, sandstonePos, "sandstone item pipe");
            if (sandstone == null) return;
            if (sandstone.getPipe().getConnectedType(Direction.NORTH) == ConnectedType.TILE
                || sandstone.getPipe().isConnected(Direction.NORTH)) {
                helper.fail("Sandstone item pipe incorrectly connected to an inventory");
                return;
            }
            if (sandstone.getPipe().getConnectedType(Direction.WEST) != ConnectedType.PIPE
                || sandstone.getPipe().getConnectedType(Direction.EAST) != ConnectedType.PIPE) {
                helper.fail("Sandstone item pipe did not connect its pipe-only route");
                return;
            }
            PipeFlowItems flow = (PipeFlowItems) sandstone.getPipe().getFlow();
            ItemStack remainder = flow.injectItem(new ItemStack(Items.SAND, 4), true, Direction.WEST, null, 0.08);
            if (!remainder.isEmpty()) {
                helper.fail("Sandstone item pipe refused a connected pipe ingress");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity rejected = requireChest(helper, rejectedChestPos, "rejected sandstone chest");
                ChestBlockEntity destination = requireChest(helper, destinationPos, "sandstone destination chest");
                if (rejected == null || destination == null) return;
                int rejectedCount = countItem(rejected, Items.SAND);
                int delivered = countItem(destination, Items.SAND);
                if (rejectedCount != 0 || delivered != 4) {
                    helper.fail("Sandstone route placed rejected/destination=" + rejectedCount + "/" + delivered);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void diamondWoodPipeExtractsOnlyItsWhitelistedItem(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos diamondWoodPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "diamond-wood source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.DIRT, 7));
            source.setItem(1, new ItemStack(Items.DIAMOND, 3));
            installPipe(helper, diamondWoodPos, BCTransportItems.PIPE_ITEM_DIAWOOD.get(), "diamond wooden item pipe");
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "diamond-wood output pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, diamondWoodPos, "diamond wooden item pipe");
            ChestBlockEntity source = requireChest(helper, sourcePos, "diamond-wood source chest");
            if (holder == null || source == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourWoodDiamond behaviour)) {
                helper.fail("Diamond wooden item pipe installed the wrong behaviour");
                return;
            }
            behaviour.filters.setStackInSlot(0, new ItemStack(Items.DIAMOND));
            long offered = 10 * BCTransportConfig.mjPerItem;
            long unused = behaviour.receivePower(offered, FluidAction.EXECUTE);
            if (unused != offered - BCTransportConfig.mjPerItem) {
                helper.fail("Diamond wooden pipe did not charge for exactly one filtered extraction");
                return;
            }
            if (countItem(source, Items.DIRT) != 7 || countItem(source, Items.DIAMOND) != 2) {
                helper.fail("Diamond wooden pipe extracted an item outside its one-item whitelist contract");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "diamond-wood destination chest");
                if (destination == null) return;
                if (countItem(destination, Items.DIAMOND) != 1 || countItem(destination, Items.DIRT) != 0) {
                    helper.fail("Diamond wooden pipe did not deliver exactly its whitelisted diamond");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void obsidianPipeCollectsCollidingItemAndDeliversIt(GameTestHelper helper) {
        BlockPos obsidianPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, obsidianPos, BCTransportItems.PIPE_ITEM_OBSIDIAN.get(), "obsidian item pipe");
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "obsidian output pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, obsidianPos, "obsidian item pipe");
            if (holder == null) return;
            if (!holder.getPipe().isConnected(Direction.EAST)) {
                helper.fail("Obsidian item pipe has no single connected output");
                return;
            }
            BlockPos absolute = helper.absolutePos(obsidianPos);
            ItemEntity entity = new ItemEntity(
                helper.getLevel(), absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5,
                new ItemStack(Items.COAL, 6)
            );
            if (!helper.getLevel().addFreshEntity(entity)) {
                helper.fail("Could not spawn the obsidian-pipe pickup fixture");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "obsidian destination chest");
                if (destination == null) return;
                int delivered = countItem(destination, Items.COAL);
                int loose = countItemEntities(helper, obsidianPos, Items.COAL, LOOSE_ITEM_SEARCH_RADIUS);
                if (delivered != 6 || loose != 0) {
                    helper.fail("Obsidian item pipe delivered/left loose=" + delivered + "/" + loose);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void voidPipeConsumesItemsAtItsCenterWithoutDroppingThem(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos voidPos = new BlockPos(2, 1, 2);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "void entry pipe");
            installPipe(helper, voidPos, BCTransportItems.PIPE_ITEM_VOID.get(), "void item pipe");
        });
        helper.runAfterDelay(6, () -> {
            PipeFlowItems flow = requireItemFlow(helper, voidPos, "void item pipe");
            if (flow == null) return;
            ItemStack remainder = flow.injectItem(
                new ItemStack(Items.ROTTEN_FLESH, 7), true, Direction.WEST, null, 0.08
            );
            if (!remainder.isEmpty()) {
                helper.fail("Void item pipe refused a connected ingress stack");
                return;
            }
            helper.succeedWhen(() -> {
                int dropped = countItemEntities(helper, voidPos, Items.ROTTEN_FLESH, LOOSE_ITEM_SEARCH_RADIUS);
                if (flow.doesContainItems() || dropped != 0) {
                    helper.fail("Void item pipe remained active or dropped " + dropped + " consumed items");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void goldPipeDispatchesLegacyAccelerationContract(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(2, 1, 2);
        helper.runAfterDelay(1, () ->
            installPipe(helper, pipePos, BCTransportItems.PIPE_ITEM_GOLD.get(), "gold item pipe")
        );
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, pipePos, "gold item pipe");
            PipeFlowItems flow = requireItemFlow(helper, pipePos, "gold item pipe");
            if (holder == null || flow == null) return;
            PipeEventItem.ItemEntry item = new PipeEventItem.ItemEntry(
                null, new ItemStack(Items.GOLD_NUGGET), Direction.WEST
            );
            PipeEventItem.ModifySpeed event = new PipeEventItem.ModifySpeed(holder, flow, item, 0.02);
            boolean handled = holder.fireEvent(event);
            if (!handled || Math.abs(event.targetSpeed - 0.25) > 1.0e-9
                || Math.abs(event.maxSpeedChange - 0.07) > 1.0e-9) {
                helper.fail("Gold item pipe acceleration was target/delta=" + event.targetSpeed + "/"
                    + event.maxSpeedChange + " instead of 0.25/0.07");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void lapisPipeRecoloursItemsAndPersistsItsSelectedColour(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(2, 1, 2);
        helper.runAfterDelay(1, () ->
            installPipe(helper, pipePos, BCTransportItems.PIPE_ITEM_LAPIS.get(), "lapis item pipe")
        );
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, pipePos, "lapis item pipe");
            PipeFlowItems flow = requireItemFlow(helper, pipePos, "lapis item pipe");
            if (holder == null || flow == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourLapis behaviour)) {
                helper.fail("Lapis item pipe installed the wrong behaviour");
                return;
            }
            activateAction(holder, BCTransportStatements.ACTION_PIPE_COLOUR[DyeColor.BLUE.ordinal()]);
            PipeEventItem.ReachCenter event = new PipeEventItem.ReachCenter(
                holder, flow, null, new ItemStack(Items.PAPER), Direction.WEST
            );
            holder.fireEvent(event);
            PipeBehaviourLapis restored = new PipeBehaviourLapis(holder.getPipe(), behaviour.writeToNbt());
            if (event.colour != DyeColor.BLUE || behaviour.getTextureIndex(null) != DyeColor.BLUE.getId()
                || restored.getTextureIndex(null) != DyeColor.BLUE.getId()) {
                helper.fail("Lapis item pipe failed to recolour or persist its blue selection");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void daizuliPipeRoutesMatchingColourToItsSelectedOutput(GameTestHelper helper) {
        BlockPos entryPos = new BlockPos(1, 1, 2);
        BlockPos daizuliPos = new BlockPos(2, 1, 2);
        BlockPos selectedChestPos = new BlockPos(2, 1, 1);
        BlockPos fallbackChestPos = new BlockPos(3, 1, 2);
        helper.setBlock(selectedChestPos, Blocks.CHEST);
        helper.setBlock(fallbackChestPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, entryPos, BCTransportItems.PIPE_ITEM_STONE.get(), "daizuli entry pipe");
            installPipe(helper, daizuliPos, BCTransportItems.PIPE_ITEM_DAIZULI.get(), "daizuli item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, daizuliPos, "daizuli item pipe");
            if (holder == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourDaizuli behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Daizuli item pipe installed the wrong behaviour or flow");
                return;
            }
            activateAction(holder, BCTransportStatements.ACTION_PIPE_DIRECTION[Direction.NORTH.ordinal()]);
            activateAction(holder, BCTransportStatements.ACTION_PIPE_COLOUR[DyeColor.BLUE.ordinal()]);
            if (behaviour.getCurrentDir() != Direction.NORTH
                || behaviour.getTextureIndex(Direction.NORTH) != DyeColor.BLUE.getId()
                || behaviour.getTextureIndex(Direction.EAST) != 16) {
                helper.fail("Daizuli item pipe did not retain its blue north-output configuration");
                return;
            }
            ItemStack matchingRemainder = flow.injectItem(
                new ItemStack(Items.IRON_INGOT, 4), true, Direction.WEST, DyeColor.BLUE, 0.08
            );
            ItemStack fallbackRemainder = flow.injectItem(
                new ItemStack(Items.GOLD_INGOT, 3), true, Direction.WEST, DyeColor.RED, 0.08
            );
            if (!matchingRemainder.isEmpty() || !fallbackRemainder.isEmpty()) {
                helper.fail("Daizuli item pipe refused a connected coloured ingress stack");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity selected = requireChest(helper, selectedChestPos, "daizuli selected chest");
                ChestBlockEntity fallback = requireChest(helper, fallbackChestPos, "daizuli fallback chest");
                if (selected == null || fallback == null) return;
                int selectedIron = countItem(selected, Items.IRON_INGOT);
                int selectedGold = countItem(selected, Items.GOLD_INGOT);
                int fallbackIron = countItem(fallback, Items.IRON_INGOT);
                int fallbackGold = countItem(fallback, Items.GOLD_INGOT);
                if (selectedIron != 4 || selectedGold != 0 || fallbackIron != 0 || fallbackGold != 3
                    || flow.doesContainItems()) {
                    helper.fail("Daizuli route selected(iron/gold)=" + selectedIron + "/" + selectedGold
                        + ", fallback(iron/gold)=" + fallbackIron + "/" + fallbackGold);
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void emzuliPresetsGateAlternateFilteredExtractionsAndApplyColours(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos emzuliPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "emzuli source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.DIAMOND, 2));
            source.setItem(1, new ItemStack(Items.EMERALD, 2));
            installPipe(helper, emzuliPos, BCTransportItems.PIPE_ITEM_EMZULI.get(), "emzuli item pipe");
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "emzuli output pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, emzuliPos, "emzuli item pipe");
            ChestBlockEntity source = requireChest(helper, sourcePos, "emzuli source chest");
            if (holder == null || source == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourEmzuli behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Emzuli item pipe installed the wrong behaviour or flow");
                return;
            }
            behaviour.invFilters.setStackInSlot(SlotIndex.SQUARE.ordinal(), new ItemStack(Items.DIAMOND));
            behaviour.invFilters.setStackInSlot(SlotIndex.CIRCLE.ordinal(), new ItemStack(Items.EMERALD));
            behaviour.slotColours.put(SlotIndex.SQUARE, DyeColor.RED);
            behaviour.slotColours.put(SlotIndex.CIRCLE, DyeColor.GREEN);

            long inactiveOffer = 2 * BCTransportConfig.mjPerItem;
            long inactiveUnused = behaviour.receivePower(inactiveOffer, FluidAction.EXECUTE);
            if (inactiveUnused != inactiveOffer || countItem(source, Items.DIAMOND) != 2
                || countItem(source, Items.EMERALD) != 2) {
                helper.fail("Inactive Emzuli presets extracted items or consumed power");
                return;
            }

            activateAction(
                holder, BCTransportStatements.ACTION_EXTRACTION_PRESET[SlotIndex.SQUARE.ordinal()]
            );
            activateAction(
                holder, BCTransportStatements.ACTION_EXTRACTION_PRESET[SlotIndex.CIRCLE.ordinal()]
            );
            long firstUnused = behaviour.receivePower(BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            long secondUnused = behaviour.receivePower(BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            if (firstUnused != 0 || secondUnused != 0
                || countItem(source, Items.DIAMOND) != 1 || countItem(source, Items.EMERALD) != 1
                || countTravellingItemWithColour(flow, Items.DIAMOND, DyeColor.RED) != 1
                || countTravellingItemWithColour(flow, Items.EMERALD, DyeColor.GREEN) != 1) {
                helper.fail("Emzuli presets failed to alternate one red diamond and one green emerald");
                return;
            }

            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "emzuli destination chest");
                if (destination == null) return;
                if (countItem(destination, Items.DIAMOND) != 1
                    || countItem(destination, Items.EMERALD) != 1 || flow.doesContainItems()) {
                    helper.fail("Emzuli preset extraction did not deliver exactly one filtered item per preset");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void diamondWoodBlacklistRejectsItsConfiguredItem(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos diamondWoodPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "blacklist source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.DIAMOND, 3));
            source.setItem(1, new ItemStack(Items.DIRT, 2));
            installPipe(
                helper, diamondWoodPos, BCTransportItems.PIPE_ITEM_DIAWOOD.get(), "blacklist diamond wooden pipe"
            );
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "blacklist output pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, diamondWoodPos, "blacklist diamond wooden pipe");
            ChestBlockEntity source = requireChest(helper, sourcePos, "blacklist source chest");
            if (holder == null || source == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourWoodDiamond behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Blacklist fixture installed the wrong diamond wooden behaviour or flow");
                return;
            }
            behaviour.filters.setStackInSlot(0, new ItemStack(Items.DIAMOND));
            behaviour.filterMode = FilterMode.BLACK_LIST;
            long unused = behaviour.receivePower(BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            if (unused != 0 || countItem(source, Items.DIAMOND) != 3 || countItem(source, Items.DIRT) != 1
                || countTravellingItem(flow, Items.DIRT) != 1) {
                helper.fail("Diamond wooden blacklist did not extract exactly one non-blacklisted dirt");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "blacklist destination chest");
                if (destination == null) return;
                if (countItem(destination, Items.DIRT) != 1 || countItem(destination, Items.DIAMOND) != 0
                    || flow.doesContainItems()) {
                    helper.fail("Diamond wooden blacklist delivered a blocked item or lost its allowed item");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void diamondWoodRoundRobinAlternatesConfiguredFilters(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos diamondWoodPos = new BlockPos(2, 1, 2);
        BlockPos stonePos = new BlockPos(3, 1, 2);
        BlockPos destinationPos = new BlockPos(4, 1, 2);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.runAfterDelay(1, () -> {
            ChestBlockEntity source = requireChest(helper, sourcePos, "round-robin source chest");
            if (source == null) return;
            source.setItem(0, new ItemStack(Items.DIAMOND, 2));
            source.setItem(1, new ItemStack(Items.DIRT, 2));
            installPipe(
                helper, diamondWoodPos, BCTransportItems.PIPE_ITEM_DIAWOOD.get(), "round-robin diamond wooden pipe"
            );
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "round-robin output pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(
                helper, diamondWoodPos, "round-robin diamond wooden pipe"
            );
            ChestBlockEntity source = requireChest(helper, sourcePos, "round-robin source chest");
            if (holder == null || source == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourWoodDiamond behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Round-robin fixture installed the wrong diamond wooden behaviour or flow");
                return;
            }
            behaviour.filters.setStackInSlot(0, new ItemStack(Items.DIAMOND));
            behaviour.filters.setStackInSlot(1, new ItemStack(Items.DIRT));
            behaviour.filterMode = FilterMode.ROUND_ROBIN;
            long firstUnused = behaviour.receivePower(BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            long secondUnused = behaviour.receivePower(BCTransportConfig.mjPerItem, FluidAction.EXECUTE);
            if (firstUnused != 0 || secondUnused != 0
                || countItem(source, Items.DIAMOND) != 1 || countItem(source, Items.DIRT) != 1
                || countTravellingItem(flow, Items.DIAMOND) != 1 || countTravellingItem(flow, Items.DIRT) != 1
                || behaviour.currentFilter != 0 || !behaviour.filterValid) {
                helper.fail("Diamond wooden round-robin failed to extract one item from each configured filter");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "round-robin destination chest");
                if (destination == null) return;
                if (countItem(destination, Items.DIAMOND) != 1 || countItem(destination, Items.DIRT) != 1
                    || flow.doesContainItems()) {
                    helper.fail("Diamond wooden round-robin did not deliver its two alternating items");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 260)
    public static void poweredStripesPipeBreaksSelectedBlockAndReturnsItsDrop(GameTestHelper helper) {
        BlockPos destinationPos = new BlockPos(1, 1, 2);
        BlockPos stonePos = new BlockPos(2, 1, 2);
        BlockPos stripesPos = new BlockPos(3, 1, 2);
        BlockPos targetPos = new BlockPos(4, 1, 2);
        helper.setBlock(destinationPos, Blocks.CHEST);
        helper.setBlock(targetPos, Blocks.STONE);
        helper.runAfterDelay(1, () -> {
            installPipe(helper, stonePos, BCTransportItems.PIPE_ITEM_STONE.get(), "stripes return pipe");
            installPipe(helper, stripesPos, BCTransportItems.PIPE_ITEM_STRIPES.get(), "stripes item pipe");
        });
        helper.runAfterDelay(6, () -> {
            TilePipeHolder holder = requireInstalledPipe(helper, stripesPos, "stripes item pipe");
            if (holder == null) return;
            if (!(holder.getPipe().getBehaviour() instanceof PipeBehaviourStripes behaviour)
                || !(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
                helper.fail("Stripes item pipe installed the wrong behaviour or flow");
                return;
            }
            activateAction(holder, BCTransportStatements.ACTION_PIPE_DIRECTION[Direction.EAST.ordinal()]);
            if (behaviour.direction != Direction.EAST) {
                helper.fail("Stripes direction action did not select its east target");
                return;
            }
            long targetPower = buildcraft.lib.misc.BlockUtil.computeBlockBreakPower(
                helper.getLevel(), helper.absolutePos(targetPos));
            long offered = targetPower;
            long unused = behaviour.receivePower(offered, FluidAction.EXECUTE);
            if (unused != 0) {
                helper.fail("Stripes item pipe refused " + unused + " of its offered breaking power");
                return;
            }
            helper.succeedWhen(() -> {
                ChestBlockEntity destination = requireChest(helper, destinationPos, "stripes destination chest");
                if (destination == null) return;
                int delivered = countItem(destination, Items.COBBLESTONE);
                int loose = countItemEntities(
                    helper, stripesPos, Items.COBBLESTONE, LOOSE_ITEM_SEARCH_RADIUS
                );
                if (!helper.getBlockState(targetPos).isAir() || delivered != 1 || loose != 0
                    || flow.doesContainItems()) {
                    helper.fail("Stripes break result air/delivered/loose/flow=" + helper.getBlockState(targetPos).isAir()
                        + "/" + delivered + "/" + loose + "/" + flow.doesContainItems());
                }
            });
        });
    }

    private static void activateAction(TilePipeHolder holder, IAction action) {
        holder.fireEvent(new PipeEventActionActivate(
            holder, action, new IStatementParameter[0], EnumPipePart.CENTER
        ));
    }

    private static int countTravellingItemWithColour(
        PipeFlowItems flow, Item item, DyeColor expectedColour
    ) {
        int count = 0;
        ListTag items = flow.writeToNbt().getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag itemTag = items.getCompound(index);
            ItemStack stack = ItemStack.of(itemTag.getCompound("stack"));
            int encodedColour = itemTag.getByte("colour");
            DyeColor colour = encodedColour == 0 ? null : DyeColor.byId(encodedColour - 1);
            if (stack.is(item) && colour == expectedColour) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static TilePipeHolder installPipe(
        GameTestHelper helper, BlockPos position, ItemPipeHolder pipeItem, String description
    ) {
        helper.setBlock(position, BCTransportBlocks.pipeHolder.get());
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!(blockEntity instanceof TilePipeHolder holder)) {
            helper.fail("Missing pipe holder for " + description + " at " + position);
            return null;
        }
        holder.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        if (holder.getPipe() == Pipe.EMPTY) {
            helper.fail(description + " did not install into its pipe holder");
            return null;
        }
        return holder;
    }

    private static TilePipeHolder requireInstalledPipe(GameTestHelper helper, BlockPos position, String description) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!(blockEntity instanceof TilePipeHolder holder) || holder.getPipe() == Pipe.EMPTY) {
            helper.fail("Missing installed " + description + " at " + position);
            return null;
        }
        return holder;
    }

    private static PipeFlowItems requireItemFlow(GameTestHelper helper, BlockPos position, String description) {
        TilePipeHolder holder = requireInstalledPipe(helper, position, description);
        if (holder == null) return null;
        if (!(holder.getPipe().getFlow() instanceof PipeFlowItems flow)) {
            helper.fail(description + " does not contain an item flow");
            return null;
        }
        return flow;
    }

    private static ChestBlockEntity requireChest(GameTestHelper helper, BlockPos position, String description) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            helper.fail("Missing " + description + " at " + position);
            return null;
        }
        return chest;
    }

    private static int countItem(Container container, Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int countTravellingItem(PipeFlowItems flow, Item item) {
        int count = 0;
        ListTag items = flow.writeToNbt().getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = ItemStack.of(items.getCompound(index).getCompound("stack"));
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int countItemEntities(
        GameTestHelper helper, BlockPos relativeCenter, Item item, double radius
    ) {
        int count = 0;
        AABB area = new AABB(helper.absolutePos(relativeCenter)).inflate(radius);
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
            if (!entity.isRemoved() && entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        return count;
    }

    private record ItemPipeCase(
        ItemPipeHolder item, String registryPath, PipeDefinition definition,
        Class<? extends PipeBehaviour> behaviourType
    ) {
    }
}
