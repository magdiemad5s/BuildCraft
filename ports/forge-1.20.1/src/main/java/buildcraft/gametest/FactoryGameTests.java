/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockWaterGel.GelStage;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.factory.tile.TileFloodGate;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TilePump;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tick-driven runtime contracts for the core BuildCraft Factory machines.
 *
 * <p>Every test drives the registered block and its real server ticker. Direct calls to machine update methods are
 * intentionally avoided.</p>
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class FactoryGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private FactoryGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void poweredPumpExtractsARealWaterSource(GameTestHelper helper) {
        BlockPos pumpPos = new BlockPos(2, 3, 2);
        BlockPos sourcePos = new BlockPos(2, 1, 2);
        helper.setBlock(sourcePos, Blocks.WATER);
        helper.setBlock(pumpPos, BCFactoryBlocks.PUMP_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(pumpPos) instanceof TilePump pump)) {
                helper.fail("Pump block entity was not created");
                return;
            }
            IMjReceiver receiver = pump.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Pump did not expose its MJ receiver capability");
                return;
            }
            long offered = 20 * MjAPI.MJ;
            long unused = receiver.receivePower(offered, FluidAction.EXECUTE);
            if (unused != 0) {
                helper.fail("Pump rejected " + unused + " of the " + offered + " microjoules offered");
                return;
            }

            helper.succeedWhen(() -> {
                if (!(helper.getBlockEntity(pumpPos) instanceof TilePump tickingPump)) {
                    helper.fail("Pump block entity disappeared while extracting water");
                    return;
                }
                IFluidHandler fluids = tickingPump.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
                if (fluids == null) {
                    helper.fail("Pump lost its Forge fluid capability");
                    return;
                }
                FluidStack stored = fluids.getFluidInTank(0);
                if (stored.getFluid() != Fluids.WATER || stored.getAmount() != 1_000) {
                    helper.fail("Pump contains " + stored.getAmount() + " mB instead of one bucket of water");
                    return;
                }
                if (!helper.getLevel().getFluidState(helper.absolutePos(sourcePos)).isEmpty()) {
                    helper.fail("Pump filled its tank without consuming the finite water source");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void floodGateConservesFluidAndHonorsRedstoneAndBlockedOutput(GameTestHelper helper) {
        BlockPos gatePos = new BlockPos(2, 3, 2);
        BlockPos outputPos = gatePos.below();
        BlockPos redstonePos = gatePos.above();
        helper.setBlock(gatePos, BCFactoryBlocks.FLOOD_GATE_BLOCK.get());
        helper.setBlock(redstonePos, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(gatePos) instanceof TileFloodGate gate)) {
                helper.fail("Flood Gate block entity was not created");
                return;
            }
            gate.openSides.clear();
            gate.openSides.add(Direction.DOWN);
            gate.syncBlockStateFromOpenSides();

            IFluidHandler fluids = gate.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (fluids == null) {
                helper.fail("Flood Gate did not expose its Forge fluid capability");
                return;
            }
            int accepted = fluids.fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
            if (accepted != 1_000) {
                helper.fail("Flood Gate accepted " + accepted + " mB instead of one bucket of water");
                return;
            }
            if (!helper.getLevel().hasNeighborSignal(helper.absolutePos(gatePos))) {
                helper.fail("Flood Gate redstone test setup did not power the machine");
            }
        });

        helper.runAfterDelay(30, () -> {
            if (!(helper.getBlockEntity(gatePos) instanceof TileFloodGate gate)) {
                helper.fail("Flood Gate disappeared during its redstone-disabled interval");
                return;
            }
            IFluidHandler fluids = gate.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (fluids == null || fluids.getFluidInTank(0).getAmount() != 1_000) {
                helper.fail("Powered Flood Gate consumed fluid while redstone-disabled");
                return;
            }
            if (!helper.getLevel().getFluidState(helper.absolutePos(outputPos)).isEmpty()) {
                helper.fail("Powered Flood Gate placed fluid despite its redstone signal");
                return;
            }

            helper.setBlock(outputPos, Blocks.STONE);
            helper.setBlock(redstonePos, Blocks.AIR);
        });

        helper.runAfterDelay(70, () -> {
            if (!(helper.getBlockEntity(gatePos) instanceof TileFloodGate gate)) {
                helper.fail("Flood Gate disappeared while its only output was blocked");
                return;
            }
            IFluidHandler fluids = gate.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (fluids == null || fluids.getFluidInTank(0).getAmount() != 1_000) {
                helper.fail("Flood Gate deleted fluid while its only output was blocked");
                return;
            }
            if (!helper.getBlockState(outputPos).is(Blocks.STONE)) {
                helper.fail("Flood Gate replaced the solid block obstructing its only open side");
                return;
            }

            helper.setBlock(outputPos, Blocks.AIR);
            helper.succeedWhen(() -> {
                if (!(helper.getBlockEntity(gatePos) instanceof TileFloodGate tickingGate)) {
                    helper.fail("Flood Gate disappeared before placing its stored fluid");
                    return;
                }
                IFluidHandler tickingFluids
                    = tickingGate.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
                if (tickingFluids == null) {
                    helper.fail("Flood Gate lost its Forge fluid capability");
                    return;
                }
                var placed = helper.getLevel().getFluidState(helper.absolutePos(outputPos));
                if (placed.getType() != Fluids.WATER || !placed.isSource()) {
                    helper.fail("Flood Gate has not placed a water source through its open side");
                    return;
                }
                if (tickingFluids.getFluidInTank(0).getAmount() != 0) {
                    helper.fail("Flood Gate duplicated water instead of consuming its stored bucket");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void poweredMiningWellMinesDownwardAndOutputsItsDrop(GameTestHelper helper) {
        BlockPos wellPos = new BlockPos(2, 3, 2);
        BlockPos targetPos = wellPos.below();
        BlockPos chestPos = wellPos.east();
        helper.setBlock(targetPos, Blocks.STONE);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(wellPos, BCFactoryBlocks.MINING_WELL_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(wellPos) instanceof TileMiningWell well)) {
                helper.fail("Mining Well block entity was not created");
                return;
            }
            IMjReceiver receiver = well.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Mining Well did not expose its MJ receiver capability");
                return;
            }
            long offered = 100 * MjAPI.MJ;
            long unused = receiver.receivePower(offered, FluidAction.EXECUTE);
            if (unused != 0) {
                helper.fail("Mining Well rejected " + unused + " of the " + offered + " microjoules offered");
                return;
            }

            helper.succeedWhen(() -> {
                if (helper.getBlockState(targetPos).is(Blocks.STONE)) {
                    helper.fail("Mining Well has not mined the block directly below it");
                    return;
                }
                if (!(helper.getBlockEntity(chestPos) instanceof Container chest)) {
                    helper.fail("Mining Well output chest disappeared");
                    return;
                }
                int cobblestone = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    if (stack.is(Items.COBBLESTONE)) {
                        cobblestone += stack.getCount();
                    }
                }
                if (cobblestone != 1) {
                    helper.fail("Mining Well output " + cobblestone + " cobblestone instead of exactly one");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void autoWorkbenchCraftsAfterBlockEntitySaveLoad(GameTestHelper helper) {
        BlockPos workbenchPos = new BlockPos(2, 1, 2);
        helper.setBlock(workbenchPos, BCFactoryBlocks.AUTO_BENCH_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(workbenchPos) instanceof TileAutoWorkbenchItems workbench)) {
                helper.fail("Auto Workbench block entity was not created");
                return;
            }
            workbench.invBlueprint.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
            workbench.invMaterials.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        });

        helper.runAfterDelay(5, () -> {
            if (!(helper.getBlockEntity(workbenchPos) instanceof TileAutoWorkbenchItems original)) {
                helper.fail("Auto Workbench disappeared before its save/load cycle");
                return;
            }
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(workbenchPos);
            helper.getLevel().removeBlockEntity(absolutePos);

            TileAutoWorkbenchItems restored
                = new TileAutoWorkbenchItems(absolutePos, helper.getBlockState(workbenchPos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(8, () -> {
            if (!(helper.getBlockEntity(workbenchPos) instanceof TileAutoWorkbenchItems restored)) {
                helper.fail("Reloaded Auto Workbench block entity was not installed");
                return;
            }
            if (!restored.invBlueprint.getStackInSlot(0).is(Items.OAK_LOG)
                || !restored.invMaterials.getStackInSlot(0).is(Items.OAK_LOG)) {
                helper.fail("Auto Workbench lost its blueprint or material during save/load");
                return;
            }
            IMjReceiver receiver = restored.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Reloaded Auto Workbench did not expose its MJ receiver capability");
                return;
            }
            long requested = receiver.getPowerRequested();
            long unused = receiver.receivePower(requested, FluidAction.EXECUTE);
            if (unused != 0) {
                helper.fail("Reloaded Auto Workbench rejected " + unused + " microjoules of requested power");
                return;
            }

            helper.succeedWhen(() -> {
                if (!(helper.getBlockEntity(workbenchPos) instanceof TileAutoWorkbenchItems tickingWorkbench)) {
                    helper.fail("Auto Workbench disappeared while crafting");
                    return;
                }
                ItemStack output = tickingWorkbench.invResult.getStackInSlot(0);
                if (!output.is(Items.OAK_PLANKS) || output.getCount() != 4) {
                    helper.fail("Auto Workbench output is " + output + " instead of four oak planks");
                    return;
                }
                if (!tickingWorkbench.invMaterials.getStackInSlot(0).isEmpty()) {
                    helper.fail("Auto Workbench crafted without consuming its saved material");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 140)
    public static void poweredDistillerProcessesOilIntoBothOutputs(GameTestHelper helper) {
        BlockPos distillerPos = new BlockPos(2, 1, 2);
        helper.setBlock(distillerPos, BCFactoryBlocks.DISTILLER_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(distillerPos) instanceof TileDistiller_BC8 distiller)) {
                helper.fail("Distiller block entity was not created");
                return;
            }
            IFluidHandler input =
                distiller.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElse(null);
            if (input == null) {
                helper.fail("Distiller did not expose its horizontal input tank");
                return;
            }
            FluidStack oil = new FluidStack(BCEnergyFluids.crudeOil[0], 80);
            if (input.fill(oil, FluidAction.EXECUTE) != oil.getAmount()) {
                helper.fail("Distiller rejected its registered cool crude-oil recipe input");
                return;
            }
            IMjReceiver receiver = distiller.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Distiller did not expose its MJ receiver capability");
                return;
            }
            long offered = 512 * MjAPI.MJ;
            long unused = receiver.receivePower(offered, FluidAction.EXECUTE);
            if (unused != 0) {
                helper.fail("Distiller rejected " + unused + " microjoules from an empty battery");
                return;
            }

            helper.succeedWhen(() -> {
                if (!(helper.getBlockEntity(distillerPos) instanceof TileDistiller_BC8 tickingDistiller)) {
                    helper.fail("Distiller disappeared while processing");
                    return;
                }
                var recipe = BuildcraftRecipeRegistry.refineryRecipes
                    .getDistillationRegistry()
                    .getRecipeForInput(new FluidStack(BCEnergyFluids.crudeOil[0], 80));
                if (recipe == null) {
                    helper.fail("Cool crude oil lost its registered distillation recipe");
                    return;
                }

                int remaining = tickingDistiller.tankIn.getFluidAmount();
                int consumed = 80 - remaining;
                if (consumed <= 0 || consumed % recipe.in().getAmount() != 0) {
                    helper.fail("Distiller consumed an invalid " + consumed + " mB of recipe input");
                    return;
                }
                int completed = consumed / recipe.in().getAmount();
                FluidStack gas = tickingDistiller.tankGasOut.getFluid();
                FluidStack liquid = tickingDistiller.tankLiquidOut.getFluid();
                if (!gas.isFluidEqual(recipe.outGas())
                    || gas.getAmount() != completed * recipe.outGas().getAmount()) {
                    helper.fail("Distiller gas output does not match completed recipe operations");
                    return;
                }
                if (!liquid.isFluidEqual(recipe.outLiquid())
                    || liquid.getAmount() != completed * recipe.outLiquid().getAmount()) {
                    helper.fail("Distiller liquid output does not match completed recipe operations");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void threeBlockHeatExchangerProcessesBothFluidStreams(GameTestHelper helper) {
        BlockPos endPos = new BlockPos(1, 1, 2);
        BlockPos middlePos = endPos.east();
        BlockPos startPos = middlePos.east();
        helper.setBlock(endPos, BCFactoryBlocks.HEATEXCHANGE_BLOCK.get());
        helper.setBlock(middlePos, BCFactoryBlocks.HEATEXCHANGE_BLOCK.get());
        helper.setBlock(startPos, BCFactoryBlocks.HEATEXCHANGE_BLOCK.get());

        helper.runAfterDelay(4, () -> {
            if (!(helper.getBlockEntity(startPos) instanceof TileHeatExchange start)
                || !(helper.getBlockEntity(endPos) instanceof TileHeatExchange end)) {
                helper.fail("Heat-exchanger endpoints were not created");
                return;
            }
            if (!start.isStart() || !end.isEnd()) {
                helper.fail("Three aligned Heat Exchangers did not form start, middle, and end sections");
                return;
            }

            IFluidHandler heatantInput =
                start.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).orElse(null);
            IFluidHandler coolantInput =
                end.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).orElse(null);
            if (heatantInput == null || coolantInput == null) {
                helper.fail("Formed Heat Exchanger did not expose both directional input capabilities");
                return;
            }
            if (heatantInput.fill(new FluidStack(BCEnergyFluids.crudeOil[0], 100), FluidAction.EXECUTE) != 100) {
                helper.fail("Heat Exchanger rejected cool crude oil from its heatant input");
                return;
            }
            if (coolantInput.fill(new FluidStack(BCEnergyFluids.crudeOil[2], 100), FluidAction.EXECUTE) != 100) {
                helper.fail("Heat Exchanger rejected searing crude oil from its coolant input");
            }
        });

        helper.runAfterDelay(130, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(startPos) instanceof TileHeatExchange start)
                || !(helper.getBlockEntity(endPos) instanceof TileHeatExchange end)) {
                helper.fail("Heat-exchanger endpoints disappeared while processing");
                return;
            }
            FluidStack coolInput = start.getSectionTank(0).getFluid();
            FluidStack heatedOutput = start.getSectionTank(1).getFluid();
            FluidStack searingInput = end.getSectionTank(2).getFluid();
            FluidStack cooledOutput = end.getSectionTank(3).getFluid();

            if (coolInput.getAmount() >= 100 || searingInput.getAmount() >= 100) {
                helper.fail("Heat Exchanger reached running state without consuming both input streams");
                return;
            }
            if (heatedOutput.getFluid() != BCEnergyFluids.crudeOil[1] || heatedOutput.getAmount() <= 0) {
                helper.fail("Heat Exchanger did not produce hot crude oil from its cool input");
                return;
            }
            if (cooledOutput.getFluid() != BCEnergyFluids.crudeOil[1] || cooledOutput.getAmount() <= 0) {
                helper.fail("Heat Exchanger did not produce hot crude oil from its searing input");
                return;
            }
            if (100 - coolInput.getAmount() != heatedOutput.getAmount()
                || 100 - searingInput.getAmount() != cooledOutput.getAmount()) {
                helper.fail("Heat Exchanger did not conserve both fluid streams");
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void upwardChuteCollectsPersistsAndTransfersLooseItems(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(2, 1, 2);
        BlockPos chestPos = chutePos.east();
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(
            chutePos,
            BCFactoryBlocks.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(BuildCraftProperties.BLOCK_FACING_6, Direction.UP)
        );

        helper.runAfterDelay(3, () -> {
            if (!(helper.getBlockEntity(chutePos) instanceof TileChute chute)) {
                helper.fail("Chute block entity was not created");
                return;
            }
            if (!helper.getBlockState(chutePos).getValue(BlockChute.CONNECTED_MAP.get(Direction.EAST))) {
                helper.fail("Chute did not expose its connected model state toward the adjacent chest");
                return;
            }

            BlockPos absolute = helper.absolutePos(chutePos);
            ItemEntity looseItems = new ItemEntity(
                helper.getLevel(),
                absolute.getX() + 0.5,
                absolute.getY() + 1.05,
                absolute.getZ() + 0.5,
                new ItemStack(Items.IRON_INGOT, 3)
            );
            if (!helper.getLevel().addFreshEntity(looseItems)) {
                helper.fail("Could not spawn the Chute pickup fixture");
            }
        });

        helper.runAfterDelay(55, () -> {
            if (!(helper.getBlockEntity(chutePos) instanceof TileChute original)) {
                helper.fail("Chute disappeared before its save/load cycle");
                return;
            }
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolute = helper.absolutePos(chutePos);
            helper.getLevel().removeBlockEntity(absolute);
            TileChute restored = new TileChute(absolute, helper.getBlockState(chutePos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(60, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(chutePos) instanceof TileChute restored)) {
                helper.fail("Reloaded Chute block entity was not installed");
                return;
            }
            if (!(helper.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
                helper.fail("Chute output chest disappeared");
                return;
            }
            int delivered = countItem(chest, Items.IRON_INGOT);
            if (delivered != 3 || !restored.inv.getStackInSlot(0).isEmpty()
                || !restored.inv.getStackInSlot(1).isEmpty()
                || !restored.inv.getStackInSlot(2).isEmpty()
                || !restored.inv.getStackInSlot(3).isEmpty()) {
                helper.fail("Chute delivered " + delivered + " of 3 persisted loose items");
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 600)
    public static void waterGelUsesItsScheduledSpreadAndGellingLifecycle(GameTestHelper helper) {
        BlockPos spreadPos = new BlockPos(2, 2, 2);
        BlockPos[] waterSources = {
            spreadPos.north(),
            spreadPos.south(),
            spreadPos.east()
        };
        for (BlockPos source : waterSources) {
            helper.setBlock(source, Blocks.WATER);
        }
        helper.setBlock(
            spreadPos,
            BCFactoryBlocks.WATER_GEL.get().defaultBlockState()
                .setValue(BlockWaterGel.PROP_STAGE, GelStage.SPREAD_0)
        );

        BlockPos gellingPos = new BlockPos(6, 2, 2);
        helper.setBlock(
            gellingPos,
            BCFactoryBlocks.WATER_GEL.get().defaultBlockState()
                .setValue(BlockWaterGel.PROP_STAGE, GelStage.GELLING_0)
        );

        helper.getLevel().scheduleTick(
            helper.absolutePos(spreadPos),
            BCFactoryBlocks.WATER_GEL.get(),
            1
        );
        helper.getLevel().scheduleTick(
            helper.absolutePos(gellingPos),
            BCFactoryBlocks.WATER_GEL.get(),
            1
        );

        helper.runAfterDelay(4, () -> {
            if (helper.getBlockState(spreadPos).getValue(BlockWaterGel.PROP_STAGE) != GelStage.SPREAD_1) {
                helper.fail("Scheduled Water Gel tick did not advance the source to spread stage 1");
                return;
            }
            for (BlockPos source : waterSources) {
                if (!helper.getBlockState(source).is(BCFactoryBlocks.WATER_GEL.get())
                    || helper.getBlockState(source).getValue(BlockWaterGel.PROP_STAGE) != GelStage.SPREAD_1) {
                    helper.fail("Water Gel did not replace exactly the reachable source-water fixture");
                    return;
                }
            }
            if (helper.getBlockState(gellingPos).getValue(BlockWaterGel.PROP_STAGE) != GelStage.GELLING_1) {
                helper.fail("Scheduled Water Gel tick did not advance gelling stage 0");
                return;
            }
            // The first scheduled tick already queued the released 400-549 tick gelling delay. Scheduling the same
            // block/type again does not replace that pending tick in modern LevelTicks, so await the real lifecycle.
            helper.succeedWhen(() -> {
                if (helper.getBlockState(gellingPos).getValue(BlockWaterGel.PROP_STAGE) != GelStage.GEL) {
                    helper.fail("Water Gel did not complete its scheduled gelling lifecycle");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void minerTubeIsProtectedCleanedAndCannotScanPastAnOrphanColumn(GameTestHelper helper) {
        BlockPos wellPos = new BlockPos(2, 4, 2);
        BlockPos upperTube = wellPos.below();
        BlockPos lowerTube = upperTube.below();
        BlockPos target = lowerTube.below();
        helper.setBlock(target, Blocks.STONE);
        helper.setBlock(upperTube, BCFactoryBlocks.TUBE_BLOCK.get());
        helper.setBlock(lowerTube, BCFactoryBlocks.TUBE_BLOCK.get());
        helper.setBlock(wellPos, BCFactoryBlocks.MINING_WELL_BLOCK.get());

        BlockPos orphanTube = new BlockPos(6, 2, 2);
        helper.setBlock(orphanTube, BCFactoryBlocks.TUBE_BLOCK.get());

        helper.runAfterDelay(3, () -> {
            BlockTube tube = (BlockTube) BCFactoryBlocks.TUBE_BLOCK.get();
            boolean removed = tube.onDestroyedByPlayer(
                helper.getBlockState(upperTube),
                helper.getLevel(),
                helper.absolutePos(upperTube),
                helper.makeMockPlayer(),
                true,
                Fluids.EMPTY.defaultFluidState()
            );
            if (removed || !helper.getBlockState(upperTube).is(tube)) {
                helper.fail("A player could remove a Tube still owned by its Mining Well");
                return;
            }

            // This represents a removed fluid below an orphaned Tube. The early port scanned upward forever when no
            // Pump or Miner terminated the column.
            tube.neighborChanged(
                helper.getBlockState(orphanTube),
                helper.getLevel(),
                helper.absolutePos(orphanTube),
                Blocks.WATER,
                helper.absolutePos(orphanTube.below()),
                false
            );
            helper.setBlock(wellPos, Blocks.AIR);
        });

        helper.runAfterDelay(8, () -> {
            if (!helper.getBlockState(upperTube).isAir() || !helper.getBlockState(lowerTube).isAir()) {
                helper.fail("Removing a Mining Well left its owned Tube column behind");
                return;
            }
            if (!helper.getBlockState(orphanTube).is(BCFactoryBlocks.TUBE_BLOCK.get())) {
                helper.fail("Orphan Tube neighbor handling unexpectedly destroyed the block");
                return;
            }
            helper.succeed();
        });
    }

    private static int countItem(Container container, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }}
