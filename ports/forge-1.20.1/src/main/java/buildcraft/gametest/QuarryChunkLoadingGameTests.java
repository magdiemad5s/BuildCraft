/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.tile.TileQuarry;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime coverage for the quarry's Forge ticket acquire/release lifecycle.
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class QuarryChunkLoadingGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private QuarryChunkLoadingGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void workingQuarryForcesAndRemovalReleasesChunks(GameTestHelper helper) {
        BlockPos quarryPos = new BlockPos(2, 1, 2);
        helper.setBlock(quarryPos, Blocks.AIR);
        BlockPos absoluteQuarryPos = helper.absolutePos(quarryPos);
        long ownerChunk = new ChunkPos(absoluteQuarryPos).toLong();
        BlockTicketSnapshot baseline = captureBlockTickets(helper, ownerChunk);
        helper.setBlock(quarryPos, BCBuildersBlocks.QUARRY.get());

        helper.runAfterDelay(1, () -> {
            if (!(helper.getBlockEntity(quarryPos) instanceof TileQuarry quarry)) {
                helper.fail("Quarry block entity was not created");
                return;
            }
            quarry.onPlacedBy(helper.makeMockPlayer(), ItemStack.EMPTY);

            helper.runAfterDelay(2, () -> {
                BlockTicketSnapshot acquired = captureBlockTickets(helper, ownerChunk);
                if (!acquired.hasAdditionalTicketsComparedWith(baseline)) {
                    helper.fail("Working quarry did not create Forge block-owned chunk tickets");
                    return;
                }

                // Replace the live block entity through its real NBT format. Chunk-unload callbacks must preserve
                // persistent Forge tickets, and the recreated quarry must claim the exact same owner/area.
                BlockState quarryState = helper.getLevel().getBlockState(absoluteQuarryPos);
                CompoundTag savedQuarry = quarry.saveWithFullMetadata();
                quarry.onChunkUnloaded();
                helper.getLevel().removeBlockEntity(absoluteQuarryPos);
                BlockTicketSnapshot afterUnloadCallbacks = captureBlockTickets(helper, ownerChunk);
                if (!afterUnloadCallbacks.equals(acquired)) {
                    helper.setBlock(quarryPos, Blocks.AIR);
                    helper.fail("Ordinary quarry unload callbacks incorrectly released persistent Forge tickets");
                    return;
                }

                BlockEntity loaded = BlockEntity.loadStatic(absoluteQuarryPos, quarryState, savedQuarry);
                if (!(loaded instanceof TileQuarry restoredQuarry)) {
                    helper.setBlock(quarryPos, Blocks.AIR);
                    helper.fail("Quarry NBT did not recreate a TileQuarry during the reload fixture");
                    return;
                }
                restoredQuarry.setLevel(helper.getLevel());
                restoredQuarry.clearRemoved();
                helper.getLevel().setBlockEntity(restoredQuarry);
                restoredQuarry.onLoad();

                helper.runAfterDelay(2, () -> {
                    if (helper.getLevel().getBlockEntity(absoluteQuarryPos) != restoredQuarry) {
                        helper.setBlock(quarryPos, Blocks.AIR);
                        helper.fail("Reloaded quarry was not installed as the live block entity");
                        return;
                    }
                    BlockTicketSnapshot afterReload = captureBlockTickets(helper, ownerChunk);
                    if (!afterReload.equals(acquired)) {
                        helper.setBlock(quarryPos, Blocks.AIR);
                        helper.fail(
                            "Reloaded quarry changed its persistent Forge ticket set"
                                + " (before reload=" + acquired.ticketCount()
                                + ", after reload=" + afterReload.ticketCount() + ")"
                        );
                        return;
                    }

                    helper.setBlock(quarryPos, Blocks.AIR);
                    helper.runAfterDelay(2, () -> {
                        BlockTicketSnapshot released = captureBlockTickets(helper, ownerChunk);
                        if (!released.equals(baseline)) {
                            helper.fail(
                                "Quarry did not restore the Forge block-ticket tracker to its pre-test state"
                                    + " (before=" + baseline.ticketCount()
                                    + ", acquired=" + acquired.ticketCount()
                                    + ", after=" + released.ticketCount() + ")"
                            );
                            return;
                        }
                        helper.succeed();
                    });
                });
            });
        });
    }

    private static BlockTicketSnapshot captureBlockTickets(GameTestHelper helper, long ownerChunk) {
        ForcedChunksSavedData data = helper.getLevel()
            .getDataStorage()
            .get(ForcedChunksSavedData::load, "chunks");
        if (data == null) {
            return BlockTicketSnapshot.EMPTY;
        }
        ForgeChunkManager.TicketTracker<BlockPos> tracker = data.getBlockForcedChunks();
        return new BlockTicketSnapshot(
            copyTicketMap(tracker.getChunks(), ownerChunk),
            copyTicketMap(tracker.getTickingChunks(), ownerChunk)
        );
    }

    private static Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> copyTicketMap(
        Map<ForgeChunkManager.TicketOwner<BlockPos>, LongSet> source,
        long ownerChunk
    ) {
        Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> copy = new HashMap<>();
        source.forEach((owner, chunks) -> {
            if (!chunks.contains(ownerChunk)) {
                return;
            }
            Set<Long> chunkCopy = new HashSet<>();
            for (long chunk : chunks) {
                chunkCopy.add(chunk);
            }
            copy.put(owner, Collections.unmodifiableSet(chunkCopy));
        });
        return Collections.unmodifiableMap(copy);
    }

    private record BlockTicketSnapshot(
        Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> regular,
        Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> ticking
    ) {
        private static final BlockTicketSnapshot EMPTY =
            new BlockTicketSnapshot(Collections.emptyMap(), Collections.emptyMap());

        private boolean hasAdditionalTicketsComparedWith(BlockTicketSnapshot baseline) {
            return containsAdditionalTickets(regular, baseline.regular)
                || containsAdditionalTickets(ticking, baseline.ticking);
        }

        private int ticketCount() {
            return ticketCount(regular) + ticketCount(ticking);
        }

        private static boolean containsAdditionalTickets(
            Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> current,
            Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> baseline
        ) {
            for (Map.Entry<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> entry : current.entrySet()) {
                Set<Long> baselineChunks = baseline.getOrDefault(entry.getKey(), Collections.emptySet());
                if (!baselineChunks.containsAll(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        private static int ticketCount(Map<ForgeChunkManager.TicketOwner<BlockPos>, Set<Long>> tickets) {
            return tickets.values().stream().mapToInt(Set::size).sum();
        }
    }
}
