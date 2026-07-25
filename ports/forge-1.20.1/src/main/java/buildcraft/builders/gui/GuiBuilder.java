/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;

import buildcraft.builders.menu.ContainerBuilder;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.pos.GuiRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import buildcraft.lib.gui.help.GuiHelpUtil;

// TODO: Convert this gui into JSON!
public class GuiBuilder extends GuiBC8<ContainerBuilder> {
    private static final ResourceLocation TEXTURE_BASE =
            new ResourceLocation("buildcraftbuilders:textures/gui/builder.png");
    private static final ResourceLocation TEXTURE_BLUEPRINT =
            new ResourceLocation("buildcraftbuilders:textures/gui/builder_blueprint.png");
    private static final int SIZE_X = 176, SIZE_BLUEPRINT_X = 256, SIZE_Y = 222, BLUEPRINT_WIDTH = 87;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_BLUEPRINT_GUI = new GuiIcon(
            TEXTURE_BLUEPRINT,
            SIZE_BLUEPRINT_X - BLUEPRINT_WIDTH,
            0,
            BLUEPRINT_WIDTH,
            SIZE_Y
    );
    private static final GuiIcon ICON_TANK_OVERLAY = new GuiIcon(TEXTURE_BLUEPRINT, 0, 54, 16, 47);
    private static final int EXCAVATE_BUTTON_X = 38;
    private static final int EXCAVATE_BUTTON_Y = 50;
    private static final int EXCAVATE_BUTTON_WIDTH = 100;
    private static final int EXCAVATE_BUTTON_HEIGHT = 20;

    private CycleButton<Boolean> canExcavateButton;
    private boolean canExcavate = true;

    public GuiBuilder(ContainerBuilder container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = SIZE_BLUEPRINT_X;
        imageHeight = SIZE_Y;
        GuiHelpUtil.addSlot(mainGui, 80, 27, "buildcraft.help.builder.blueprint.title", 0xFF_66_AA_FF, "buildcraft.help.builder.blueprint.desc");
        GuiHelpUtil.addSlots(mainGui, 8, 72, 9, 3, "buildcraft.help.builder.resources.title", 0xFF_88_CC_88, "buildcraft.help.builder.resources.desc");
        GuiHelpUtil.addSlots(mainGui, 179, 18, 4, 6, "buildcraft.help.builder.required.title", 0xFF_DD_CC_55, "buildcraft.help.builder.required.desc");
        GuiHelpUtil.addRoot(mainGui, 179, 145, 72, 47, "buildcraft.help.builder.fluids.title", 0xFF_55_BB_DD, "buildcraft.help.builder.fluids.desc");
        GuiHelpUtil.addRoot(mainGui, 176, 0, 80, 222, "buildcraft.help.builder.preview.title", 0xFF_CC_AA_FF, "buildcraft.help.builder.preview.desc");
        GuiHelpUtil.addRoot(mainGui, EXCAVATE_BUTTON_X, EXCAVATE_BUTTON_Y, EXCAVATE_BUTTON_WIDTH, EXCAVATE_BUTTON_HEIGHT,
            "buildcraft.help.builder.excavate.title", 0xFF_99_CC_FF, "buildcraft.help.builder.excavate.desc");
    }

    @Override
    public void init() {
        super.init();

        for (int i = 0; i < container.widgetTanks.size(); i++) {
            mainGui.shownElements.add(
                container.widgetTanks.get(i).createGuiElement(
                    mainGui,
                    new GuiRectangle(179 + i * 18, 145, 16, 47).offset(mainGui.rootElement),
                    ICON_TANK_OVERLAY
                )
            );
        }

        TileBuilder tile = container.getTile();
        canExcavate = tile != null && tile.canExcavate();
        canExcavateButton = addRenderableWidget(
            CycleButton.onOffBuilder(canExcavate).create(
                leftPos + EXCAVATE_BUTTON_X,
                topPos + EXCAVATE_BUTTON_Y,
                EXCAVATE_BUTTON_WIDTH,
                EXCAVATE_BUTTON_HEIGHT,
                Component.translatable("block.architect.excavate"),
                (button, newValue) -> {
                    canExcavate = newValue;
                    TileBuilder currentTile = container.getTile();
                    if (currentTile != null) {
                        currentTile.sendCanExcavate(newValue);
                    }
                }
            )
        );
    }

    @Override
    public void containerTick() {
        super.containerTick();
        TileBuilder tile = container.getTile();
        if (canExcavateButton == null || tile == null) {
            return;
        }
        boolean serverValue = tile.canExcavate();
        if (serverValue != canExcavate) {
            canExcavate = serverValue;
            canExcavateButton.setValue(serverValue);
        }
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = getActiveGraphics();
    	RenderSystem._setShaderTexture(0, TEXTURE_BASE);
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);
        RenderSystem._setShaderTexture(0, TEXTURE_BLUEPRINT);
        ICON_BLUEPRINT_GUI.drawAt(guiGraphics, mainGui.rootElement.offset(SIZE_BLUEPRINT_X - BLUEPRINT_WIDTH, 0));
    }
}
