/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.silicon.container;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.items.IItemHandler;

/**
 * Converts a server-owned crafting recipe into the Advanced Crafting Table's
 * legacy 3x3 phantom blueprint.
 *
 * <p>The released 1.12.2 GUI centered one-wide and one-high shaped recipes,
 * left larger recipes at the top-left, and laid shapeless recipes out in row
 * order. This class preserves that layout while choosing an ingredient
 * representative that is already present in the table whenever possible.</p>
 */
public final class AdvancedCraftingRecipeSelection {
    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;

    private AdvancedCraftingRecipeSelection() {
    }

    /**
     * Builds a complete nine-slot blueprint, or returns empty when the recipe
     * cannot be represented safely by the legacy grid.
     */
    public static Optional<NonNullList<ItemStack>> createBlueprint(
        CraftingRecipe recipe,
        IItemHandler materials
    ) {
        if (!isRepresentable(recipe) || materials == null) {
            return Optional.empty();
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        int recipeWidth = GRID_WIDTH;
        int recipeHeight = GRID_HEIGHT;
        if (recipe instanceof IShapedRecipe<?> shaped) {
            recipeWidth = shaped.getRecipeWidth();
            recipeHeight = shaped.getRecipeHeight();
        }

        int[] layout;
        try {
            layout = layoutIngredientIndexes(recipeWidth, recipeHeight, ingredients.size());
        } catch (IllegalArgumentException invalidDimensions) {
            return Optional.empty();
        }

        int[] availableCounts = snapshotAvailableCounts(materials);
        NonNullList<ItemStack> blueprint = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            int ingredientIndex = layout[slot];
            if (ingredientIndex < 0) {
                continue;
            }

            Ingredient ingredient = ingredients.get(ingredientIndex);
            if (ingredient.isEmpty()) {
                continue;
            }

            ItemStack representative = selectRepresentative(ingredient, materials, availableCounts);
            if (representative.isEmpty()) {
                return Optional.empty();
            }
            blueprint.set(slot, representative);
        }
        return Optional.of(blueprint);
    }

    public static boolean isRepresentable(CraftingRecipe recipe) {
        if (recipe == null || recipe.isSpecial() || !recipe.canCraftInDimensions(GRID_WIDTH, GRID_HEIGHT)) {
            return false;
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty() || ingredients.size() > GRID_SIZE) {
            return false;
        }

        if (recipe instanceof IShapedRecipe<?> shaped) {
            int width = shaped.getRecipeWidth();
            int height = shaped.getRecipeHeight();
            return width >= 1 && width <= GRID_WIDTH
                && height >= 1 && height <= GRID_HEIGHT
                && ingredients.size() <= width * height;
        }
        return true;
    }

    /**
     * Returns a grid-slot to ingredient-index map. Empty grid slots contain
     * {@code -1}. Package-private for focused layout tests.
     */
    static int[] layoutIngredientIndexes(int recipeWidth, int recipeHeight, int ingredientCount) {
        if (recipeWidth < 1 || recipeWidth > GRID_WIDTH
            || recipeHeight < 1 || recipeHeight > GRID_HEIGHT
            || ingredientCount < 1 || ingredientCount > recipeWidth * recipeHeight) {
            throw new IllegalArgumentException(
                "Invalid recipe layout " + recipeWidth + "x" + recipeHeight
                    + " with " + ingredientCount + " ingredients"
            );
        }

        int[] layout = new int[GRID_SIZE];
        Arrays.fill(layout, -1);
        int offsetX = recipeWidth == 1 ? 1 : 0;
        int offsetY = recipeHeight == 1 ? 1 : 0;

        for (int y = 0; y < recipeHeight; y++) {
            for (int x = 0; x < recipeWidth; x++) {
                int ingredientIndex = x + y * recipeWidth;
                if (ingredientIndex >= ingredientCount) {
                    continue;
                }
                int gridIndex = x + offsetX + (y + offsetY) * GRID_WIDTH;
                layout[gridIndex] = ingredientIndex;
            }
        }
        return layout;
    }

    static int[] snapshotAvailableCounts(IItemHandler materials) {
        int[] counts = new int[materials.getSlots()];
        for (int slot = 0; slot < counts.length; slot++) {
            ItemStack stack = materials.getStackInSlot(slot);
            counts[slot] = stack.isEmpty() ? 0 : Math.max(0, stack.getCount());
        }
        return counts;
    }

    /**
     * Prefers a real material already held by the table. The per-slot count
     * snapshot prevents one available item from being selected for multiple
     * recipe positions. If no held material matches, the recipe's first
     * advertised representative is used, matching the released GUI.
     */
    static ItemStack selectRepresentative(
        Ingredient ingredient,
        IItemHandler materials,
        int[] availableCounts
    ) {
        if (availableCounts.length != materials.getSlots()) {
            throw new IllegalArgumentException("Material count snapshot does not match the handler");
        }

        for (int slot = 0; slot < materials.getSlots(); slot++) {
            if (availableCounts[slot] <= 0) {
                continue;
            }
            ItemStack material = materials.getStackInSlot(slot);
            if (!material.isEmpty() && ingredient.test(material)) {
                availableCounts[slot]--;
                return singleCopy(material);
            }
        }

        for (ItemStack candidate : ingredient.getItems()) {
            if (!candidate.isEmpty()) {
                return singleCopy(candidate);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack singleCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
