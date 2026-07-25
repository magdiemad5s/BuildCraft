package buildcraft.transport.pipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PipeCapabilityLifecycleContractTest {
    @Test
    void pipeAndHolderPropagateForgeCapabilityLifecycle() throws IOException {
        String pipe = read("src/main/java/buildcraft/transport/pipe/Pipe.java");
        String holder = read("src/main/java/buildcraft/transport/tile/TilePipeHolder.java");

        assertTrue(pipe.contains("cachedCapabilities"));
        assertTrue(pipe.contains("provided.addListener"));
        assertTrue(pipe.contains("flow.invalidateCapabilities();"));
        assertTrue(pipe.contains("flow.reviveCapabilities();"));
        assertTrue(holder.contains("public void invalidateCaps()"));
        assertTrue(holder.contains("pipe.invalidateCapabilities();"));
        assertTrue(holder.contains("public void reviveCaps()"));
        assertTrue(holder.contains("pipe.reviveCapabilities();"));
    }

    @Test
    void mjFluidAndRfFlowsExposeStableCachedHandles() throws IOException {
        String mj = read("src/main/java/buildcraft/transport/pipe/flow/PipeFlowPower.java");
        String fluid = read("src/main/java/buildcraft/transport/pipe/flow/PipeFlowFluids.java");
        String rf = read("src/main/java/buildcraft/transport/pipe/flow/PipeFlowRedstoneFlux.java");

        assertTrue(mj.contains("getCachedCapability(MjAPI.CAP_RECEIVER, facing"));
        assertTrue(mj.contains("getCachedCapability(MjAPI.CAP_CONNECTOR, facing"));
        assertTrue(fluid.contains("CapUtil.CAP_FLUIDS, facing"));
        assertTrue(rf.contains("getCachedCapability(ForgeCapabilities.ENERGY, facing"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
