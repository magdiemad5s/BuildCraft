/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.lib.gui.recipe;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeBookPagePhantom extends RecipeBookPage {
    private final GuiRecipeBookPhantom gui;

    public RecipeBookPagePhantom(GuiRecipeBookPhantom gui) throws ReflectiveOperationException {
        this.gui = gui;
        replaceRecipeButtons();
    }

    @SuppressWarnings("unchecked")
    private void replaceRecipeButtons() throws IllegalAccessException {
        boolean replaced = false;
        for (Field field : RecipeBookPage.class.getDeclaredFields()) {
            if (field.getType() != List.class) {
                continue;
            }
            field.setAccessible(true);
            List<Object> values = (List<Object>) field.get(this);
            if (values == null || values.isEmpty() || !(values.get(0) instanceof RecipeButton)) {
                continue;
            }
            for (int index = 0; index < values.size(); index++) {
                values.set(index, new GuiButtonRecipePhantom());
            }
            replaced = true;
            break;
        }
        if (!replaced) {
            throw new IllegalAccessException("Unable to locate RecipeBookPage recipe buttons");
        }
    }

    @Override
    public boolean mouseClicked(
        double mouseX,
        double mouseY,
        int mouseButton,
        int pageX,
        int pageY,
        int pageWidth,
        int pageHeight
    ) {
        if (!super.mouseClicked(
            mouseX,
            mouseY,
            mouseButton,
            pageX,
            pageY,
            pageWidth,
            pageHeight
        )) {
            return false;
        }

        Recipe<?> recipe = getLastClickedRecipe();
        if (recipe instanceof CraftingRecipe craftingRecipe) {
            gui.recipeSetter.accept(craftingRecipe);
        }
        return true;
    }
}
