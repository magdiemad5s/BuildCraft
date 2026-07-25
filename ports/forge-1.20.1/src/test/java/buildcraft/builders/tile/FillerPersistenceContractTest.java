/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0
 */
package buildcraft.builders.tile;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FillerPersistenceContractTest {
    @Test
    void addonIdentitySurvivesLoadBeforeLevelAssignment() throws IOException {
        String source = readTile();
        assertTrue(source.contains("pendingAddonVolumeBoxId = nbt.contains"));
        assertTrue(source.contains("pendingAddonSlot = pendingAddonVolumeBoxId == null"));
        assertTrue(source.indexOf("public void onLoad()") < source.lastIndexOf("resolvePersistedAddon();"));
    }

    @Test
    void reservedBuilderTransactionsAreRestoredAfterBuildingInfoExists() throws IOException {
        String source = readTile();
        assertTrue(source.contains("pendingBuilderNbt = nbt.contains(\"builder\")"));
        int onLoad = source.indexOf("public void onLoad()");
        int updateInfo = source.indexOf("updateBuildingInfo();", onLoad);
        int restore = source.indexOf("currentBuilder.deserializeNBT(pendingBuilderNbt)", onLoad);
        assertTrue(onLoad >= 0 && updateInfo > onLoad && restore > updateInfo);
    }

    @Test
    void cancelledBlueprintItemsCannotDisappearWhenTheInventoryRefills() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/buildcraft/builders/snapshot/BlueprintBuilder.java")
        );
        assertTrue(source.contains("ItemStack remainder = tile.getInvResources().insert(stack, false, false)"));
        assertTrue(source.contains("Block.popResource(serverLevel, tile.getBuilderPos(), remainder)"));
    }

    private static String readTile() throws IOException {
        return Files.readString(Path.of("src/main/java/buildcraft/builders/tile/TileFiller.java"));
    }
}
