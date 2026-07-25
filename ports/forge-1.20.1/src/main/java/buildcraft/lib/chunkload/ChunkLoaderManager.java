/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.chunkload;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongSet;

import buildcraft.api.tiles.IHasWork;
import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.world.ForgeChunkManager;

/**
 * Owns BuildCraft's Forge chunk tickets.
 * <p>
 * Forge 1.20.1 persists block-owned tickets itself, so the quarry position is the stable owner identity. The local map
 * only mirrors the chunks currently owned by each tile so stale chunks can be unforced when an area changes or the
 * tile stops working.
 */
public final class ChunkLoaderManager {
    private static final boolean FULLY_TICKING = true;

    private static final Map<ServerLevel, Map<BlockPos, Set<ChunkPos>>> ACTIVE_TICKETS = new WeakHashMap<>();
    private static final Set<ServerLevel> VALIDATING_LEVELS =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private ChunkLoaderManager() {
    }

    /**
     * Synchronizes the Forge tickets for a tile with its current requested area. Calling this while loading is safe and
     * idempotent.
     */
    public static synchronized <T extends BlockEntity & IChunkLoadingTile> void loadChunksForTile(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || VALIDATING_LEVELS.contains(level)) {
            return;
        }
        if (tile.isRemoved() || !canLoadFor(tile)) {
            releaseTrackedChunksFor(level, tile.getBlockPos());
            return;
        }

        Set<ChunkPos> desired = getChunksToLoad(tile.getBlockPos(), tile.getChunksToLoad());
        updateChunksFor(level, tile.getBlockPos(), desired);
    }

    /**
     * Releases every ticket currently known for a tile. The current requested area is included as a best-effort
     * fallback so removal remains safe even if the in-memory mirror was rebuilt.
     */
    public static synchronized void releaseChunksFor(BlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || VALIDATING_LEVELS.contains(level)) {
            return;
        }

        BlockPos owner = tile.getBlockPos().immutable();
        Set<ChunkPos> chunks = removeTrackedChunks(level, owner);
        if (chunks == null) {
            chunks = new HashSet<>();
            chunks.add(new ChunkPos(owner));
            if (tile instanceof IChunkLoadingTile loadingTile) {
                Set<ChunkPos> requested = loadingTile.getChunksToLoad();
                if (requested != null) {
                    chunks.addAll(requested);
                }
            }
        }

        for (ChunkPos chunk : chunks) {
            ForgeChunkManager.forceChunk(
                level, BCLib.MODID, owner, chunk.x, chunk.z, false, FULLY_TICKING
            );
        }
    }

    /**
     * Validates persistent Forge tickets before they are reinstated. Invalid owners, completed machines, disabled
     * chunk loading, obsolete non-ticking tickets, and chunks outside the tile's current area are removed.
     */
    public static synchronized void validateTickets(
        ServerLevel level, ForgeChunkManager.TicketHelper ticketHelper
    ) {
        VALIDATING_LEVELS.add(level);
        ACTIVE_TICKETS.remove(level);
        try {
            Map<BlockPos, Set<ChunkPos>> retainedByOwner = new HashMap<>();
            for (Map.Entry<BlockPos, Pair<LongSet, LongSet>> entry
                : ticketHelper.getBlockTickets().entrySet()) {
                BlockPos owner = entry.getKey().immutable();
                Pair<LongSet, LongSet> ticketSets = entry.getValue();

                // BuildCraft quarries require full ticks. Remove obsolete non-ticking tickets from older attempts.
                for (long chunk : ticketSets.getFirst().toLongArray()) {
                    ticketHelper.removeTicket(owner, chunk, false);
                }

                BlockEntity blockEntity = level.getBlockEntity(owner);
                if (!(blockEntity instanceof IChunkLoadingTile loadingTile)
                    || blockEntity.isRemoved()
                    || !canLoadFor(loadingTile)) {
                    ticketHelper.removeAllTickets(owner);
                    continue;
                }

                Set<ChunkPos> desired = getChunksToLoad(owner, loadingTile.getChunksToLoad());
                long ownerChunk = new ChunkPos(owner).toLong();
                if (!ticketSets.getSecond().contains(ownerChunk)) {
                    // Without its own chunk the owner cannot reliably load and refresh the rest of its area.
                    ticketHelper.removeAllTickets(owner);
                    continue;
                }

                Set<ChunkPos> retained = new HashSet<>();
                for (long chunkLong : ticketSets.getSecond().toLongArray()) {
                    ChunkPos chunk = new ChunkPos(chunkLong);
                    if (desired.contains(chunk)) {
                        retained.add(chunk);
                    } else {
                        ticketHelper.removeTicket(owner, chunkLong, FULLY_TICKING);
                    }
                }
                if (!retained.isEmpty()) {
                    retainedByOwner.put(owner, retained);
                }
            }
            if (!retainedByOwner.isEmpty()) {
                ACTIVE_TICKETS.put(level, retainedByOwner);
            }
        } finally {
            VALIDATING_LEVELS.remove(level);
        }
    }

    static boolean canLoadFor(IChunkLoadingTile tile) {
        IChunkLoadingTile.LoadType loadType = tile.getLoadType();
        return loadType != null
            && BCLibConfig.chunkLoadingLevel.canLoad(loadType)
            && (!(tile instanceof IHasWork hasWork) || hasWork.hasWork());
    }

    static Set<ChunkPos> getChunksToLoad(BlockPos owner, @Nullable Set<ChunkPos> requested) {
        Set<ChunkPos> chunks = new HashSet<>();
        if (requested != null) {
            chunks.addAll(requested);
        }
        chunks.add(new ChunkPos(owner));
        return Collections.unmodifiableSet(chunks);
    }

    static TicketDelta getTicketDelta(Set<ChunkPos> current, Set<ChunkPos> desired) {
        Set<ChunkPos> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);
        Set<ChunkPos> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);
        return new TicketDelta(
            Collections.unmodifiableSet(toAdd),
            Collections.unmodifiableSet(toRemove)
        );
    }

    private static void releaseTrackedChunksFor(ServerLevel level, BlockPos ownerPos) {
        BlockPos owner = ownerPos.immutable();
        Set<ChunkPos> chunks = removeTrackedChunks(level, owner);
        if (chunks == null) {
            return;
        }
        for (ChunkPos chunk : chunks) {
            ForgeChunkManager.forceChunk(
                level, BCLib.MODID, owner, chunk.x, chunk.z, false, FULLY_TICKING
            );
        }
    }
    private static void updateChunksFor(ServerLevel level, BlockPos ownerPos, Set<ChunkPos> desired) {
        BlockPos owner = ownerPos.immutable();
        Map<BlockPos, Set<ChunkPos>> levelTickets =
            ACTIVE_TICKETS.computeIfAbsent(level, ignored -> new HashMap<>());
        Set<ChunkPos> current = levelTickets.computeIfAbsent(owner, ignored -> new HashSet<>());
        TicketDelta delta = getTicketDelta(current, desired);

        for (ChunkPos chunk : delta.toRemove()) {
            ForgeChunkManager.forceChunk(
                level, BCLib.MODID, owner, chunk.x, chunk.z, false, FULLY_TICKING
            );
            current.remove(chunk);
        }
        for (ChunkPos chunk : delta.toAdd()) {
            ForgeChunkManager.forceChunk(
                level, BCLib.MODID, owner, chunk.x, chunk.z, true, FULLY_TICKING
            );
            // A false return also means that Forge already had this exact ticket, which is still the desired state.
            current.add(chunk);
        }
    }

    @Nullable
    private static Set<ChunkPos> removeTrackedChunks(ServerLevel level, BlockPos owner) {
        Map<BlockPos, Set<ChunkPos>> levelTickets = ACTIVE_TICKETS.get(level);
        if (levelTickets == null) {
            return null;
        }
        Set<ChunkPos> removed = levelTickets.remove(owner);
        if (levelTickets.isEmpty()) {
            ACTIVE_TICKETS.remove(level);
        }
        return removed;
    }

    record TicketDelta(Set<ChunkPos> toAdd, Set<ChunkPos> toRemove) {
    }
}
