/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import buildcraft.lib.gui.slot.SlotDisplay;
import net.minecraft.world.inventory.Slot;

class BCMenuUtilTest {
    @Test
    void displaySlotsCannotBeQuickMoveSources() {
        assertFalse(BCMenuUtil.isQuickMoveSourceType(SlotDisplay.class));
    }

    @Test
    void ordinaryInventorySlotsRemainQuickMoveSources() {
        assertTrue(BCMenuUtil.isQuickMoveSourceType(Slot.class));
    }
}
