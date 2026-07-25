/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.list;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.api.lists.ListMatchHandler.Type;
import buildcraft.api.lists.ListRegistry;
import buildcraft.test.MinecraftTestBootstrap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ListMatchHandlerClassTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void registeredItemClassesCompareTheTargetItemRatherThanTheItemStackWrapper() {
        ListMatchHandlerClass handler = new ListMatchHandlerClass();
        ItemStack source = new ItemStack(Items.IRON_PICKAXE);
        ItemStack sameClass = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack differentClass = new ItemStack(Items.IRON_AXE);
        Class<? extends Item> groupedClass = source.getItem().getClass();

        boolean added = ListRegistry.itemClassAsType.add(groupedClass);
        try {
            assertTrue(handler.isValidSource(Type.TYPE, source));
            assertTrue(handler.matches(Type.TYPE, source, sameClass, true));
            assertFalse(handler.matches(Type.TYPE, source, differentClass, false));
        } finally {
            if (added) {
                ListRegistry.itemClassAsType.remove(groupedClass);
            }
        }
    }
}
