/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import buildcraft.api.enums.EnumSnapshotType;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.GlobalSavedDataSnapshots;
import buildcraft.builders.snapshot.SchematicBlockManager;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.blockEntity.TileMarkerVolume;
import buildcraft.core.marker.VolumeSubCache;
import buildcraft.lib.block.BlockBCBase_Neptune;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tick-driven runtime coverage for the three core Builders machines.
 *
 * <p>These tests deliberately operate through the registered blocks, block-entity tickers, MJ capabilities,
 * inventories, snapshot store, and Forge event bus. They are not source-contract or direct helper-method tests.</p>
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class BuildersMachineGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private BuildersMachineGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, batch = "buildcraft_builders_quarry", timeoutTicks = 400)
    public static void quarryBuildsFrameConsumesPowerMinesAndRetainsDrops(GameTestHelper helper) {
        // Keep the complete fixture inside the 5x5x5 empty template. Coordinates far outside a GameTest template can
        // overlap another parallel test or retain stale state between runs; either makes the power assertion invalid.
        BlockPos outputPos = new BlockPos(0, 0, 0);
        BlockPos quarryPos = new BlockPos(1, 0, 0);
        BlockPos markerMin = new BlockPos(1, 0, 1);
        BlockPos markerX = new BlockPos(4, 0, 1);
        BlockPos markerZ = new BlockPos(1, 0, 4);
        BlockState markerState = BCCoreBlocks.MARKER_VOLUME.get().defaultBlockState()
            .setValue(BlockBCBase_Neptune.BLOCK_FACING_6, Direction.DOWN);
        helper.setBlock(markerMin.above(), Blocks.STONE);
        helper.setBlock(markerX.above(), Blocks.STONE);
        helper.setBlock(markerZ.above(), Blocks.STONE);
        helper.setBlock(markerMin, markerState);
        helper.setBlock(markerX, markerState);
        helper.setBlock(markerZ, markerState);

        helper.startSequence().thenIdle(2).thenExecute(() -> {
            TileMarkerVolume marker =
                blockEntity(helper, markerMin, TileMarkerVolume.class, "quarry volume marker");
            if (marker == null) {
                return;
            }
            VolumeSubCache markerCache = (VolumeSubCache) marker.getLocalCache();
            BlockPos absoluteMarkerMin = helper.absolutePos(markerMin);
            if (!markerCache.tryConnect(absoluteMarkerMin, helper.absolutePos(markerX))
                || !markerCache.tryConnect(absoluteMarkerMin, helper.absolutePos(markerZ))) {
                helper.fail("Could not establish the Quarry GameTest's bounded marker area");
                return;
            }

            helper.setBlock(outputPos, Blocks.CHEST);
            helper.setBlock(quarryPos, BCBuildersBlocks.QUARRY.get());
            TileQuarry quarry = blockEntity(helper, quarryPos, TileQuarry.class, "quarry");
            ChestBlockEntity output = blockEntity(helper, outputPos, ChestBlockEntity.class, "quarry output chest");
            if (quarry == null || output == null) {
                return;
            }

            Player owner = helper.makeMockPlayer();
            quarry.onPlacedBy(owner, ItemStack.EMPTY);
            if (!quarry.frameBox.isInitialized() || quarry.framePoses.isEmpty()) {
                helper.fail("Quarry did not claim its bounded marker area and establish frame positions");
                return;
            }

            IMjReceiver receiver = quarry.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            IMjReadable readable = quarry.getCapability(MjAPI.CAP_READABLE).orElse(null);
            if (receiver == null || readable == null) {
                helper.fail("Quarry did not expose its legacy MJ receiver/readable capabilities");
                return;
            }
            long offered = receiver.getPowerRequested();
            long excess = receiver.receivePower(offered, FluidAction.EXECUTE);
            long startingPower = readable.getStored();
            if (offered <= 0 || excess != 0 || startingPower <= 0 || startingPower > readable.getCapacity()) {
                helper.fail(
                    "Quarry rejected or unsafely stored its requested MJ"
                        + " (offered=" + offered + ", excess=" + excess
                        + ", stored=" + startingPower + ", capacity=" + readable.getCapacity() + ")"
                );
                return;
            }

            ServerLevel level = helper.getLevel();
            int targetY = quarry.frameBox.min().getY() - 1;
            List<BlockPos> orePositions = new ArrayList<>();
            for (int x = quarry.frameBox.min().getX() + 1; x < quarry.frameBox.max().getX(); x++) {
                for (int z = quarry.frameBox.min().getZ() + 1; z < quarry.frameBox.max().getZ(); z++) {
                    BlockPos orePos = new BlockPos(x, targetY, z);
                    level.setBlockAndUpdate(orePos, Blocks.DIAMOND_ORE.defaultBlockState());
                    orePositions.add(orePos);
                }
            }

            QuarryProgress progress = new QuarryProgress(startingPower, orePositions);
            helper.onEachTick(() -> {
                if (!progress.sawFrame) {
                    progress.sawFrame = quarry.framePoses.stream()
                        .anyMatch(pos -> level.getBlockState(pos).is(BCBuildersBlocks.FRAME.get()));
                }

                int retainedDiamonds = countItem(output, Items.DIAMOND);
                if (retainedDiamonds <= 0) {
                    return;
                }

                long remainingPower = readable.getStored();
                if (!progress.sawFrame) {
                    cleanupQuarryFixture(helper, quarryPos, outputPos, orePositions);
                    helper.fail("Quarry mined before ever constructing a visible frame");
                    return;
                }
                if (remainingPower < 0 || remainingPower >= progress.startingPower) {
                    cleanupQuarryFixture(helper, quarryPos, outputPos, orePositions);
                    helper.fail(
                        "Quarry did not consume MJ safely while working"
                            + " (before=" + progress.startingPower + ", after=" + remainingPower + ")"
                    );
                    return;
                }
                if (orePositions.stream().noneMatch(level::isEmptyBlock)) {
                    cleanupQuarryFixture(helper, quarryPos, outputPos, orePositions);
                    helper.fail("Quarry output a diamond without removing any ore from its mining area");
                    return;
                }

                cleanupQuarryFixture(helper, quarryPos, outputPos, orePositions);
                helper.runAfterDelay(2, helper::succeed);
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, batch = "buildcraft_builders_filler", timeoutTicks = 200)
    public static void fillerFillPatternRefundsCancelledPlacementThenCompletes(GameTestHelper helper) {
        BlockPos fillerPos = new BlockPos(1, 1, 1);
        BlockPos firstTarget = new BlockPos(2, 1, 1);
        BlockPos secondTarget = new BlockPos(3, 1, 1);
        helper.setBlock(firstTarget, Blocks.AIR);
        helper.setBlock(secondTarget, Blocks.AIR);
        helper.setBlock(fillerPos, Blocks.AIR);
        helper.setBlock(fillerPos, BCBuildersBlocks.FILLER.get());

        helper.startSequence().thenIdle(2).thenExecute(() -> {
            TileFiller filler = blockEntity(helper, fillerPos, TileFiller.class, "filler");
            if (filler == null) {
                return;
            }
            filler.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);

            BlockPos absoluteFirst = helper.absolutePos(firstTarget);
            BlockPos absoluteSecond = helper.absolutePos(secondTarget);
            filler.box.reset();
            filler.box.setMin(absoluteFirst);
            filler.box.setMax(absoluteSecond);
            filler.setPattern(BCBuildersStatements.PATTERN_FILL, new IStatementParameter[0]);
            filler.onStatementChange();

            ItemStack remainder = filler.invResources.insertItem(0, new ItemStack(Items.STONE, 2), false);
            int insertedStone = countItem(filler.invResources, Items.STONE);
            if (!remainder.isEmpty() || insertedStone != 2) {
                helper.fail(
                    "Filler resource inventory rejected or duplicated its two stone input blocks"
                        + " (remainder=" + remainder + ", stored=" + insertedStone + ")"
                );
                return;
            }
            filler.getBattery().addPower(1_000L * MjAPI.MJ, FluidAction.EXECUTE);
            long startingPower = filler.getBattery().getStored();

            CancelFirstPlacement listener = new CancelFirstPlacement(absoluteFirst);
            MinecraftForge.EVENT_BUS.register(listener);
            TransactionProgress progress = new TransactionProgress();
            helper.onEachTick(() -> {
                if (listener.attempts == 1 && !progress.refundObserved) {
                    int placed = (helper.getLevel().getBlockState(absoluteFirst).is(Blocks.STONE) ? 1 : 0)
                        + (helper.getLevel().getBlockState(absoluteSecond).is(Blocks.STONE) ? 1 : 0);
                    int stored = countItem(filler.invResources, Items.STONE);
                    // The other stone may already be reserved by the second queued PlaceTask. Only the resource for
                    // the Forge-cancelled first task must be back in the inventory at this point.
                    if (!helper.getLevel().isEmptyBlock(absoluteFirst) || stored != 1) {
                        MinecraftForge.EVENT_BUS.unregister(listener);
                        helper.fail(
                            "Cancelled Filler placement did not refund its reserved stone"
                                + " (placed=" + placed + ", refunded=" + stored + ")"
                        );
                        return;
                    }
                    progress.refundObserved = true;
                }

                if (!helper.getLevel().getBlockState(absoluteFirst).is(Blocks.STONE)
                    || !helper.getLevel().getBlockState(absoluteSecond).is(Blocks.STONE)) {
                    return;
                }

                MinecraftForge.EVENT_BUS.unregister(listener);
                if (!progress.refundObserved || listener.attempts < 2) {
                    helper.fail("Filler did not retry a Forge-cancelled placement");
                    return;
                }
                if (countItem(filler.invResources, Items.STONE) != 0) {
                    helper.fail("Filler completed the two-block fill without consuming exactly two stone");
                    return;
                }
                if (filler.getBattery().getStored() < 0 || filler.getBattery().getStored() >= startingPower) {
                    helper.fail("Filler did not consume MJ safely while filling");
                    return;
                }

                helper.setBlock(firstTarget, Blocks.AIR);
                helper.setBlock(secondTarget, Blocks.AIR);
                helper.setBlock(fillerPos, Blocks.AIR);
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, batch = "buildcraft_builders_builder", timeoutTicks = 240)
    public static void builderConsumesBlueprintAndHonoursPlacementCancellation(GameTestHelper helper) {
        BlockPos builderPos = new BlockPos(2, 1, 2);
        BlockPos sourcePos = new BlockPos(4, 1, 1);
        helper.setBlock(builderPos.south(), Blocks.AIR);
        helper.setBlock(builderPos, Blocks.AIR);
        helper.setBlock(sourcePos, Blocks.AIR);
        helper.setBlock(builderPos, BCBuildersBlocks.BUILDER.get());
        helper.setBlock(sourcePos, Blocks.GOLD_BLOCK);

        helper.startSequence().thenIdle(2).thenExecute(() -> {
            TileBuilder builder = blockEntity(helper, builderPos, TileBuilder.class, "builder");
            if (builder == null) {
                return;
            }
            Player owner = helper.makeMockPlayer();
            builder.onPlacedBy(owner, ItemStack.EMPTY);

            BlockPos absoluteSource = helper.absolutePos(sourcePos);
            BlockState sourceState = helper.getLevel().getBlockState(absoluteSource);
            ISchematicBlock schematic = SchematicBlockManager.getSchematicBlock(new SchematicBlockContext(
                helper.getLevel(),
                absoluteSource,
                absoluteSource,
                sourceState,
                sourceState.getBlock()
            ));
            if (schematic == null) {
                helper.fail("Could not capture a gold block for the minimal Builder blueprint");
                return;
            }

            Blueprint blueprint = new Blueprint();
            blueprint.size = new BlockPos(1, 1, 1);
            blueprint.facing = Direction.NORTH;
            blueprint.offset = BlockPos.ZERO;
            blueprint.palette.add(schematic);
            blueprint.data = new int[] { 0 };
            blueprint.computeKey();
            GlobalSavedDataSnapshots snapshots = GlobalSavedDataSnapshots.get(helper.getLevel());
            snapshots.addSnapshot(blueprint);

            Snapshot.Header header = new Snapshot.Header(
                blueprint.key,
                owner.getUUID(),
                owner.getGameProfile().getName(),
                new Date(0L),
                "BuildCraft Neo Builder GameTest",
                false,
                true,
                true
            );
            ItemStack blueprintStack = ItemSnapshot.getUsed(EnumSnapshotType.BLUEPRINT, header);
            ItemStack snapshotBeforeInsert = builder.invSnapshot.getStackInSlot(0).copy();
            if (!snapshotBeforeInsert.isEmpty()) {
                snapshots.removeSnapshot(blueprint.key);
                helper.fail("Fresh Builder snapshot slot was not empty before insertion: " + snapshotBeforeInsert);
                return;
            }

            ItemStack blueprintRemainder = builder.invSnapshot.insertItem(0, blueprintStack, false);
            ItemStack insertedBlueprint = builder.invSnapshot.getStackInSlot(0);
            if (!blueprintRemainder.isEmpty()
                || insertedBlueprint.isEmpty()
                || !ItemStack.isSameItemSameTags(blueprintStack, insertedBlueprint)) {
                snapshots.removeSnapshot(blueprint.key);
                helper.fail(
                    "Builder rejected its minimal blueprint"
                        + " (input=" + blueprintStack
                        + ", remainder=" + blueprintRemainder
                        + ", slot=" + insertedBlueprint + ")"
                );
                return;
            }

            ItemStack resourceRemainder =
                builder.invResources.insertItem(0, new ItemStack(Items.GOLD_BLOCK), false);
            int storedGold = countItem(builder.invResources, Items.GOLD_BLOCK);
            if (!resourceRemainder.isEmpty() || storedGold != 1) {
                snapshots.removeSnapshot(blueprint.key);
                helper.fail(
                    "Builder rejected its required gold block"
                        + " (remainder=" + resourceRemainder + ", stored=" + storedGold + ")"
                );
                return;
            }
            builder.getBattery().addPower(1_000L * MjAPI.MJ, FluidAction.EXECUTE);
            long startingPower = builder.getBattery().getStored();
            helper.setBlock(sourcePos, Blocks.AIR);

            BlockPos absoluteBuilder = helper.absolutePos(builderPos);
            BlockPos absoluteTarget = absoluteBuilder.south();
            CancelFirstPlacement listener = new CancelFirstPlacement(absoluteTarget);
            MinecraftForge.EVENT_BUS.register(listener);
            TransactionProgress progress = new TransactionProgress();
            helper.onEachTick(() -> {
                if (listener.attempts == 1 && !progress.refundObserved) {
                    int stored = countItem(builder.invResources, Items.GOLD_BLOCK);
                    if (!helper.getLevel().isEmptyBlock(absoluteTarget) || stored != 1) {
                        MinecraftForge.EVENT_BUS.unregister(listener);
                        snapshots.removeSnapshot(blueprint.key);
                        helper.fail(
                            "Cancelled Builder placement changed the world or lost its resource"
                                + " (refunded gold=" + stored + ")"
                        );
                        return;
                    }
                    progress.refundObserved = true;
                }

                if (!helper.getLevel().getBlockState(absoluteTarget).is(Blocks.GOLD_BLOCK)) {
                    return;
                }

                MinecraftForge.EVENT_BUS.unregister(listener);
                snapshots.removeSnapshot(blueprint.key);
                if (!progress.refundObserved || listener.attempts < 2) {
                    helper.fail("Builder did not retry a Forge-cancelled blueprint placement");
                    return;
                }
                if (!builder.invSnapshot.getStackInSlot(0).isEmpty()) {
                    helper.fail("Builder placed the blueprint but did not consume/move it out of the blueprint slot");
                    return;
                }
                if (countItem(builder.invResources, Items.GOLD_BLOCK) != 0) {
                    helper.fail("Builder placed the blueprint without consuming exactly one gold block");
                    return;
                }
                if (builder.getBattery().getStored() < 0 || builder.getBattery().getStored() >= startingPower) {
                    helper.fail("Builder did not consume MJ safely while placing its blueprint");
                    return;
                }

                helper.getLevel().setBlockAndUpdate(absoluteTarget, Blocks.AIR.defaultBlockState());
                helper.setBlock(builderPos, Blocks.AIR);
                helper.succeed();
            });
        });
    }

    private static void cleanupQuarryFixture(
        GameTestHelper helper,
        BlockPos quarryPos,
        BlockPos outputPos,
        List<BlockPos> orePositions
    ) {
        helper.setBlock(quarryPos, Blocks.AIR);
        helper.setBlock(outputPos, Blocks.AIR);
        for (BlockPos orePos : orePositions) {
            if (helper.getLevel().getBlockState(orePos).is(Blocks.DIAMOND_ORE)) {
                helper.getLevel().setBlockAndUpdate(orePos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static int countItem(ChestBlockEntity chest, Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countItem(net.minecraftforge.items.IItemHandler handler, Item item) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static <T extends BlockEntity> T blockEntity(
        GameTestHelper helper,
        BlockPos pos,
        Class<T> expectedType,
        String description
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (expectedType.isInstance(blockEntity)) {
            return expectedType.cast(blockEntity);
        }
        helper.fail("Expected " + description + " at " + pos + " but found " + blockEntity);
        return null;
    }

    private static final class CancelFirstPlacement {
        private final BlockPos target;
        private int attempts;

        private CancelFirstPlacement(BlockPos target) {
            this.target = target;
        }

        @SubscribeEvent
        public void onPlace(BlockEvent.EntityPlaceEvent event) {
            if (!target.equals(event.getPos())) {
                return;
            }
            attempts++;
            if (attempts == 1) {
                event.setCanceled(true);
            }
        }
    }

    private static final class QuarryProgress {
        private final long startingPower;
        private final List<BlockPos> orePositions;
        private boolean sawFrame;

        private QuarryProgress(long startingPower, List<BlockPos> orePositions) {
            this.startingPower = startingPower;
            this.orePositions = orePositions;
        }
    }

    private static final class TransactionProgress {
        private boolean refundObserved;
    }
}
