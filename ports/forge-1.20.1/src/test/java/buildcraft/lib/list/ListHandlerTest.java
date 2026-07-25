/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ListHandlerTest {
    @Test
    void oversizedLegacyStackListIsClampedToTheNineSlotContract() {
        CompoundTag data = new CompoundTag();
        ListTag stacks = new ListTag();
        for (int index = 0; index < ListHandler.WIDTH + 3; index++) {
            CompoundTag stackTag = new CompoundTag();
            new ItemStack(Items.STONE, index + 1).save(stackTag);
            stacks.add(stackTag);
        }
        data.put("st", stacks);
        data.putBoolean("Fp", true);
        data.putBoolean("Ft", true);
        data.putBoolean("Fm", true);

        ListHandler.Line line = ListHandler.Line.fromNBT(data);

        assertEquals(ListHandler.WIDTH, line.stacks.size());
        assertEquals(ListHandler.WIDTH, line.getStack(ListHandler.WIDTH - 1).getCount());
        assertTrue(line.precise);
        assertTrue(line.byType);
        assertTrue(line.byMaterial);
    }
}
