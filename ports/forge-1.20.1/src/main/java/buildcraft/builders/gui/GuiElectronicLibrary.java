/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.gui;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;

import buildcraft.api.core.render.ISprite;
import buildcraft.builders.menu.ContainerElectronicLibrary;
import buildcraft.builders.snapshot.GlobalSavedDataSnapshots;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Snapshot.Header;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.button.GuiButtonDrawable;
import buildcraft.lib.gui.button.IButtonClickEventTrigger;
import buildcraft.lib.gui.button.StandardSpriteButtons;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import buildcraft.lib.gui.help.GuiHelpUtil;
import buildcraft.lib.misc.GuiUtil;

import net.minecraftforge.fml.LogicalSide;
public class GuiElectronicLibrary extends GuiBC8<ContainerElectronicLibrary> {
    private static final ResourceLocation TEXTURE_BASE =
        new ResourceLocation("buildcraftbuilders:textures/gui/electronic_library.png");
    private static final int SIZE_X = 244, SIZE_Y = 220;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS_DOWN = new GuiIcon(TEXTURE_BASE, 234, 240, 22, 16);
    private static final GuiRectangle RECT_PROGRESS_DOWN = new GuiRectangle(194, 58, 22, 16);
    private static final GuiIcon ICON_PROGRESS_UP = new GuiIcon(TEXTURE_BASE, 234, 224, 22, 16);
    private static final GuiRectangle RECT_PROGRESS_UP = new GuiRectangle(194, 79, 22, 16);
    private static final GuiRectangle RECT_SNAPSHOT_LIST = new GuiRectangle(8, 22, 154, 108);
    private static final int SNAPSHOT_ROW_HEIGHT = 8;
    private static final int MAX_VISIBLE_SNAPSHOTS = RECT_SNAPSHOT_LIST.height < SNAPSHOT_ROW_HEIGHT ? 0
        : (int) (RECT_SNAPSHOT_LIST.height / SNAPSHOT_ROW_HEIGHT);
    private static final GuiRectangle RECT_DELETE_BUTTON = new GuiRectangle(174, 109, 25, 15);

    private final GuiButtonDrawable delButton;
    private int scrollOffset;

    public GuiElectronicLibrary(ContainerElectronicLibrary container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        IGuiPosition buttonPos = mainGui.rootElement.offset(174, 109);
        delButton = new GuiButtonDrawable(mainGui, "del", buttonPos, StandardSpriteButtons.EIGHTH_BUTTON_DRAWABLE);
        delButton.enabled = false;
        delButton.registerListener(this::onDelButtonClick);
        mainGui.shownElements.add(delButton);
        mainGui.shownElements.add(delButton.createTextElement(Component.translatable("gui.del")));
        GuiHelpUtil.addRoot(mainGui, (int) RECT_SNAPSHOT_LIST.x, (int) RECT_SNAPSHOT_LIST.y, (int) RECT_SNAPSHOT_LIST.width,
            (int) RECT_SNAPSHOT_LIST.height, "buildcraft.help.library.list.title", 0xFF_66_AA_FF,
            "buildcraft.help.library.list.desc");
        GuiHelpUtil.addSlot(mainGui, 175, 57, "buildcraft.help.library.download_output.title", 0xFF_88_CC_88, "buildcraft.help.library.download_output.desc");
        GuiHelpUtil.addSlot(mainGui, 219, 57, "buildcraft.help.library.download_input.title", 0xFF_DD_CC_55, "buildcraft.help.library.download_input.desc");
        GuiHelpUtil.addSlot(mainGui, 175, 79, "buildcraft.help.library.upload_input.title", 0xFF_CC_AA_FF, "buildcraft.help.library.upload_input.desc");
        GuiHelpUtil.addSlot(mainGui, 219, 79, "buildcraft.help.library.upload_output.title", 0xFF_55_BB_DD, "buildcraft.help.library.upload_output.desc");
        GuiHelpUtil.addRoot(mainGui, 194, 58, 22, 37, "buildcraft.help.library.progress.title", 0xFF_EE_DD_77, "buildcraft.help.library.progress.desc");
        GuiHelpUtil.addRoot(mainGui, (int) RECT_DELETE_BUTTON.x, (int) RECT_DELETE_BUTTON.y, (int) RECT_DELETE_BUTTON.width,
            (int) RECT_DELETE_BUTTON.height, "buildcraft.help.library.delete.title", 0xFF_CC_55_55,
            "buildcraft.help.library.delete.desc");
    }

    private void onDelButtonClick(IButtonClickEventTrigger button, int buttonKey) {
        Snapshot.Key selected = getSelected();
        if (selected != null) {
            Snapshot snapshot = getSnapshots().getSnapshot(selected);
            if (snapshot != null) {
                container.sendSelectedToServer(null);
                getSnapshots().removeSnapshot(snapshot.key);
            }
        }
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = getActiveGraphics();
    	RenderSystem._setShaderTexture(0, TEXTURE_BASE);
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);
        TileElectronicLibrary tile = container.getTile();
        drawHorizontalProgress(guiGraphics, RECT_PROGRESS_DOWN, ICON_PROGRESS_DOWN,
            tile == null ? 0 : tile.getDownloadProgress(partialTicks), true);
        drawHorizontalProgress(guiGraphics, RECT_PROGRESS_UP, ICON_PROGRESS_UP,
            tile == null ? 0 : tile.getUploadProgress(partialTicks), false);
        iterateSnapshots((i, rect, key) -> {
            boolean isSelected = key.equals(getSelected());
            if (isSelected) {
                drawGradientRect(guiGraphics, rect, 0xFF_55_55_55, 0xFF_55_55_55);
            }
            int colour = isSelected ? 0xffffa0 : 0xe0e0e0;
            Header header = key.header;
            String text = header == null ? key.toString() : header.name;
            text = font.plainSubstrByWidth(text, Math.max(0, (int) rect.width - 2));
            drawString(guiGraphics, font, text, rect.x + 1, rect.y, colour);
        });
        delButton.enabled = getSnapshots().getSnapshot(getSelected()) != null;
    }


    private void drawHorizontalProgress(GuiGraphics guiGraphics, GuiRectangle rect, GuiIcon icon, double progress, boolean rightToLeft) {
        if (progress <= 0) {
            return;
        }
        progress = Math.min(1, progress);
        double width = rect.width * progress;
        double x = mainGui.rootElement.getX() + rect.x;
        if (rightToLeft) {
            x += rect.width - width;
        }
        double y = mainGui.rootElement.getY() + rect.y;
        double u = rightToLeft ? 1 - progress : 0;
        ISprite sprite = GuiUtil.subRelative(icon.sprite, u, 0, progress, 1);
        GuiIcon.draw(guiGraphics, sprite, x, y, x + width, y + rect.height);
    }

    private GlobalSavedDataSnapshots getSnapshots() {
        return GlobalSavedDataSnapshots.get(LogicalSide.CLIENT);
    }

    private Snapshot.Key getSelected() {
        TileElectronicLibrary tile = container.getTile();
        return tile == null ? null : tile.selected;
    }

    private void iterateSnapshots(ISnapshotIterator iterator) {
        List<Snapshot.Key> list = getSnapshots().getList();
        scrollOffset = clampScrollOffset(scrollOffset);
        GuiRectangle rect = new GuiRectangle(mainGui.rootElement.getX() + RECT_SNAPSHOT_LIST.x,
            mainGui.rootElement.getY() + RECT_SNAPSHOT_LIST.y, RECT_SNAPSHOT_LIST.width, SNAPSHOT_ROW_HEIGHT);
        int max = Math.min(list.size(), scrollOffset + MAX_VISIBLE_SNAPSHOTS);
        for (int index = scrollOffset; index < max; index++) {
            iterator.call(index, rect, list.get(index));
            rect = rect.offset(0, SNAPSHOT_ROW_HEIGHT);
        }
    }

    private int clampScrollOffset(int requested) {
        int max = Math.max(0, getSnapshots().getList().size() - MAX_VISIBLE_SNAPSHOTS);
        return Math.max(0, Math.min(requested, max));
    }

    private boolean isMouseOverSnapshotList(double mouseX, double mouseY) {
        double left = mainGui.rootElement.getX() + RECT_SNAPSHOT_LIST.x;
        double top = mainGui.rootElement.getY() + RECT_SNAPSHOT_LIST.y;
        return mouseX >= left
            && mouseX < left + RECT_SNAPSHOT_LIST.width
            && mouseY >= top
            && mouseY < top + RECT_SNAPSHOT_LIST.height;
    }
    

    @Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton){
        AtomicBoolean found = new AtomicBoolean(false);
        iterateSnapshots((i, rect, key) -> {
            if (rect.contains(mainGui.mouse)) {
                container.sendSelectedToServer(key);
                delButton.enabled = true;
                found.set(true);
            }
        });
        if (!found.get()) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0 || !isMouseOverSnapshotList(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int previous = scrollOffset;
        scrollOffset = clampScrollOffset(scrollOffset + (delta > 0 ? -1 : 1));
        if (scrollOffset != previous) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    @FunctionalInterface
    private interface ISnapshotIterator {
        void call(int snapshotIndex, GuiRectangle rect, Snapshot.Key key);
    }
}
