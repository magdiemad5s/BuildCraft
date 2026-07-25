package buildcraft.builders.tile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class QuarryTicketLifecycleContractTest {
    @Test
    void unloadCallbacksRetainTicketsAndTrueBlockRemovalReleasesThem() throws IOException {
        String tileSource = read("src/main/java/buildcraft/builders/tile/TileQuarry.java");
        String baseBlockSource = read("src/main/java/buildcraft/lib/block/BlockBCTile_Neptune.java");
        String blockSource = read("src/main/java/buildcraft/builders/block/BlockQuarry.java");

        assertFalse(
            tileSource.contains("public void setRemoved()"),
            "Vanilla invokes setRemoved during chunk unload, so it must not release persisted tickets"
        );
        assertFalse(
            tileSource.contains("public void onChunkUnloaded()"),
            "The Forge chunk-unload callback must not release persisted tickets"
        );
        assertFalse(
            blockSource.contains("quarry.onRemove("),
            "The quarry block must not duplicate the centralized tile removal callback"
        );
        assertTrue(
            baseBlockSource.contains("tileBC.handleRemoval(true);"),
            "Actual block replacement must run the centralized tile removal lifecycle"
        );
    }

    @Test
    void persistedTicketPruningIteratesSnapshots() throws IOException {
        String managerSource = read("src/main/java/buildcraft/lib/chunkload/ChunkLoaderManager.java");

        assertTrue(managerSource.contains("ticketSets.getFirst().toLongArray()"));
        assertTrue(managerSource.contains("ticketSets.getSecond().toLongArray()"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
