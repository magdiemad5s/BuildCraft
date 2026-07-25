/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class AdvancedCraftingGameTests {
    private AdvancedCraftingGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selectedRecipeBuildsAndCraftsUsingServerRecipeData(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        helper.setBlock(tablePos, BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            if (!(helper.getBlockEntity(tablePos) instanceof TileAdvancedCraftingTable table)) {
                helper.fail("Advanced Crafting Table block entity was not created");
                return;
            }

            Recipe<?> found = helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("minecraft", "stick"))
                .orElse(null);
            if (!(found instanceof CraftingRecipe stickRecipe)) {
                helper.fail("Vanilla stick recipe was not loaded as a crafting recipe");
                return;
            }

            table.invMaterials.setStackInSlot(0, new ItemStack(Items.BIRCH_PLANKS));
            table.invMaterials.setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
            if (!table.applyRecipeSelection(stickRecipe)) {
                helper.fail("Server recipe selection did not populate the phantom blueprint");
                return;
            }
            if (!table.invBlueprint.getStackInSlot(1).is(Items.BIRCH_PLANKS)
                || !table.invBlueprint.getStackInSlot(4).is(Items.OAK_PLANKS)) {
                helper.fail("The centered stick blueprint did not preserve both available plank choices");
                return;
            }

            table.getWorkbenchCrafting().tick();
            if (!table.getWorkbenchCrafting().canCraft()) {
                helper.fail("Advanced Crafting Table did not recognize its stored materials");
                return;
            }

            table.power = table.getTarget();
            table.update();
            ItemStack result = table.invResults.getStackInSlot(0);
            if (!result.is(Items.STICK) || result.getCount() != 4) {
                helper.fail("Advanced Crafting Table produced " + result + " instead of 4 sticks");
                return;
            }
            if (!table.invMaterials.getStackInSlot(0).isEmpty()
                || !table.invMaterials.getStackInSlot(1).isEmpty()) {
                helper.fail("Advanced Crafting Table did not consume both plank ingredients");
                return;
            }
            helper.succeed();
        });
    }
}
