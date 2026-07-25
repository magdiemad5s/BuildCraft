/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import buildcraft.api.core.BCLog;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.recipe.GuiRecipeBookPhantom;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.tile.craft.WorkbenchCrafting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;
import buildcraft.lib.gui.help.GuiHelpUtil;

public class GuiAutoCraftItems extends GuiBC8<ContainerAutoCraftItems>
    implements RecipeUpdateListener {
    private static final ResourceLocation TEXTURE_BASE =
        new ResourceLocation("buildcraftfactory:textures/gui/autobench_item.png");
    private static final ResourceLocation TEXTURE_MISC =
        new ResourceLocation("buildcraftlib:textures/gui/misc_slots.png");
    private static final ResourceLocation VANILLA_CRAFTING_TABLE =
        new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 197;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_FILTER_OVERLAY_SAME = new GuiIcon(TEXTURE_MISC, 54, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_DIFFERENT = new GuiIcon(TEXTURE_MISC, 72, 0, 18, 18);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 23, 10);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(90, 47, 23, 10);

    private final GuiRecipeBookPhantom recipeBook;
    /** If true then the recipe book will be drawn on top of this GUI, rather than beside it */
    private boolean widthTooNarrow;
    private ImageButton recipeButton;
    private boolean recipeBookInitialized;
    private boolean recipeFilterCaptured;
    private boolean wasFilteringCraftable;

    public GuiAutoCraftItems(ContainerAutoCraftItems container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        GuiRecipeBookPhantom book;
        try {
            book = new GuiRecipeBookPhantom(this::sendRecipe);
        } catch (ReflectiveOperationException e) {
            BCLog.logger.warn("[factory.gui] An exception was thrown while creating the recipe book gui!", e);
            book = null;
        }
        recipeBook = book;
        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
        GuiHelpUtil.addSlots(mainGui, 30, 17, 3, 3, "buildcraft.help.autoworkbench.recipe.title", 0xFF_66_AA_FF, "buildcraft.help.autoworkbench.recipe.desc");
        GuiHelpUtil.addSlots(mainGui, 8, 84, 9, 1, "buildcraft.help.autoworkbench.materials.title", 0xFF_88_CC_88, "buildcraft.help.autoworkbench.materials.desc");
        GuiHelpUtil.addSlot(mainGui, 124, 35, "buildcraft.help.autoworkbench.result.title", 0xFF_DD_CC_55, "buildcraft.help.autoworkbench.result.desc");
        GuiHelpUtil.addRoot(mainGui, 90, 47, 23, 10, "buildcraft.help.autoworkbench.progress.title", 0xFF_CC_AA_FF, "buildcraft.help.autoworkbench.progress.desc");
    }

    private void sendRecipe(Recipe<?> recipe) {
        List<ItemStack> stacks = new ArrayList<>(9);

        int maxX = recipe instanceof IShapedRecipe ? ((IShapedRecipe<?>) recipe).getRecipeWidth() : 3;
        int maxY = recipe instanceof IShapedRecipe ? ((IShapedRecipe<?>) recipe).getRecipeHeight() : 3;
        int offsetX = maxX == 1 ? 1 : 0;
        int offsetY = maxY == 1 ? 1 : 0;
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x < offsetX || y < offsetY) {
                    stacks.add(ItemStack.EMPTY);
                    continue;
                }
                int i = x - offsetX + (y - offsetY) * maxX;
                if (i >= ingredients.size() || x - offsetX >= maxX) {
                    stacks.add(ItemStack.EMPTY);
                } else {
                    Ingredient ing = ingredients.get(i);
                    ItemStack[] matching = ing.getItems();
                    if (matching.length >= 1) {
                        stacks.add(matching[0]);
                    } else {
                        stacks.add(ItemStack.EMPTY);
                    }
                }
            }
        }

        container.sendSetPhantomSlots(container.blueprintHandler, stacks);
    }

    @Override
    protected boolean shouldAddHelpLedger() {
        // Don't add it on the left side because it clashes with the recipe book
        return false;
    }

    @Override
    public void init() {
        super.init();
        widthTooNarrow = this.width < SIZE_X + 176;
        recipeBookInitialized = false;
        recipeButton = null;
        tryInitializeRecipeBook();
    }

    private void tryInitializeRecipeBook() {
        if (recipeBook == null || recipeBookInitialized) {
            return;
        }

        TileAutoWorkbenchItems tile = container.getTile();
        if (tile == null) {
            return;
        }

        disableCraftableFilterForPhantomGrid();
        WorkbenchCrafting invCraft = tile.getWorkbenchCrafting();
        recipeBook.init(width, height, minecraft, widthTooNarrow, invCraft.getCraftingMenu(menu));
        leftPos = recipeBook.updateScreenPosition(width, imageWidth);
        recipeButton =
            new ImageButton(leftPos + 5, height / 2 - 66, 20, 18, 0, 168, 19, VANILLA_CRAFTING_TABLE, this::onPress);
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!isRecipeBookAvailable()) {
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            return;
        }
        if (recipeBook.isVisible() && this.widthTooNarrow) {
            renderBackground(guiGraphics);
            this.drawBackgroundLayer(guiGraphics.pose(), mouseX, mouseY, partialTicks);
            recipeBook.render(guiGraphics, mouseX, mouseY, partialTicks);
            renderTooltip(guiGraphics, mouseX, mouseY);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            recipeBook.render(guiGraphics, mouseX, mouseY, partialTicks);
            recipeBook.renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, partialTicks);
        }

        recipeBook.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Override
    protected void drawBackgroundLayer(com.mojang.blaze3d.vertex.PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = getActiveGraphics();
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);

        TileAutoWorkbenchItems tile = container.getTile();
        double progress = tile == null ? 0.0 : tile.getProgress(partialTicks);

        drawProgress(guiGraphics, RECT_PROGRESS, ICON_PROGRESS, progress, 1);

        if (hasFilters()) {
//            RenderSystem.enableGUIStandardItemLighting();
            forEachFilter((slot, filterStack) -> {
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                guiGraphics.renderItem(filterStack, x, y);
                guiGraphics.renderItemDecorations(font, filterStack, x, y);
            });
//            RenderHelper.disableStandardItemLighting();

            RenderSystem.disableDepthTest();
            forEachFilter((slot, filterStack) -> {
                ItemStack real = slot.getItem();
                final GuiIcon icon;
                if (real.isEmpty() || StackUtil.canMerge(real, filterStack)) {
                    icon = ICON_FILTER_OVERLAY_SAME;
                } else {
                    icon = ICON_FILTER_OVERLAY_DIFFERENT;
                }
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                icon.drawAt(guiGraphics, x - 1, y - 1);
            });
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    protected void drawForegroundLayer(com.mojang.blaze3d.vertex.PoseStack pose, int mouseX, int mouseY) {
        GuiGraphics guiGraphics = getActiveGraphics();
        int rootX = (int) mainGui.rootElement.getX();
        int rootY = (int) mainGui.rootElement.getY();
        guiGraphics.drawString(font, title, rootX + 8, rootY + 6, 0x404040, false);
        guiGraphics.drawString(
            font,
            playerInventoryTitle,
            rootX + 8,
            rootY + imageHeight - 94,
            0x404040,
            false
        );
    }
    private boolean hasFilters() {
        SlotBase[] filters = container.filterSlots;
        for (int s = 0; s < filters.length; s++) {
            ItemStack filter = filters[s].getItem();
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void forEachFilter(IFilterSlotIterator iter) {
    	SlotBase[] filters = container.filterSlots;
        for (int s = 0; s < filters.length; s++) {
            ItemStack filter = filters[s].getItem();
            if (!filter.isEmpty()) {
                iter.iterate(container.materialSlots[s], filter);
            }
        }
    }

    @FunctionalInterface
    private interface IFilterSlotIterator {
        void iterate(SlotBase drawSlot, ItemStack filterStack);
    }

    protected void onPress(Button button){
        if (button == recipeButton && isRecipeBookAvailable()) {
            recipeBook.initVisuals();
            recipeBook.toggleVisibility();
            leftPos = recipeBook.updateScreenPosition(width, imageWidth);
            recipeButton.setPosition(this.leftPos + 5, this.height / 2 - 66);
        }
    }

    @Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton){
        if (!isRecipeBookAvailable()) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (!recipeBook.mouseClicked(mouseX, mouseY, mouseButton)) {
            if (!widthTooNarrow || !recipeBook.isVisible()) {
                return super.mouseClicked(mouseX, mouseY, mouseButton);
            }
            return false;
        }
        return true;
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
	protected boolean isHovering(int rectX, int rectY, int rectWidth, int rectHeight, double pointX, double pointY) {
        if (!isRecipeBookAvailable()) {
            return super.isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
        }
        return (!widthTooNarrow || !recipeBook.isVisible())
            && super.isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
	}

    @Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int _guiLeft, int _guiTop, int p_97761_) {
        if (!isRecipeBookAvailable()) {
            return super.hasClickedOutside(mouseX, mouseY, _guiLeft, _guiTop, p_97761_);
        }
        boolean flag =
            mouseX < _guiLeft || mouseY < _guiTop || mouseX >= _guiLeft + imageWidth || mouseY >= _guiTop + imageHeight;
        return recipeBook.hasClickedOutside(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, p_97761_) && flag;
	}

    @Override
    public void removed() {
        restoreCraftableFilter();
        super.removed();
    }

    // RecipeUpdateListener

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
