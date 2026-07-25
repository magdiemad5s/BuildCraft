/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuReplacerSlotPolicyTest {
    @Test
    void quickMoveOnlyAcceptsIndexesInsideTheMenuSlotList() {
        assertFalse(MenuReplacer.isValidSlotIndex(-1, 39));
        assertTrue(MenuReplacer.isValidSlotIndex(0, 39));
        assertTrue(MenuReplacer.isValidSlotIndex(38, 39));
        assertFalse(MenuReplacer.isValidSlotIndex(39, 39));
        assertFalse(MenuReplacer.isValidSlotIndex(Integer.MAX_VALUE, 39));
    }

    @Test
    void anEmptySlotListRejectsEveryIndex() {
        assertFalse(MenuReplacer.isValidSlotIndex(0, 0));
    }
}
