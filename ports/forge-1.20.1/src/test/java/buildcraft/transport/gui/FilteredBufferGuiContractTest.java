/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.transport.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FilteredBufferGuiContractTest {
    @Test
    void rendersAllNineSynchronizedFiltersBehindTheOriginalBufferRow() throws IOException {
        String menu = read(
            "src/main/java/buildcraft/transport/container/ContainerFilteredBuffer_BC8.java"
        );
        String screen = read(
            "src/main/java/buildcraft/transport/gui/GuiFilteredBuffer.java"
        );

        assertTrue(menu.contains("public final SlotPhantom[] filterSlots = new SlotPhantom[9]"));
        assertTrue(menu.contains("filterSlots[i] = filterSlot"));
        assertTrue(screen.contains("i < container.filterSlots.length"));
        assertTrue(screen.contains("container.filterSlots[i].getItem()"));
        assertTrue(screen.contains("+ 8 + i * 18"));
        assertTrue(screen.contains("+ 61"));
        assertTrue(screen.contains("guiGraphics.renderItem(stack, currentX, currentY)"));
        assertTrue(screen.contains("BCTransportSprites.NOTHING_FILTERED_BUFFER_SLOT"));
        assertTrue(screen.contains("guiGraphics.setColor(1, 1, 1, 0.7f)"));
        assertFalse(screen.contains("container.tile"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
