/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.recipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import buildcraft.api.recipes.IngredientStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

class IngredientStackAllocationTest {
    @Test
    void allocatesOneCountedRequirementAcrossMultipleSlots() {
        List<ItemStack> stacks = List.of(
            new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.IRON_INGOT)
        );

        int[] allocation = IngredientStackAllocation.find(
            stacks.size(),
            stacks::get,
            List.of(new IngredientStack(Ingredient.of(Items.IRON_INGOT), 2)),
            false
        );

        assertNotNull(allocation);
        assertArrayEquals(new int[] {1, 1}, allocation);
    }

    @Test
    void reroutesOverlappingIngredientsInsteadOfDoubleAllocatingOneItem() {
        List<ItemStack> stacks = List.of(
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.IRON_INGOT)
        );

        int[] allocation = IngredientStackAllocation.find(
            stacks.size(),
            stacks::get,
            List.of(
                new IngredientStack(Ingredient.of(Items.DIAMOND, Items.IRON_INGOT)),
                new IngredientStack(Ingredient.of(Items.DIAMOND))
            ),
            false
        );

        assertNotNull(allocation);
        assertArrayEquals(new int[] {1, 1}, allocation);
    }

    @Test
    void preciseMatchingAllowsRemaindersInParticipatingStacksButRejectsExtraStacks() {
        List<ItemStack> oneStack = List.of(new ItemStack(Items.IRON_INGOT, 64));
        int[] allocation = IngredientStackAllocation.find(
            oneStack.size(),
            oneStack::get,
            List.of(new IngredientStack(Ingredient.of(Items.IRON_INGOT), 2)),
            true
        );
        assertNotNull(allocation);
        assertArrayEquals(new int[] {2}, allocation);

        List<ItemStack> withExtra = List.of(
            new ItemStack(Items.IRON_INGOT, 64),
            new ItemStack(Items.GOLD_INGOT)
        );
        assertNull(IngredientStackAllocation.find(
            withExtra.size(),
            withExtra::get,
            List.of(new IngredientStack(Ingredient.of(Items.IRON_INGOT), 2)),
            true
        ));
    }

    @Test
    void rejectsIncompleteAndInvalidRequirements() {
        List<ItemStack> stacks = List.of(new ItemStack(Items.IRON_INGOT));
        assertNull(IngredientStackAllocation.find(
            stacks.size(),
            stacks::get,
            List.of(new IngredientStack(Ingredient.of(Items.IRON_INGOT), 2)),
            false
        ));
        assertNull(IngredientStackAllocation.find(
            stacks.size(),
            stacks::get,
            List.of(new IngredientStack(Ingredient.of(Items.IRON_INGOT), -1)),
            false
        ));
    }
}
