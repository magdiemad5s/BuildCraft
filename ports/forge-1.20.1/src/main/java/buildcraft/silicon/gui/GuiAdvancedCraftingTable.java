/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.silicon.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import buildcraft.api.core.BCLog;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.help.GuiHelpUtil;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.recipe.GuiRecipeBookPhantom;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;

public class GuiAdvancedCraftingTable extends GuiBC8<ContainerAdvancedCraftingTable>
    implements RecipeUpdateListener {
    private static final ResourceLocation TEXTURE_BASE =
        new ResourceLocation("buildcraftsilicon:textures/gui/advanced_crafting_table.png");
    private static final ResourceLocation VANILLA_CRAFTING_TABLE =
        new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 241;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 4, 70);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(164, 7, 4, 70);

    private final GuiRecipeBookPhantom recipeBook;
    /** If true, the recipe book is drawn over this GUI instead of beside it. */
    private boolean widthTooNarrow;
    private ImageButton recipeButton;
    private boolean recipeFilterCaptured;
    private boolean recipeBookInitialized;
    private boolean wasFilteringCraftable;

    public GuiAdvancedCraftingTable(
        ContainerAdvancedCraftingTable container,
        Inventory inventory,
        Component title
    ) {
        super(container, inventory, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;

        GuiRecipeBookPhantom book;
        try {
            book = new GuiRecipeBookPhantom(this::sendRecipe);
        } catch (ReflectiveOperationException exception) {
            BCLog.logger.warn("[silicon.gui] Unable to create the Advanced Crafting Table recipe book", exception);
            book = null;
        }
        recipeBook = book;

        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
        mainGui.shownElements.add(new LedgerTablePower(mainGui, container::getTile, true));
        GuiHelpUtil.addSlots(
            mainGui,
            33,
            16,
            3,
            3,
            "buildcraft.help.advanced_crafting.recipe.title",
            0xFF_66_AA_FF,
            "buildcraft.help.advanced_crafting.recipe.desc"
        );
        GuiHelpUtil.addSlots(
            mainGui,
            15,
            85,
            5,
            3,
            "buildcraft.help.advanced_crafting.materials.title",
            0xFF_88_CC_88,
            "buildcraft.help.advanced_crafting.materials.desc"
        );
        GuiHelpUtil.addSlots(
            mainGui,
            109,
            85,
            3,
            3,
            "buildcraft.help.advanced_crafting.outputs.title",
            0xFF_DD_CC_55,
            "buildcraft.help.advanced_crafting.outputs.desc"
        );
        GuiHelpUtil.addSlot(
            mainGui,
            127,
            33,
            "buildcraft.help.advanced_crafting.preview.title",
            0xFF_CC_AA_FF,
            "buildcraft.help.advanced_crafting.preview.desc"
        );
        GuiHelpUtil.addRoot(
            mainGui,
            (int) RECT_PROGRESS.x - 2,
            (int) RECT_PROGRESS.y - 1,
            (int) RECT_PROGRESS.width + 4,
            (int) RECT_PROGRESS.height + 2,
            "buildcraft.help.advanced_crafting.power.title",
            0xFF_D4_6C_1F,
            "buildcraft.help.advanced_crafting.power.desc"
        );
    }

    private void sendRecipe(CraftingRecipe recipe) {
        container.sendSelectRecipe(recipe);
    }

    @Override
    protected boolean shouldAddHelpLedger() {
        // The normal left ledger position clashes with the recipe book.
        return false;
    }

    @Override
    public void init() {
        super.init();
        widthTooNarrow = width < SIZE_X + 176;
        recipeBookInitialized = false;
        recipeButton = null;
        tryInitializeRecipeBook();
    }

    private void tryInitializeRecipeBook() {
        if (recipeBook == null || recipeBookInitialized) {
            return;
        }

        TileAdvancedCraftingTable tile = container.getTile();
        if (tile == null) {
            return;
        }

        disableCraftableFilterForPhantomGrid();
        recipeBook.init(
            width,
            height,
            minecraft,
            widthTooNarrow,
            tile.getWorkbenchCrafting().getCraftingMenu(menu)
        );
        leftPos = recipeBook.updateScreenPosition(width, imageWidth);
        recipeButton = new ImageButton(
            leftPos + 5, height / 2 - 90, 20, 18, 0, 168, 19,
            VANILLA_CRAFTING_TABLE, this::onPress
        );
        addRenderableWidget(recipeButton);
        recipeBookInitialized = true;
    }

    private boolean isRecipeBookAvailable() {
        return recipeBook != null && recipeBookInitialized;
    }

    private void disableCraftableFilterForPhantomGrid() {
        if (minecraft.player == null) {
            return;
        }
        if (!recipeFilterCaptured) {
            wasFilteringCraftable = minecraft.player.getRecipeBook().isFiltering(RecipeBookType.CRAFTING);
            recipeFilterCaptured = true;
        }
        // Phantom blueprints can select any known, fitting recipe even when
        // the player's own inventory does not contain its ingredients.
        minecraft.player.getRecipeBook().setFiltering(RecipeBookType.CRAFTING, false);
    }

    private void restoreCraftableFilter() {
        if (recipeFilterCaptured && minecraft.player != null) {
            minecraft.player.getRecipeBook().setFiltering(RecipeBookType.CRAFTING, wasFilteringCraftable);
        }
        recipeFilterCaptured = false;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        tryInitializeRecipeBook();
        if (isRecipeBookAvailable()) {
            recipeBook.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isRecipeBookAvailable()) {
            super.render(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        if (recipeBook.isVisible() && widthTooNarrow) {
            renderBackground(graphics);
            recipeBook.render(graphics, mouseX, mouseY, partialTicks);
            renderTooltip(graphics, mouseX, mouseY);
        } else {
            super.render(graphics, mouseX, mouseY, partialTicks);
            recipeBook.render(graphics, mouseX, mouseY, partialTicks);
            recipeBook.renderGhostRecipe(graphics, leftPos, topPos, true, partialTicks);
        }
        recipeBook.renderTooltip(graphics, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        ICON_GUI.drawAt(getActiveGraphics(), mainGui.rootElement);

        TileAdvancedCraftingTable tile = container.getTile();
        long target = tile == null ? 0 : tile.getGuiTarget();
        if (target != 0) {
            double progress = (double) tile.power / target;
            ICON_PROGRESS.drawCutInside(
                getActiveGraphics(),
                new GuiRectangle(
                    RECT_PROGRESS.x,
                    (int) (RECT_PROGRESS.y + RECT_PROGRESS.height * Math.max(1 - progress, 0)),
                    RECT_PROGRESS.width,
                    (int) Math.ceil(RECT_PROGRESS.height * Math.min(progress, 1))
                ).offset(mainGui.rootElement)
            );
        }
    }

    @Override
    protected void drawForegroundLayer(PoseStack pose, int mouseX, int mouseY) {
        GuiGraphics graphics = getActiveGraphics();
        int rootX = (int) mainGui.rootElement.getX();
        int rootY = (int) mainGui.rootElement.getY();
        graphics.drawString(
            font,
            title,
            rootX + (imageWidth - font.width(title)) / 2,
            rootY + 5,
            0x404040,
            false
        );
        graphics.drawString(
            font,
            playerInventoryTitle,
            rootX + 8,
            rootY + imageHeight - 94,
            0x404040,
            false
        );
    }

    private void onPress(Button button) {
        if (button == recipeButton && isRecipeBookAvailable()) {
            recipeBook.toggleVisibility();
            leftPos = recipeBook.updateScreenPosition(width, imageWidth);
            recipeButton.setPosition(leftPos + 5, height / 2 - 90);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!isRecipeBookAvailable()) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (recipeBook.mouseClicked(mouseX, mouseY, mouseButton)) {
            setFocused(recipeBook);
            return true;
        }
        if (!widthTooNarrow || !recipeBook.isVisible()) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return isRecipeBookAvailable() && recipeBook.keyPressed(keyCode, scanCode, modifiers)
            || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return isRecipeBookAvailable() && recipeBook.keyReleased(keyCode, scanCode, modifiers)
            || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return isRecipeBookAvailable() && recipeBook.charTyped(character, modifiers)
            || super.charTyped(character, modifiers);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
        if (isRecipeBookAvailable()) {
            recipeBook.slotClicked(slot);
        }
    }

    @Override
    protected boolean isHovering(
        int rectX,
        int rectY,
        int rectWidth,
        int rectHeight,
        double pointX,
        double pointY
    ) {
        return !isRecipeBookAvailable()
            ? super.isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY)
            : (!widthTooNarrow || !recipeBook.isVisible())
                && super.isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    protected boolean hasClickedOutside(
        double mouseX,
        double mouseY,
        int guiLeft,
        int guiTop,
        int mouseButton
    ) {
        if (!isRecipeBookAvailable()) {
            return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
        }
        boolean outsideMain = mouseX < guiLeft || mouseY < guiTop
            || mouseX >= guiLeft + imageWidth || mouseY >= guiTop + imageHeight;
        return recipeBook.hasClickedOutside(
            mouseX,
            mouseY,
            leftPos,
            topPos,
            imageWidth,
            imageHeight,
            mouseButton
        ) && outsideMain;
    }

    @Override
    public void removed() {
        restoreCraftableFilter();
        super.removed();
    }

    @Override
    public void recipesUpdated() {
        if (isRecipeBookAvailable()) {
            recipeBook.recipesUpdated();
        }
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return recipeBook;
    }
}
