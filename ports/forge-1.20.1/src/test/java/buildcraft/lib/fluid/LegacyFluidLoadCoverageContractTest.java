package buildcraft.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class LegacyFluidLoadCoverageContractTest {
    @Test
    void everyProductionFluidStackNbtLoadUsesTheCompatibilityBoundary() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> directLoaders;
        try (var files = Files.walk(sourceRoot)) {
            directLoaders = files
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> {
                    try {
                        return Files.readString(path).contains("FluidStack.loadFluidStackFromNBT");
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to inspect " + path, exception);
                    }
                })
                .map(path -> sourceRoot.relativize(path).toString().replace('\\', '/'))
                .sorted()
                .toList();
        }

        assertEquals(List.of("buildcraft/lib/fluid/LegacyFluidCompat.java"), directLoaders);
    }
}
