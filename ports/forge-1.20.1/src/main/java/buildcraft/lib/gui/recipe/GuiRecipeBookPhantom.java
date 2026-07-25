/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.lib.gui.recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.tile.item.ItemHandlerManager;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * A recipe book that selects recipes into phantom slots rather than asking
 * vanilla to move real player-inventory items.
 *
 * <p>Vanilla's {@link RecipeBookComponent#mouseClicked(double, double, int)}
 * always sends a placement packet for the player's real open menu. BuildCraft
 * menus are not {@link RecipeBookMenu} subclasses, so recipe-page clicks are
 * intercepted here and forwarded to the BuildCraft menu's authoritative recipe
 * request instead.</p>
 */
public class GuiRecipeBookPhantom extends RecipeBookComponent {
    public final Consumer<CraftingRecipe> recipeSetter;

    private final RecipeBookPagePhantom phantomPage;
    private final Field stackedContentsField;
    private int componentWidth;
    private int componentHeight;
    private boolean componentWidthTooNarrow;

    public GuiRecipeBookPhantom(Consumer<CraftingRecipe> recipeSetter) throws ReflectiveOperationException {
        this.recipeSetter = recipeSetter;
        phantomPage = new RecipeBookPagePhantom(this);
        replaceUniqueFieldByType(RecipeBookPage.class, phantomPage);
        stackedContentsField = findUniqueFieldByType(StackedContents.class);
    }

    private static Field findUniqueFieldByType(Class<?> type) throws NoSuchFieldException {
        Field match = null;
        for (Field field : RecipeBookComponent.class.getDeclaredFields()) {
            if (!type.isAssignableFrom(field.getType())) {
                continue;
            }
            if (match != null) {
                throw new NoSuchFieldException(
                    "Multiple RecipeBookComponent fields have type " + type.getName()
                );
            }
            field.setAccessible(true);
            match = field;
        }
        if (match == null) {
            throw new NoSuchFieldException("No RecipeBookComponent field has type " + type.getName());
        }
        return match;
    }

    private void replaceUniqueFieldByType(Class<?> type, Object value) throws ReflectiveOperationException {
        Field field = findUniqueFieldByType(type);
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalAccessException("Refusing to replace static RecipeBookComponent field " + field);
        }
        field.set(this, value);
    }

    @Override
    public void init(
        int width,
        int height,
        Minecraft minecraft,
        boolean widthTooNarrow,
        RecipeBookMenu<?> menu
    ) {
        componentWidth = width;
        componentHeight = height;
        componentWidthTooNarrow = widthTooNarrow;
        super.init(width, height, minecraft, widthTooNarrow, menu);
    }

    /**
     * Handles recipe-page and overlay clicks without allowing vanilla to send
     * its incompatible ServerboundPlaceRecipePacket. Other recipe-book controls
     * continue through the vanilla implementation.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isVisible() && minecraft != null && minecraft.player != null && !minecraft.player.isSpectator()) {
            int xOffset = componentWidthTooNarrow ? 0 : 86;
            int pageX = (componentWidth - IMAGE_WIDTH) / 2 - xOffset;
            int pageY = (componentHeight - IMAGE_HEIGHT) / 2;
            if (phantomPage.mouseClicked(
                mouseX,
                mouseY,
                mouseButton,
                pageX,
                pageY,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
            )) {
                ghostRecipe.clear();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Compatibility helper retained for the original BuildCraft screens.
     */
    public void initVisuals(boolean ignored, CraftingContainer craftingContainer) {
        initVisuals();
        try {
            StackedContents stackedContents = (StackedContents) stackedContentsField.get(this);
            craftingContainer.fillStackedContents(stackedContents);
            recipesUpdated();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to update phantom recipe book contents", exception);
        }
    }

    @Override
    public void initVisuals() {
        super.initVisuals();
        // Filtering by craftable inventory items is meaningless for a phantom
        // blueprint, so the containing screen temporarily forces "show all".
        StateSwitchingButton button = filterButton;
        button.setX(-100_000);
        button.setY(-100_000);
    }

    boolean hasInstalledPhantomPage() {
        return phantomPage != null;
    }
}
