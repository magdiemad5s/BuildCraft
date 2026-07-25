/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.help.GuiHelpUtil;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiFilteredBuffer extends GuiBC8<ContainerFilteredBuffer_BC8> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcrafttransport:textures/gui/filtered_buffer.png");
    private static final int SIZE_X = 176, SIZE_Y = 169;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);

    public GuiFilteredBuffer(ContainerFilteredBuffer_BC8 container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        GuiHelpUtil.addSlots(mainGui, 8, 27, 9, 1, "buildcraft.help.filtered_buffer.filters.title", 0xFF_66_AA_FF, "buildcraft.help.filtered_buffer.filters.desc");
        GuiHelpUtil.addSlots(mainGui, 8, 61, 9, 1, "buildcraft.help.filtered_buffer.inventory.title", 0xFF_88_CC_88, "buildcraft.help.filtered_buffer.inventory.desc");
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = getActiveGraphics();
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);
        for (int i = 0; i < container.filterSlots.length; i++) {
            ItemStack stack = container.filterSlots[i].getItem();
            int currentX = (int) mainGui.rootElement.getX() + 8 + i * 18;
            int currentY = (int) mainGui.rootElement.getY() + 61;
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, currentX, currentY);
                guiGraphics.renderItemDecorations(font, stack, currentX, currentY);
            } else {
                GuiIcon.drawAt(
                    guiGraphics,
                    BCTransportSprites.NOTHING_FILTERED_BUFFER_SLOT,
                    currentX,
                    currentY,
                    16,
                    16
                );
            }
        }
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1, 1, 1, 0.7f);
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);

        guiGraphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    @Override
    protected void drawForegroundLayer(PoseStack pose, int mouseX, int mouseY) {
        GuiGraphics guiGraphics = getActiveGraphics();
        int x = leftPos;
        int y = topPos;
        Component title = Component.translatable("block.buildcrafttransport.filtered_buffer");
        int xPos = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, x + xPos, y + 10, 0x404040, false);
    }
}
