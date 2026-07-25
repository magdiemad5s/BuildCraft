package buildcraft.lib.misc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class AdvancementOwnerContractTest {
    private static final List<String> GUARDED_TILE_SOURCES = List.of(
        "src/main/java/buildcraft/lib/engine/TileEngineBase_BC8.java",
        "src/main/java/buildcraft/factory/tile/TilePump.java",
        "src/main/java/buildcraft/factory/tile/TileFloodGate.java",
        "src/main/java/buildcraft/factory/tile/TileChute.java",
        "src/main/java/buildcraft/transport/tile/TilePipeHolder.java",
        "src/main/java/buildcraft/builders/tile/TileArchitectTable.java",
        "src/main/java/buildcraft/silicon/tile/TileAssemblyTable.java"
    );

    @Test
    void guardedAdvancementPathsDoNotDereferenceFallbackOwnersDirectly() {
        for (String sourcePath : GUARDED_TILE_SOURCES) {
            assertFalse(read(sourcePath).contains("getOwner().getId()"), sourcePath);
        }
        assertTrue(read("src/main/java/buildcraft/energy/tile/TileSpringOil.java")
            .contains("profile == null || profile.getId() == null"));
    }

    private static String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
