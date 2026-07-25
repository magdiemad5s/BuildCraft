/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class MenuClientLoadRaceContractTest {
    private static final List<Path> NULLABLE_TILE_SCREENS = List.of(
        Path.of("src/main/java/buildcraft/builders/gui/GuiArchitectTable.java"),
        Path.of("src/main/java/buildcraft/builders/gui/GuiBuilder.java"),
        Path.of("src/main/java/buildcraft/builders/gui/GuiElectronicLibrary.java"),
        Path.of("src/main/java/buildcraft/silicon/gui/GuiAdvancedCraftingTable.java"),
        Path.of("src/main/java/buildcraft/silicon/gui/GuiAssemblyTable.java"),
        Path.of("src/main/java/buildcraft/silicon/gui/GuiChargingTable.java"),
        Path.of("src/main/java/buildcraft/silicon/gui/GuiIntegrationTable.java"),
        Path.of("src/main/java/buildcraft/silicon/gui/GuiProgrammingTable.java")
    );

    @Test
    void tileMenusRetryClientBlockEntityResolutionWithoutWeakeningTheServerGate() throws IOException {
        String source = read("src/main/java/buildcraft/lib/gui/ContainerBCTile.java");

        assertTrue(source.contains("public T getTile()"));
        assertTrue(source.contains("if (tile == null)"));
        assertTrue(source.contains("tile = resolveTile();"));
        assertTrue(source.contains(": player.level().isClientSide"));
        assertTrue(source.contains("resolved.canInteractWith(player)"));
    }

    @Test
    void builderAlwaysCreatesFourOrderedFluidWidgets() throws IOException {
        String source = read("src/main/java/buildcraft/builders/menu/ContainerBuilder.java");

        assertTrue(source.contains("List<Tank> tanks = new ArrayList<>(4)"));
        assertTrue(source.contains("for (int i = 0; i < 4; i++)"));
        assertTrue(source.contains("new Tank(\"tank\" + (i + 1)"));
        assertTrue(source.contains("builder.getTankManager().stream()"));
        assertFalse(source.contains("tile.getTankManager()"));
    }

    @Test
    void affectedScreensResolveOrGuardTheNullableTile() throws IOException {
        for (Path screen : NULLABLE_TILE_SCREENS) {
            String source = Files.readString(screen);
            assertFalse(source.contains("container.tile"), () -> screen + " directly dereferences the race-prone tile field");
            assertTrue(
                source.contains("container.getTile()") || source.contains("container::getTile"),
                () -> screen + " does not use delayed tile resolution"
            );
        }

        String ledger = read("src/main/java/buildcraft/silicon/gui/LedgerTablePower.java");
        assertTrue(ledger.contains("Supplier<? extends TileLaserTableBase>"));
        assertTrue(ledger.contains("tile == null ? 0"));

        String advanced = read("src/main/java/buildcraft/silicon/gui/GuiAdvancedCraftingTable.java");
        assertTrue(advanced.contains("tryInitializeRecipeBook()"));
        assertTrue(advanced.contains("if (tile == null)"));
        assertTrue(advanced.contains("isRecipeBookAvailable()"));
        assertTrue(advanced.contains("restoreCraftableFilter();"));
        assertTrue(advanced.contains("super.removed();"));
        assertFalse(advanced.contains("recipeBook.removed();"));
    }
    @Test
    void deferredScreensInitializeAfterClientTileArrival() throws IOException {
        String baseScreen = read("src/main/java/buildcraft/lib/gui/GuiBC8.java");
        assertTrue(baseScreen.contains("ownerLedgerPending = shouldAddOwnerLedger()"));
        assertTrue(baseScreen.contains("private void tryAddOwnerLedger()"));
        assertTrue(baseScreen.contains("TileBC_Neptune tile = tileMenu.getBCTile()"));
        assertTrue(baseScreen.contains("tryAddOwnerLedger();"));

        String filler = read("src/main/java/buildcraft/builders/gui/GuiFiller.java");
        assertTrue(filler.contains("private boolean tryLoadJson()"));
        assertTrue(filler.contains("TileFiller tile = container.getTile()"));
        assertTrue(filler.contains("if (tile == null)"));
        assertTrue(filler.contains("preLoad(jsonGui, tile);"));
        assertTrue(filler.contains("jsonGui.load();"));
        assertTrue(filler.contains("private static final int SIZE_Y = 241"));

        String autoWorkbench = read("src/main/java/buildcraft/factory/gui/GuiAutoCraftItems.java");
        assertTrue(autoWorkbench.contains("private void tryInitializeRecipeBook()"));
        assertTrue(autoWorkbench.contains("recipeBook.init(width, height, minecraft"));
        assertTrue(autoWorkbench.contains("tryInitializeRecipeBook();"));
        assertTrue(autoWorkbench.contains("restoreCraftableFilter();"));
        assertTrue(autoWorkbench.contains("super.removed();"));
        assertFalse(autoWorkbench.contains("recipeBook.removed();"));

        String tank = read("src/main/java/buildcraft/factory/gui/GuiTank.java");
        assertTrue(tank.contains("private void tryAddTankHelp()"));
        assertTrue(tank.contains("TileTank tile = container.getTile()"));
        assertTrue(tank.contains("tankHelpAdded = true;"));

        String zonePlanner = read("src/main/java/buildcraft/robotics/gui/GuiZonePlanner.java");
        assertTrue(zonePlanner.contains("private void tryInitializeTileState()"));
        assertTrue(zonePlanner.contains("TileZonePlanner tile = container.getTile()"));
        assertTrue(zonePlanner.contains("suppressNameResponder = true;"));
        assertTrue(zonePlanner.contains("tileStateInitialized = true;"));
    }

    @Test
    void engineAndDynamoLedgersResolveLateClientTiles() throws IOException {
        String engineLedger = read("src/main/java/buildcraft/lib/gui/ledger/LedgerEngine.java");
        assertTrue(engineLedger.contains("Supplier<? extends TileEngineBase_BC8>"));
        assertTrue(engineLedger.contains("engine == null ? 0"));
        assertTrue(engineLedger.contains("BCLibSprites.ENGINE_INACTIVE"));

        String dynamoLedger = read("src/main/java/buildcraft/energy/client/gui/LedgerDynamoMJ.java");
        assertTrue(dynamoLedger.contains("Supplier<? extends TileDynamoMJ>"));
        assertTrue(dynamoLedger.contains("dynamo == null ? 0"));
        assertTrue(dynamoLedger.contains("BCLibSprites.ENGINE_INACTIVE"));

        for (String screen : List.of(
            "GuiEngineStone_BC8.java",
            "GuiEngineIron_BC8.java",
            "GuiEngineRF.java",
            "GuiDynamoMJ.java"
        )) {
            String source = read("src/main/java/buildcraft/energy/client/gui/" + screen);
            assertTrue(source.contains("container::getTile"), screen + " does not use a lazy tile supplier");
        }
    }

    @Test
    void requesterUsesServerOwnedTemplatesAndTheReleasedGuiLayout()
        throws IOException, NoSuchAlgorithmException {
        String menu = read("src/main/java/buildcraft/robotics/container/ContainerRequester.java");
        assertTrue(menu.contains("if (!player.level().isClientSide)"));
        assertTrue(menu.contains("requester.canInteractWith(player)"));
        assertTrue(menu.contains("requester.setRequest(slot.handlerIndex, toSet)"));
        assertTrue(menu.contains("addFullPlayerInventory(19, 101)"));
        assertTrue(menu.contains("9 + x * 18, 7 + y * 18"));
        assertTrue(menu.contains("117 + x * 18, 7 + y * 18"));

        String tile = read("src/main/java/buildcraft/robotics/tile/TileRequester.java");
        assertTrue(tile.contains("nbt.put(\"inv\", writeLegacyInventory(inv))"));
        assertTrue(tile.contains("nbt.put(\"req\", writeLegacyInventory(requests))"));
        assertTrue(tile.contains("loadLegacyInventory(nbt.getCompound(\"req\")"));

        String screen = read("src/main/java/buildcraft/robotics/gui/GuiRequester.java");
        assertTrue(screen.contains("private static final int SIZE_X = 196"));
        assertTrue(screen.contains("private static final int SIZE_Y = 181"));
        assertTrue(screen.contains("GuiHelpUtil.addSlots(mainGui, 9, 7, 4, 5"));
        assertTrue(screen.contains("GuiHelpUtil.addSlots(mainGui, 117, 7, 4, 5"));

        Path texture = Path.of(
            "src/main/resources/assets/buildcraftrobotics/textures/gui/requester_gui.png"
        );
        BufferedImage image = ImageIO.read(texture.toFile());
        assertTrue(image != null, "Requester GUI PNG could not be decoded");
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        String sha256 = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture))
        );
        assertEquals("28fc455a571966c928de4651be1b40885339e97d12f3d70d9972ca35034236bc", sha256);
    }


    @Test
    void missingGateHolderUsesANonNullClosableFallback() throws IOException {
        String menu = read("src/main/java/buildcraft/silicon/container/ContainerGate.java");
        String screen = read("src/main/java/buildcraft/silicon/gui/GuiGate.java");

        assertTrue(menu.contains("return new ContainerGate(containerId, playerInventory);"));
        assertTrue(menu.contains("public boolean isValidGateMenu()"));
        assertTrue(menu.contains("super(playerInventory, BCSiliconGuis.MENU_GATE.get(), containerId, null)"));
        assertTrue(screen.contains("if (!container.isValidGateMenu())"));
        assertTrue(screen.contains("minecraft.player.closeContainer()"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
