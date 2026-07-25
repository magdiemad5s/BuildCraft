package buildcraft.lib.chunkload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import buildcraft.api.tiles.IHasWork;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.BCLibConfig.ChunkLoaderLevel;
import buildcraft.lib.chunkload.IChunkLoadingTile.LoadType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class ChunkLoaderManagerTest {
    private final ChunkLoaderLevel originalLevel = BCLibConfig.chunkLoadingLevel;

    @AfterEach
    void restoreConfig() {
        BCLibConfig.chunkLoadingLevel = originalLevel;
    }

    @Test
    void requestedAreaAlwaysIncludesStableOwnerChunk() {
        BlockPos owner = new BlockPos(31, 70, -17);
        ChunkPos ownerChunk = new ChunkPos(owner);
        ChunkPos additional = new ChunkPos(4, 5);

        Set<ChunkPos> normalized = ChunkLoaderManager.getChunksToLoad(owner, Set.of(additional));

        assertEquals(Set.of(ownerChunk, additional), normalized);
        assertThrows(UnsupportedOperationException.class, () -> normalized.add(new ChunkPos(9, 9)));
        assertEquals(Set.of(ownerChunk), ChunkLoaderManager.getChunksToLoad(owner, null));
    }

    @Test
    void ticketDeltaOnlyChangesChunksThatActuallyDiffer() {
        ChunkPos retained = new ChunkPos(0, 0);
        ChunkPos stale = new ChunkPos(1, 0);
        ChunkPos added = new ChunkPos(2, 0);

        ChunkLoaderManager.TicketDelta delta = ChunkLoaderManager.getTicketDelta(
            Set.of(retained, stale),
            Set.of(retained, added)
        );

        assertEquals(Set.of(added), delta.toAdd());
        assertEquals(Set.of(stale), delta.toRemove());
    }

    @Test
    void configLoadTypeAndWorkStateAllGateTickets() {
        BCLibConfig.chunkLoadingLevel = ChunkLoaderLevel.STRICT_TILES;
        assertTrue(ChunkLoaderManager.canLoadFor(new TestLoadingTile(LoadType.HARD, true)));
        assertFalse(ChunkLoaderManager.canLoadFor(new TestLoadingTile(LoadType.SOFT, true)));
        assertFalse(ChunkLoaderManager.canLoadFor(new TestLoadingTile(LoadType.HARD, false)));
        assertFalse(ChunkLoaderManager.canLoadFor(new TestLoadingTile(null, true)));

        BCLibConfig.chunkLoadingLevel = ChunkLoaderLevel.SELF_TILES;
        assertTrue(ChunkLoaderManager.canLoadFor(new TestLoadingTile(LoadType.SOFT, true)));

        BCLibConfig.chunkLoadingLevel = ChunkLoaderLevel.NONE;
        assertFalse(ChunkLoaderManager.canLoadFor(new TestLoadingTile(LoadType.HARD, true)));
    }

    private record TestLoadingTile(LoadType loadType, boolean hasWork)
        implements IChunkLoadingTile, IHasWork {
        @Override
        public LoadType getLoadType() {
            return loadType;
        }
    }
}
