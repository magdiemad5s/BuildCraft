/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.blockEntity.TileEngineCreative;
import buildcraft.core.blockEntity.TileEngineRedstone_BC8;
import buildcraft.core.blockEntity.TilePowerConsumerTester;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.energy.tile.TileEngineStone_BC8;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileTank;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportConfig;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.PipeFlowRedstoneFlux;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated runtime contracts for BuildCraft transport.
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class TransportGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private TransportGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void fluidPipeFaceAdvertisesOneTankAndAcceptsFluid(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(1, 1, 1);
        BlockPos pipePos = tankPos.east();
        helper.setBlock(tankPos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(pipePos, BCTransportBlocks.pipeHolder.get());

        helper.runAfterDelay(1, () -> {
            if (!(helper.getBlockEntity(pipePos) instanceof TilePipeHolder pipeHolder)) {
                helper.fail("Fluid pipe holder block entity was not created");
                return;
            }
            pipeHolder.onPlacedBy(
                helper.makeMockPlayer(),
                new ItemStack(BCTransportItems.PIPE_FLUID_STONE.get())
            );
        });

        helper.runAfterDelay(4, () -> {
            if (!(helper.getBlockEntity(pipePos) instanceof TilePipeHolder pipeHolder)) {
                helper.fail("Fluid pipe holder disappeared before capability validation");
                return;
            }
            IFluidHandler handler = pipeHolder
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElse(null);
            if (handler == null) {
                helper.fail("Connected fluid pipe did not expose Forge's fluid capability");
                return;
            }
            if (handler.getTanks() != 1 || handler.getTankCapacity(0) <= 0) {
                helper.fail(
                    "Fluid pipe advertised " + handler.getTanks()
                        + " tanks with capacity " + handler.getTankCapacity(0)
                );
                return;
            }

            int expected = PipeApi.getFluidTransferInfo(BCTransportPipes.stoneFluid).transferPerTick;
            FluidStack water = new FluidStack(Fluids.WATER, Math.max(250, expected));
            if (!handler.isFluidValid(0, water)) {
                helper.fail("Connected fluid pipe rejected water during validity probing");
                return;
            }
            int accepted = handler.fill(water, FluidAction.EXECUTE);
            if (accepted != expected) {
                helper.fail("Connected fluid pipe accepted " + accepted + " mB instead of " + expected + " mB");
                return;
            }
            FluidStack stored = handler.getFluidInTank(0);
            if (stored.getFluid() != Fluids.WATER || stored.getAmount() != expected) {
                helper.fail("Fluid pipe tank query did not report the " + expected + " mB water transfer");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void poweredWoodenFluidPipeTransfersWaterBetweenTanks(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = sourcePos.east();
        BlockPos destinationPos = pipePos.east();
        helper.setBlock(sourcePos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(pipePos, BCTransportBlocks.pipeHolder.get());

        helper.runAfterDelay(1, () -> {
            if (!(helper.getBlockEntity(pipePos) instanceof TilePipeHolder pipeHolder)) {
                helper.fail("Wooden fluid pipe holder block entity was not created");
                return;
            }
            pipeHolder.onPlacedBy(
                helper.makeMockPlayer(),
                new ItemStack(BCTransportItems.PIPE_FLUID_WOOD.get())
            );
        });

        helper.runAfterDelay(5, () -> {
            if (!(helper.getBlockEntity(pipePos) instanceof TilePipeHolder pipeHolder)
                || !(pipeHolder.getPipe().getBehaviour() instanceof PipeBehaviourWood behaviour)) {
                helper.fail("Wooden fluid pipe was not installed in its holder");
                return;
            }
            if (behaviour.getCurrentDir() != Direction.WEST) {
                helper.fail("Wooden fluid pipe faced " + behaviour.getCurrentDir() + " instead of its source tank");
                return;
            }
            helper.setBlock(destinationPos, BCFactoryBlocks.TANK_BLOCK.get());
        });

        helper.runAfterDelay(7, () -> {
            if (!(helper.getBlockEntity(pipePos) instanceof TilePipeHolder pipeHolder)
                || !(pipeHolder.getPipe().getBehaviour() instanceof PipeBehaviourWood behaviour)) {
                helper.fail("Wooden fluid pipe disappeared before transfer");
                return;
            }
            if (behaviour.getCurrentDir() != Direction.WEST) {
                helper.fail("Wooden fluid pipe changed away from its source tank before transfer");
                return;
            }
            if (!(helper.getBlockEntity(sourcePos) instanceof TileTank sourceTank)
                || !(helper.getBlockEntity(destinationPos) instanceof TileTank destinationTank)) {
                helper.fail("BuildCraft source or destination tank block entity was not created");
                return;
            }

            IFluidHandler source = sourceTank
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST)
                .orElse(null);
            IFluidHandler destination = destinationTank
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElse(null);
            if (source == null || destination == null) {
                helper.fail("BuildCraft source or destination tank did not expose its connected fluid capability");
                return;
            }

            int initialAmount = 1_000;
            int accepted = source.fill(new FluidStack(Fluids.WATER, initialAmount), FluidAction.EXECUTE);
            if (accepted != initialAmount) {
                helper.fail("Source tank accepted " + accepted + " mB instead of " + initialAmount + " mB");
                return;
            }

            int transferPerTick = PipeApi.getFluidTransferInfo(BCTransportPipes.woodFluid).transferPerTick;
            FluidStack simulatedDrain = source.drain(transferPerTick, FluidAction.SIMULATE);
            int sourceAfterSimulation = source.getFluidInTank(0).getAmount();
            if (simulatedDrain.getAmount() != transferPerTick
                || sourceAfterSimulation != initialAmount
                || sourceTank.tank.getFluidAmount() != initialAmount) {
                helper.fail(
                    "Simulating a " + transferPerTick + " mB extraction returned " + simulatedDrain.getAmount()
                        + " mB and left handler/local amounts at " + sourceAfterSimulation + "/"
                        + sourceTank.tank.getFluidAmount() + " mB"
                );
                return;
            }
            long offeredPower = transferPerTick * BCTransportConfig.mjPerMillibucket;
            long unusedPower = behaviour.receivePower(offeredPower, FluidAction.EXECUTE);
            int sourceAfterExtraction = source.getFluidInTank(0).getAmount();
            int extracted = initialAmount - sourceAfterExtraction;
            if (extracted != transferPerTick || unusedPower != 0) {
                helper.fail(
                    "Powered wooden fluid pipe extracted " + extracted + " mB with "
                        + unusedPower + " microjoules left; expected " + transferPerTick + " mB and no remainder"
                );
                return;
            }

            helper.succeedWhen(() -> {
                FluidStack delivered = destination.getFluidInTank(0);
                if (delivered.getFluid() != Fluids.WATER || delivered.getAmount() != extracted) {
                    helper.fail(
                        "Destination tank contains " + delivered.getAmount()
                            + " mB instead of the extracted " + extracted + " mB of water"
                    );
                }
                if (source.getFluidInTank(0).getAmount() != sourceAfterExtraction) {
                    helper.fail("Source tank changed after the one powered extraction");
                }
            });
        });
    }
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 240)
    public static void creativeEnginePowersTesterThroughWoodAndStonePipes(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos woodenPipePos = enginePos.east();
        BlockPos stonePipePos = woodenPipePos.east();
        BlockPos consumerPos = stonePipePos.east();

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(woodenPipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(stonePipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.CREATIVE)
        );
        helper.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(1, () -> {
            installPipe(helper, woodenPipePos, new ItemStack(BCTransportItems.PIPE_POWER_WOOD.get()), "wooden MJ");
            installPipe(helper, stonePipePos, new ItemStack(BCTransportItems.PIPE_POWER_STONE.get()), "stone MJ");
        });

        helper.runAfterDelay(5, () -> {
            TilePipeHolder woodenPipe = requirePipeHolder(helper, woodenPipePos, "wooden MJ");
            TilePipeHolder stonePipe = requirePipeHolder(helper, stonePipePos, "stone MJ");
            if (woodenPipe == null || stonePipe == null) {
                return;
            }
            if (!(woodenPipe.getPipe().getFlow() instanceof PipeFlowPower)
                || !(stonePipe.getPipe().getFlow() instanceof PipeFlowPower)) {
                helper.fail("MJ test network did not install real wood and stone power-pipe flows");
                return;
            }
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineCreative engine)) {
                helper.fail("Creative engine block did not create TileEngineCreative");
                return;
            }

            engine.currentOutputIndex = TileEngineCreative.outputs.length - 1;
            engine.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            engine.rotateIfInvalid();
            if (!engine.isRedstonePowered) {
                helper.fail("Creative engine did not observe its adjacent redstone block");
                return;
            }
            if (engine.getCurrentFacing() != Direction.EAST) {
                helper.fail("Creative engine faced " + engine.getCurrentFacing() + " instead of the wooden power pipe");
            }
        });

        helper.runAfterDelay(12, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("MJ power-consumer tester block entity disappeared");
                return;
            }
            if (consumer.getTotalReceived() <= 0 || consumer.getLastReceived() <= 0) {
                helper.fail("Creative MJ engine has not delivered power through the wooden and stone pipe network");
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 280)
    public static void redstoneEnginePowersTesterThroughWoodAndStonePipes(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos woodenPipePos = enginePos.east();
        BlockPos stonePipePos = woodenPipePos.east();
        BlockPos consumerPos = stonePipePos.east();

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(woodenPipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(stonePipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.WOOD)
        );
        helper.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(1, () -> {
            installPipe(helper, woodenPipePos, new ItemStack(BCTransportItems.PIPE_POWER_WOOD.get()), "wooden MJ");
            installPipe(helper, stonePipePos, new ItemStack(BCTransportItems.PIPE_POWER_STONE.get()), "stone MJ");
        });

        helper.runAfterDelay(5, () -> {
            TilePipeHolder woodenPipe = requirePipeHolder(helper, woodenPipePos, "wooden MJ");
            TilePipeHolder stonePipe = requirePipeHolder(helper, stonePipePos, "stone MJ");
            if (woodenPipe == null || stonePipe == null) {
                return;
            }
            if (!(woodenPipe.getPipe().getFlow() instanceof PipeFlowPower)
                || !(stonePipe.getPipe().getFlow() instanceof PipeFlowPower)) {
                helper.fail("Redstone MJ test network did not install real wood and stone power-pipe flows");
                return;
            }
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineRedstone_BC8 engine)) {
                helper.fail("Redstone engine block did not create TileEngineRedstone_BC8");
                return;
            }

            engine.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            engine.rotateIfInvalid();
            if (!engine.isRedstonePowered) {
                helper.fail("Redstone engine did not observe its adjacent redstone block");
                return;
            }
            if (engine.getCurrentFacing() != Direction.EAST) {
                helper.fail("Redstone engine faced " + engine.getCurrentFacing() + " instead of the wooden power pipe");
            }
        });

        helper.runAfterDelay(12, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("MJ power-consumer tester block entity disappeared");
                return;
            }
            if (consumer.getTotalReceived() <= 0 || consumer.getLastReceived() <= 0) {
                helper.fail("Redstone MJ engine has not delivered power through the wooden and stone pipe network");
            }
        }));
    }

    /**
     * Regression coverage for the released 8.0.0 combustion-engine failure mode: with redstone, fuel and coolant
     * must both be consumed, the GUI-facing output fields must report MJ, the buffer must remain capped, and the
     * engine must actually deliver MJ through real power pipes.
     */
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 240)
    public static void combustionEngineConsumesResourcesReportsOutputAndDeliversBoundedMj(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos woodenPipePos = enginePos.east();
        BlockPos stonePipePos = woodenPipePos.east();
        BlockPos consumerPos = stonePipePos.east();
        final int initialFuel = 1_000;
        final int initialCoolant = 1_000;

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(woodenPipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(stonePipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.IRON)
        );
        helper.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(1, () -> {
            installPipe(helper, woodenPipePos, new ItemStack(BCTransportItems.PIPE_POWER_WOOD.get()), "wooden MJ");
            installPipe(helper, stonePipePos, new ItemStack(BCTransportItems.PIPE_POWER_STONE.get()), "stone MJ");
        });

        helper.runAfterDelay(5, () -> {
            TilePipeHolder woodenPipe = requirePipeHolder(helper, woodenPipePos, "wooden MJ");
            TilePipeHolder stonePipe = requirePipeHolder(helper, stonePipePos, "stone MJ");
            if (woodenPipe == null || stonePipe == null) {
                return;
            }
            if (!(woodenPipe.getPipe().getFlow() instanceof PipeFlowPower)
                || !(stonePipe.getPipe().getFlow() instanceof PipeFlowPower)) {
                helper.fail("Combustion-engine test did not install real wood and stone MJ-pipe flows");
                return;
            }
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineIron_BC8 engine)) {
                helper.fail("Combustion engine block did not create TileEngineIron_BC8");
                return;
            }

            engine.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            engine.rotateIfInvalid();
            if (!engine.isRedstonePowered) {
                helper.fail("Combustion engine did not observe its adjacent redstone block");
                return;
            }
            if (engine.getCurrentFacing() != Direction.EAST) {
                helper.fail("Combustion engine faced " + engine.getCurrentFacing() + " instead of the wooden power pipe");
                return;
            }
            if (BCEnergyFluids.fuelGaseous[0] == null) {
                helper.fail("Registered BuildCraft combustion fuel was unavailable during the GameTest");
                return;
            }

            int acceptedFuel = engine.tankFuel.fill(
                new FluidStack(BCEnergyFluids.fuelGaseous[0], initialFuel), FluidAction.EXECUTE
            );
            int acceptedCoolant = engine.tankCoolant.fill(
                new FluidStack(Fluids.WATER, initialCoolant), FluidAction.EXECUTE
            );
            if (acceptedFuel != initialFuel || acceptedCoolant != initialCoolant) {
                helper.fail(
                    "Combustion engine accepted fuel/coolant amounts " + acceptedFuel + "/" + acceptedCoolant
                        + " instead of " + initialFuel + "/" + initialCoolant
                );
                return;
            }

            long capacity = engine.getMaxPower();
            engine.addPower(capacity * 2);
            if (engine.getEnergyStored() != capacity) {
                helper.fail(
                    "Combustion engine stored " + engine.getEnergyStored()
                        + " microjoules after an over-capacity offer; expected its " + capacity + " cap"
                );
                return;
            }
            while (engine.getEnergyStored() > 0) {
                long extracted = engine.extractPower(0, capacity, true);
                if (extracted <= 0) {
                    helper.fail("Combustion engine failed to clear its bounded setup buffer");
                    return;
                }
            }

            // Released combustion engines consume coolant only above the 100 C ideal temperature.
            // Stored MJ does not heat an engine, so establish that real precondition through the
            // compatibility-sensitive persisted heat field.
            CompoundTag warmedState = engine.saveWithoutMetadata();
            warmedState.putDouble("heat", 110.0);
            engine.load(warmedState);
        });

        helper.runAfterDelay(50, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineIron_BC8 engine)) {
                helper.fail("Combustion engine block entity disappeared before validation");
                return;
            }
            if (!(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("MJ power-consumer tester disappeared before combustion-engine delivery");
                return;
            }
            if (!engine.isRedstonePowered) {
                helper.fail("Combustion engine lost redstone power while its redstone block remained adjacent");
                return;
            }
            if (engine.tankFuel.getFluidAmount() >= initialFuel) {
                helper.fail("Combustion engine did not consume any fuel while redstone-powered");
                return;
            }
            if (engine.tankCoolant.getFluidAmount() >= initialCoolant) {
                helper.fail("Combustion engine did not consume coolant after producing heat");
                return;
            }
            if (engine.getCurrentOutput() <= 0 || engine.currentOutput <= 0) {
                helper.fail(
                    "Combustion engine reported output " + engine.getCurrentOutput()
                        + "/" + engine.currentOutput + " instead of a positive MJ value"
                );
                return;
            }
            if (engine.getEnergyStored() < 0 || engine.getEnergyStored() > engine.getMaxPower()) {
                helper.fail(
                    "Combustion engine stored " + engine.getEnergyStored()
                        + " microjoules outside its [0," + engine.getMaxPower() + "] bound"
                );
                return;
            }
            if (consumer.getTotalReceived() <= 0 || consumer.getLastReceived() <= 0) {
                helper.fail("Combustion engine has not delivered MJ through the wooden and stone pipe network");
            }
        }));
    }

    /** Covers both the Stirling engine's solid-fuel loop and the 8.0 engine-chaining path. */
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 360)
    public static void chainedStirlingEnginesConsumeFuelAndDeliverMj(GameTestHelper helper) {
        BlockPos rearEnginePos = new BlockPos(1, 1, 1);
        BlockPos frontEnginePos = rearEnginePos.east();
        BlockPos woodenPipePos = frontEnginePos.east();
        BlockPos stonePipePos = woodenPipePos.east();
        BlockPos consumerPos = stonePipePos.east();

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(woodenPipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(stonePipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(
            rearEnginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.STONE)
        );
        helper.setBlock(
            frontEnginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.STONE)
        );
        helper.setBlock(rearEnginePos.north(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(frontEnginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(1, () -> {
            installPipe(helper, woodenPipePos, new ItemStack(BCTransportItems.PIPE_POWER_WOOD.get()), "wooden MJ");
            installPipe(helper, stonePipePos, new ItemStack(BCTransportItems.PIPE_POWER_STONE.get()), "stone MJ");
        });

        helper.runAfterDelay(5, () -> {
            TilePipeHolder woodenPipe = requirePipeHolder(helper, woodenPipePos, "wooden MJ");
            TilePipeHolder stonePipe = requirePipeHolder(helper, stonePipePos, "stone MJ");
            if (woodenPipe == null || stonePipe == null) {
                return;
            }
            if (!(woodenPipe.getPipe().getFlow() instanceof PipeFlowPower)
                || !(stonePipe.getPipe().getFlow() instanceof PipeFlowPower)) {
                helper.fail("Stirling-chain test did not install real wood and stone MJ-pipe flows");
                return;
            }
            if (!(helper.getBlockEntity(rearEnginePos) instanceof TileEngineStone_BC8 rear)
                || !(helper.getBlockEntity(frontEnginePos) instanceof TileEngineStone_BC8 front)) {
                helper.fail("Stirling engine blocks did not create TileEngineStone_BC8 instances");
                return;
            }

            rear.invFuel.setStackInSlot(0, new ItemStack(Items.COAL));
            front.invFuel.setStackInSlot(0, new ItemStack(Items.COAL));
            front.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            front.rotateIfInvalid();
            rear.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            rear.rotateIfInvalid();

            if (rear.getCurrentFacing() != Direction.EAST || front.getCurrentFacing() != Direction.EAST) {
                helper.fail(
                    "Stirling chain faced " + rear.getCurrentFacing() + "/" + front.getCurrentFacing()
                        + " instead of east toward the MJ network"
                );
                return;
            }
            if (rear.getReceiverToPower(Direction.EAST) == null) {
                helper.fail("Rear Stirling engine could not resolve a receiver through the chained front engine");
            }
        });

        helper.runAfterDelay(20, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(rearEnginePos) instanceof TileEngineStone_BC8 rear)
                || !(helper.getBlockEntity(frontEnginePos) instanceof TileEngineStone_BC8 front)
                || !(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("Stirling chain lost an engine or its MJ consumer while running");
                return;
            }
            if (!rear.invFuel.getStackInSlot(0).isEmpty() || !front.invFuel.getStackInSlot(0).isEmpty()) {
                helper.fail("One or both powered Stirling engines did not consume their coal fuel");
                return;
            }
            if (!rear.isBurning() || !front.isBurning()) {
                helper.fail("One or both fueled Stirling engines stopped before their coal burn completed");
                return;
            }
            if (rear.getCurrentOutput() <= 0 || front.getCurrentOutput() <= 0) {
                helper.fail("Stirling engine GUI-facing output remained zero while both engines were burning");
                return;
            }
            if (consumer.getTotalReceived() <= 0 || consumer.getLastReceived() <= 0) {
                helper.fail("Chained Stirling engines have not delivered MJ through the real pipe network");
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 300)
    public static void dynamoFeedsRfEngineThroughWoodAndStoneRfPipes(GameTestHelper helper) {
        BlockPos dynamoPos = new BlockPos(1, 1, 1);
        BlockPos woodenPipePos = dynamoPos.east();
        BlockPos stonePipePos = woodenPipePos.east();
        BlockPos enginePos = stonePipePos.east();

        helper.setBlock(woodenPipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(stonePipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(dynamoPos, BCEnergyBlocks.MJ_DYNAMO.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.RF)
        );
        helper.setBlock(dynamoPos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(1, () -> {
            installPipe(helper, woodenPipePos, new ItemStack(BCTransportItems.PIPE_RF_WOOD.get()), "wooden RF");
            installPipe(helper, stonePipePos, new ItemStack(BCTransportItems.PIPE_RF_STONE.get()), "stone RF");
        });

        helper.runAfterDelay(5, () -> {
            TilePipeHolder woodenPipe = requirePipeHolder(helper, woodenPipePos, "wooden RF");
            TilePipeHolder stonePipe = requirePipeHolder(helper, stonePipePos, "stone RF");
            if (woodenPipe == null || stonePipe == null) {
                return;
            }
            if (!(woodenPipe.getPipe().getFlow() instanceof PipeFlowRedstoneFlux)
                || !(stonePipe.getPipe().getFlow() instanceof PipeFlowRedstoneFlux)) {
                helper.fail("RF test network did not install real wood and stone RF-pipe flows");
                return;
            }
            if (!(helper.getBlockEntity(dynamoPos) instanceof TileDynamoMJ dynamo)) {
                helper.fail("MJ dynamo block did not create TileDynamoMJ");
                return;
            }
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineRF)) {
                helper.fail("RF engine block did not create TileEngineRF");
                return;
            }

            dynamo.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            dynamo.rotateIfInvalid();
            if (!dynamo.isRedstonePowered) {
                helper.fail("MJ dynamo did not observe its adjacent redstone block");
                return;
            }
            if (dynamo.getCurrentDirection() != Direction.EAST) {
                helper.fail("MJ dynamo faced " + dynamo.getCurrentDirection() + " instead of the wooden RF pipe");
                return;
            }

            IMjReceiver input = dynamo.getCapability(MjAPI.CAP_RECEIVER, Direction.WEST).orElse(null);
            if (input == null || !input.canReceive()) {
                helper.fail("MJ dynamo did not expose an MJ input on its non-output face");
                return;
            }
            long offeredMj = 100L * MjAPI.MJ;
            long unusedMj = input.receivePower(offeredMj, FluidAction.EXECUTE);
            if (unusedMj != 0) {
                helper.fail("MJ dynamo left " + unusedMj + " microjoules unaccepted during test setup");
            }
        });

        helper.runAfterDelay(15, () -> helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineRF receiver)) {
                helper.fail("RF engine block entity disappeared before FE delivery");
                return;
            }
            if (receiver.getCurrentForgeEnergy() <= 0) {

                helper.fail("MJ dynamo has not delivered FE through the wooden and stone RF-pipe network");
            }
        }));
    }

    /** Regular MJ machines receive continuous output; only redstone receivers wait for a piston pulse. */
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void stirlingEngineFeedsRegularMjReceiverBeforeFirstPistonPulse(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos consumerPos = enginePos.east();

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.STONE)
        );
        helper.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineStone_BC8 engine)) {
                helper.fail("Stirling engine block did not create TileEngineStone_BC8");
                return;
            }
            engine.invFuel.setStackInSlot(0, new ItemStack(Items.COAL));
            engine.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            engine.rotateIfInvalid();
            if (engine.getCurrentFacing() != Direction.EAST) {
                helper.fail("Stirling engine did not face the adjacent regular MJ receiver");
            }
        });

        helper.runAfterDelay(15, () -> {
            if (!(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("Regular MJ receiver disappeared");
                return;
            }
            if (consumer.getTotalReceived() <= 0) {
                helper.fail("Regular MJ receiver waited for a piston pulse instead of receiving continuous power");
                return;
            }
            helper.succeed();
        });
    }

    /** Wooden engines deliberately power redstone receivers such as wooden pipes, not arbitrary MJ machines. */
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 60)
    public static void redstoneEngineRejectsRegularMjReceiver(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos consumerPos = enginePos.east();

        helper.setBlock(consumerPos, BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(
            enginePos,
            BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
                .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.WOOD)
        );
        helper.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(enginePos) instanceof TileEngineRedstone_BC8 engine)) {
                helper.fail("Redstone engine block did not create TileEngineRedstone_BC8");
                return;
            }
            engine.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);
            if (engine.getReceiverToPower(Direction.EAST) != null) {
                helper.fail("Redstone engine connected directly to a non-redstone MJ receiver");
            }
        });

        helper.runAfterDelay(30, () -> {
            if (!(helper.getBlockEntity(consumerPos) instanceof TilePowerConsumerTester consumer)) {
                helper.fail("Regular MJ receiver disappeared");
                return;
            }
            if (consumer.getTotalReceived() != 0) {
                helper.fail("Redstone engine bypassed its pulse-only receiver boundary");
                return;
            }
            helper.succeed();
        });
    }

    private static void installPipe(GameTestHelper helper, BlockPos position, ItemStack pipeStack, String pipeName) {
        TilePipeHolder pipeHolder = requirePipeHolder(helper, position, pipeName);
        if (pipeHolder != null) {
            pipeHolder.onPlacedBy(helper.makeMockPlayer(), pipeStack);
        }
    }

    private static TilePipeHolder requirePipeHolder(GameTestHelper helper, BlockPos position, String pipeName) {
        if (helper.getBlockEntity(position) instanceof TilePipeHolder pipeHolder) {
            return pipeHolder;
        }
        helper.fail("Expected a " + pipeName + " pipe holder at " + position);
        return null;
    }
}
