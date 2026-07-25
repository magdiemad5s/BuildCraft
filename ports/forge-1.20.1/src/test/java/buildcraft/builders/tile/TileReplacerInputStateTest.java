/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class TileReplacerInputStateTest {
    @Test
    void equalInputStateIsRecognizedWithoutReducingItToAnIntegerHash() {
        CompoundTag input = new CompoundTag();
        input.putString("snapshot", "blueprint");
        input.putString("from", "stone");
        input.putString("to", "glass");

        assertTrue(TileReplacer.isSameSkippedInput(input, input.copy()));
        assertFalse(TileReplacer.isSameSkippedInput(null, input));
    }

    @Test
    void distinctNbtWithTheSameHashIsNeverTreatedAsTheSameSkippedInput() {
        CompoundTag first = new CompoundTag();
        first.putInt("Aa", 1);
        CompoundTag second = new CompoundTag();
        second.putInt("BB", 1);

        assertEquals(first.hashCode(), second.hashCode(), "The fixture must exercise a real 32-bit hash collision");
        assertFalse(first.equals(second));
        assertFalse(TileReplacer.isSameSkippedInput(first, second));
    }
}
