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
import buildcraft.test.MinecraftTestBootstrap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ListMatchHandlerToolsTest {
    private final ListMatchHandlerTools handler = new ListMatchHandlerTools();

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void typeMatchingUsesForgeDigActionsAcrossToolTiers() {
        ItemStack ironPickaxe = new ItemStack(Items.IRON_PICKAXE);
        ItemStack diamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack ironAxe = new ItemStack(Items.IRON_AXE);

        assertTrue(handler.isValidSource(Type.TYPE, ironPickaxe));
        assertTrue(handler.matches(Type.TYPE, ironPickaxe, diamondPickaxe, true));
        assertFalse(handler.matches(Type.TYPE, ironPickaxe, ironAxe, false));
    }

    @Test
    void swordsAndShearsRemainDistinctToolClasses() {
        ItemStack ironSword = new ItemStack(Items.IRON_SWORD);
        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack shears = new ItemStack(Items.SHEARS);

        assertTrue(handler.matches(Type.TYPE, ironSword, diamondSword, true));
        assertFalse(handler.matches(Type.TYPE, ironSword, shears, false));
    }

    @Test
    void nonToolsAndNonTypeModesAreRejected() {
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);

        assertFalse(handler.isValidSource(Type.TYPE, stick));
        assertFalse(handler.isValidSource(Type.MATERIAL, pickaxe));
        assertFalse(handler.matches(Type.MATERIAL, pickaxe, pickaxe, true));
    }
}
