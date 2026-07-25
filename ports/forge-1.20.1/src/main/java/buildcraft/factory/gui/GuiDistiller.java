/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/
 */
package buildcraft.factory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager.IDistillationRecipe;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.component.TankComponent;
import buildcraft.lib.gui.help.GuiHelpUtil;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.neo.forge1201.factory.DistillerMenuStatePolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** BuildCraft 8 Distiller screen using the original released GUI sheet. */
public class GuiDistiller extends GuiBC8<ContainerDistiller> {
    private static final ResourceLocation TEXTURE_BASE =
        new ResourceLocation("buildcraftfactory", "textures/gui/distiller.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 161;

    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_TANK_VERTICAL_OVERLAY =
        new GuiIcon(TEXTURE_BASE, 0, 161, 16, 38);
    private static final GuiIcon ICON_TANK_HORIZONTAL_OVERLAY =
        new GuiIcon(TEXTURE_BASE, 17, 161, 34, 17);
    private static final GuiIcon ICON_OFF_VALID_INPUT =
        new GuiIcon(TEXTURE_BASE, 176, 14, 17, 29);
    private static final GuiRectangle RECT_OFF_VALID_INPUT = new GuiRectangle(61, 26, 17, 29);
    private static final GuiIcon ICON_OFF_OUTPUT_GAS =
        new GuiIcon(TEXTURE_BASE, 192, 0, 20, 13);
    private static final GuiRectangle RECT_OFF_OUTPUT_GAS = new GuiRectangle(77, 12, 20, 13);
    private static final GuiIcon ICON_OFF_OUTPUT_LIQUID =
        new GuiIcon(TEXTURE_BASE, 192, 44, 20, 13);
    private static final GuiRectangle RECT_OFF_OUTPUT_LIQUID = new GuiRectangle(77, 56, 20, 13);

    private static final GuiIcon ICON_ACTIVE_BACKGROUND =
        new GuiIcon(TEXTURE_BASE, 176, 57, 36, 57);
    private static final GuiRectangle RECT_ACTIVE_BACKGROUND = new GuiRectangle(61, 12, 36, 57);
    private static final GuiIcon ICON_ACTIVE_INPUT =
        new GuiIcon(TEXTURE_BASE, 212, 26, 26, 5);
    private static final GuiRectangle RECT_ACTIVE_INPUT = new GuiRectangle(61, 38, 26, 5);
    private static final GuiIcon ICON_ACTIVE_GAS_VERTICAL =
        new GuiIcon(TEXTURE_BASE, 225, 13, 13, 16);
    private static final GuiRectangle RECT_ACTIVE_GAS_VERTICAL = new GuiRectangle(74, 25, 13, 16);
    private static final GuiIcon ICON_ACTIVE_LIQUID_VERTICAL =
        new GuiIcon(TEXTURE_BASE, 225, 28, 13, 16);
    private static final GuiRectangle RECT_ACTIVE_LIQUID_VERTICAL = new GuiRectangle(74, 40, 13, 16);
    private static final GuiIcon ICON_ACTIVE_GAS_OUT =
        new GuiIcon(TEXTURE_BASE, 230, 5, 18, 3);
    private static final GuiRectangle RECT_ACTIVE_GAS_OUT = new GuiRectangle(79, 17, 18, 3);
    private static final GuiIcon ICON_ACTIVE_LIQUID_OUT =
        new GuiIcon(TEXTURE_BASE, 230, 49, 18, 3);
    private static final GuiRectangle RECT_ACTIVE_LIQUID_OUT = new GuiRectangle(79, 61, 18, 3);

    private final TankComponent inputTank = new TankComponent(
        44, 23, 16, 38, DistillerMenuStatePolicy.TANK_CAPACITY_MILLIBUCKETS, -1, -1, 2
    );
    private final TankComponent gasTank = new TankComponent(
        98, 10, 34, 17, DistillerMenuStatePolicy.TANK_CAPACITY_MILLIBUCKETS, -1, -1, 2
    );
    private final TankComponent liquidTank = new TankComponent(
        98, 54, 34, 17, DistillerMenuStatePolicy.TANK_CAPACITY_MILLIBUCKETS, -1, -1, 2
    );

    public GuiDistiller(ContainerDistiller container, Inventory inventory, Component title) {
        super(container, inventory, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        inventoryLabelX = 8;
        inventoryLabelY = SIZE_Y - 96;

        inputTank.setDataoffset(DistillerMenuStatePolicy.INPUT_TANK_BUTTON);
        gasTank.setDataoffset(DistillerMenuStatePolicy.GAS_TANK_BUTTON);
        liquidTank.setDataoffset(DistillerMenuStatePolicy.LIQUID_TANK_BUTTON);
        inputTank.setup(this, container.data);
        gasTank.setup(this, container.data);
        liquidTank.setup(this, container.data);

        GuiHelpUtil.addRoot(
            mainGui, 44, 23, 16, 38,
            "buildcraft.help.distiller.tank_in.title", 0xFF_E2_63_63,
            "buildcraft.help.distiller.tank_in.desc"
        );
        GuiHelpUtil.addRoot(
            mainGui, 98, 10, 34, 17,
            "buildcraft.help.distiller.tank_gas_out.title", 0xFF_E4_E4_00,
            "buildcraft.help.distiller.tank_gas_out.desc"
        );
        GuiHelpUtil.addRoot(
            mainGui, 98, 54, 34, 17,
            "buildcraft.help.distiller.tank_liquid_out.title", 0xFF_B5_00_FF,
            "buildcraft.help.distiller.tank_liquid_out.desc"
        );
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = getActiveGraphics();
        ICON_GUI.drawAt(guiGraphics, mainGui.rootElement);

        if (container.isActive()) {
            drawActiveState(guiGraphics);
        } else {
            drawInactiveState(guiGraphics);
        }

        inputTank.render(guiGraphics, mouseX, mouseY, partialTicks, this);
        gasTank.render(guiGraphics, mouseX, mouseY, partialTicks, this);
        liquidTank.render(guiGraphics, mouseX, mouseY, partialTicks, this);

        ICON_TANK_VERTICAL_OVERLAY.drawAt(guiGraphics, leftPos + 44, topPos + 23);
        ICON_TANK_HORIZONTAL_OVERLAY.drawAt(guiGraphics, leftPos + 98, topPos + 10);
        ICON_TANK_HORIZONTAL_OVERLAY.drawAt(guiGraphics, leftPos + 98, topPos + 54);
    }

    private void drawActiveState(GuiGraphics guiGraphics) {
        ICON_ACTIVE_BACKGROUND.drawAt(guiGraphics, RECT_ACTIVE_BACKGROUND.offset(mainGui.rootElement));

        // Preserve the legacy flow indication with a continuously moving three-stage
        // pulse. The original texture regions and fluid tint colours are retained.
        double phase = (System.currentTimeMillis() % 1_200L) / 1_200.0;
        double inputProgress = Mth.clamp(phase * 3.0, 0.0, 1.0);
        double branchProgress = Mth.clamp(phase * 3.0 - 1.0, 0.0, 1.0);
        double outputProgress = Mth.clamp(phase * 3.0 - 2.0, 0.0, 1.0);

        withFluidTint(container.getFluid(0), () ->
            drawProgress(guiGraphics, RECT_ACTIVE_INPUT, ICON_ACTIVE_INPUT, inputProgress, 1.0)
        );
        withFluidTint(container.getFluid(1), () -> {
            drawProgress(guiGraphics, RECT_ACTIVE_GAS_VERTICAL, ICON_ACTIVE_GAS_VERTICAL, 1.0, branchProgress);
            drawProgress(guiGraphics, RECT_ACTIVE_GAS_OUT, ICON_ACTIVE_GAS_OUT, outputProgress, 1.0);
        });
        withFluidTint(container.getFluid(2), () -> {
            drawProgress(
                guiGraphics, RECT_ACTIVE_LIQUID_VERTICAL, ICON_ACTIVE_LIQUID_VERTICAL, 1.0, branchProgress
            );
            drawProgress(guiGraphics, RECT_ACTIVE_LIQUID_OUT, ICON_ACTIVE_LIQUID_OUT, outputProgress, 1.0);
        });
    }

    private void drawInactiveState(GuiGraphics guiGraphics) {
        FluidStack input = container.getFluidStack(0);
        FluidStack gas = container.getFluidStack(1);
        FluidStack liquid = container.getFluidStack(2);
        IDistillationRecipe recipe =
            BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry().getRecipeForInput(input);

        if (recipe != null) {
            ICON_OFF_VALID_INPUT.drawAt(guiGraphics, RECT_OFF_VALID_INPUT.offset(mainGui.rootElement));
        }
        if (isOutputBlocked(gas, container.getCapacity(1), recipe == null ? FluidStack.EMPTY : recipe.outGas())) {
            ICON_OFF_OUTPUT_GAS.drawAt(guiGraphics, RECT_OFF_OUTPUT_GAS.offset(mainGui.rootElement));
        }
        if (isOutputBlocked(
            liquid, container.getCapacity(2), recipe == null ? FluidStack.EMPTY : recipe.outLiquid()
        )) {
            ICON_OFF_OUTPUT_LIQUID.drawAt(guiGraphics, RECT_OFF_OUTPUT_LIQUID.offset(mainGui.rootElement));
        }
    }

    private static boolean isOutputBlocked(FluidStack present, int capacity, FluidStack expected) {
        if (present.isEmpty()) {
            return false;
        }
        if (present.getAmount() >= capacity) {
            return true;
        }
        return !expected.isEmpty() && !present.isFluidEqual(expected);
    }

    private static void withFluidTint(Fluid fluid, Runnable draw) {
        int tint = fluid == null || fluid == Fluids.EMPTY
            ? 0xFFFFFFFF
            : IClientFluidTypeExtensions.of(fluid).getTintColor();
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        RenderSystem.setShaderColor(
            ((tint >>> 16) & 0xFF) / 255.0F,
            ((tint >>> 8) & 0xFF) / 255.0F,
            (tint & 0xFF) / 255.0F,
            alpha
        );
        try {
            draw.run();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    protected void drawForegroundLayer(PoseStack pose, int mouseX, int mouseY) {
        GuiGraphics guiGraphics = getActiveGraphics();
        int rootX = (int) mainGui.rootElement.getX();
        int rootY = (int) mainGui.rootElement.getY();
        guiGraphics.drawString(
            font, title, rootX + (imageWidth - font.width(title)) / 2, rootY + 6, 0x404040, false
        );
        guiGraphics.drawString(
            font, playerInventoryTitle, rootX + inventoryLabelX, rootY + inventoryLabelY, 0x404040, false
        );
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        inputTank.renderTooltip(guiGraphics, mouseX, mouseY);
        gasTank.renderTooltip(guiGraphics, mouseX, mouseY);
        liquidTank.renderTooltip(guiGraphics, mouseX, mouseY);
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputTank.onClick(mouseX, mouseY, button)
            || gasTank.onClick(mouseX, mouseY, button)
            || liquidTank.onClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        inputTank.mouseRelease(mouseX, mouseY, button);
        gasTank.mouseRelease(mouseX, mouseY, button);
        liquidTank.mouseRelease(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        inputTank.onClose();
        gasTank.onClose();
        liquidTank.onClose();
        super.onClose();
    }
}
