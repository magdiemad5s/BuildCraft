package buildcraft.factory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PortRuntimeRegressionContractTest {
    @Test
    void miningWellDoesNotExposeInfiniteRenderShapes() throws IOException {
        String source = read("src/main/java/buildcraft/factory/block/BlockMiningWell.java");

        assertFalse(source.contains("Shapes.INFINITY"));
        assertFalse(source.contains("getOcclusionShape("));
        assertFalse(source.contains("getVisualShape("));
    }

    @Test
    void canonicalTankNbtWinsOverLegacyFallback() throws IOException {
        String source = read("src/main/java/buildcraft/factory/tile/TileTank.java");
        int modernRead = source.indexOf("if (nbt.contains(\"tanks\", Tag.TAG_COMPOUND))");
        int legacyRead = source.indexOf(
            "if (!foundTankData && nbt.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND))"
        );

        assertTrue(modernRead >= 0, "The canonical tanks compound must be read");
        assertTrue(legacyRead > modernRead, "The released root tank tag must only be a fallback");
    }

    @Test
    void combustionMenuSurvivesClientBlockEntityLoadRace() throws IOException {
        String source = read("src/main/java/buildcraft/energy/menu/ContainerEngineIron_BC8.java");

        assertTrue(source.contains("tile == null ? new Tank(\"fuel\""));
        assertTrue(source.contains("tile == null ? new Tank(\"coolant\""));
        assertTrue(source.contains("tile == null ? new Tank(\"residue\""));
        assertTrue(source.contains("player.level().isClientSide || super.stillValid(player)"));
    }

    @Test
    void engineAndWorkbenchScreensRenderSafelyDuringLoadRace() throws IOException {
        String iron = read("src/main/java/buildcraft/energy/client/gui/GuiEngineIron_BC8.java");
        String stone = read("src/main/java/buildcraft/energy/client/gui/GuiEngineStone_BC8.java");
        String workbench = read("src/main/java/buildcraft/factory/gui/GuiAutoCraftItems.java");

        assertTrue(iron.contains("container::getTile"));
        assertFalse(iron.contains("container.tile"));
        assertTrue(stone.contains("container::getTile"));
        assertTrue(stone.contains("TileEngineStone_BC8 tile = container.getTile()"));
        assertTrue(stone.contains("tile == null ? 0.0"));
        assertFalse(stone.contains("container.tile"));
        assertTrue(workbench.contains("TileAutoWorkbenchItems tile = container.getTile()"));
        assertTrue(workbench.contains("tile == null ? 0.0"));
        assertFalse(workbench.contains("container.tile"));
    }

    @Test
    void heatExchangerInvalidatesDetachedCapabilitiesAndRevivesReattachments() throws IOException {
        String source = read("src/main/java/buildcraft/factory/tile/TileHeatExchange.java");

        assertTrue(source.contains("exchange.section.caps.invalidate();"));
        assertTrue(source.contains("removedSection.caps.invalidate();"));
        assertTrue(source.contains("section.caps.revive();"));
        assertTrue(source.contains("public void invalidateCaps()"));
        assertTrue(source.contains("public void reviveCaps()"));
        assertTrue(source.contains("start.caps.revive();"));
        assertTrue(source.contains("end.caps.revive();"));
    }

    @Test
    void floodGateUsesModernEmptyFluidSentinelsAndFlowingDetection() throws IOException {
        String blockUtil = read("src/main/java/buildcraft/lib/misc/BlockUtil.java");
        String floodGate = read("src/main/java/buildcraft/factory/tile/TileFloodGate.java");

        assertTrue(blockUtil.contains("if (!fs.isEmpty())"));
        assertTrue(blockUtil.contains("return fs.getType();"));
        assertTrue(floodGate.contains("fluid != Fluids.EMPTY"));
        assertTrue(floodGate.contains("getLocalState(offsetPos)) == Fluids.EMPTY"));
        assertFalse(floodGate.contains("getFluidWithoutFlowing(getLocalState(offsetPos)) == null"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
