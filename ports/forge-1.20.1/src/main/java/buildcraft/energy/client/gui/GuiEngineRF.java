/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.menu.ContainerEngineRF;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiElementSimple;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.elem.ToolTip;
import buildcraft.lib.gui.help.DummyHelpElement;
import buildcraft.lib.gui.help.ElementHelpInfo;
import buildcraft.lib.gui.help.ElementHelpInfo.HelpPosition;
import buildcraft.lib.gui.ledger.LedgerEngine;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.misc.LocaleUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Restored RF-engine screen using the original 1.12.2 GUI sheet. */
public class GuiEngineRF extends GuiBC8<ContainerEngineRF> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("buildcraftenergy", "textures/gui/rf_engine_gui.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 177;
    private static final GuiIcon GUI = new GuiIcon(TEXTURE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ENERGY = new GuiIcon(TEXTURE, SIZE_X, 0, 16, 60);
    private static final GuiIcon OVERLAY = new GuiIcon(TEXTURE, 57, 18, 80, 23);
    private static final GuiRectangle UPGRADES = new GuiRectangle(60, 42, 74, 20);
    private static final GuiRectangle UPGRADE_TYPES = new GuiRectangle(60, 20, 74, 20);
    private static final GuiRectangle FE_BATTERY = new GuiRectangle(30, 17, 8, 62);

    public GuiEngineRF(ContainerEngineRF container, Inventory inventory, Component title) {
        super(container, inventory, title.getString().isEmpty()
            ? Component.translatable("tile.engineRf.name").withStyle(Style.EMPTY.withColor(0x404040))
            : title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        inventoryLabelX = 8;
        inventoryLabelY = SIZE_Y - 96;
        mainGui.shownElements.add(
            new DummyHelpElement(
                UPGRADES.offset(mainGui.rootElement),
                new ElementHelpInfo(
                    "buildcraft.help.rf_engine.upgrades.title",
                    0xFF_FF_FF_FF,
                    "buildcraft.help.rf_engine.upgrades.desc"
                )
            )
        );
        mainGui.shownElements.add(new GuiElementSimple(mainGui, UPGRADE_TYPES.offset(mainGui.rootElement)) {
            @Override
            public void addToolTips(List<ToolTip> tooltips) {
                if (!contains(mainGui.mouse)) {
                    return;
                }
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("buildcraft.gui.rf_engine.upgrade_types"));
                addUpgradeLine(lines, new ItemStack(BCCoreItems.GEAR_IRON.get()), 2 * MjAPI.MJ);
                addUpgradeLine(lines, new ItemStack(BCCoreItems.GEAR_GOLD.get()), 3 * MjAPI.MJ);
                tooltips.add(new ToolTip(lines));
            }
        });
        mainGui.shownElements.add(new LedgerEngine(mainGui, container::getTile, true));
        mainGui.shownElements.add(new GuiElementSimple(mainGui, FE_BATTERY.offset(mainGui.rootElement)) {
                @Override
                public void addHelpElements(List<HelpPosition> elements) {
                    TileEngineRF tile = container.getTile();
                    String fe = (tile == null ? 0 : tile.getForgeEnergyConsumptionRate()) + " FE/t";
                    String mj = MjAPI.formatMj(tile == null ? 0 : tile.getMjPerTick()) + " MJ/t";
                    String conversion = LocaleUtil.localize(
                        "buildcraft.help.rf_engine.rf_battery.desc", fe, mj
                    );
                    ElementHelpInfo help = ElementHelpInfo.preTranslated(
                        LocaleUtil.localize("buildcraft.help.rf_engine.rf_battery.title"),
                        0xFF_FF_FF_FF,
                        conversion
                    );
                    elements.add(help.target(this));
                }

                @Override
                public void addToolTips(List<ToolTip> tooltips) {
                    if (contains(mainGui.mouse)) {
                        TileEngineRF tile = container.getTile();
                        tooltips.add(new ToolTip(Component.literal(
                            (tile == null ? 0 : tile.getCurrentForgeEnergy()) + " / "
                                + TileEngineRF.MAX_FORGE_ENERGY + " FE"
                        )));
                    }
                }
            });
    }

    private static void addUpgradeLine(List<Component> lines, ItemStack stack, long microMjPerTick) {
        lines.add(
            stack.getHoverName().copy()
                .append(Component.literal(" = +"))
                .append(LocaleUtil.localizeMjFlow(microMjPerTick))
        );
    }

    @Override
    public void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        GUI.drawAt(getActiveGraphics(), mainGui.rootElement);
        TileEngineRF tile = container.getTile();
        if (tile == null) {
            return;
        }

        double fraction = Mth.clamp(
            tile.getCurrentForgeEnergy() / (double) TileEngineRF.MAX_FORGE_ENERGY,
            0.0,
            1.0
        );
        int height = (int) Math.round(60 * fraction);
        if (height > 0) {
            ENERGY.drawCutInside(
                getActiveGraphics(),
                new GuiRectangle(31, 18 + 60 - height, 6, height).offset(mainGui.rootElement)
            );
        }

        int x = (int) mainGui.rootElement.getX();
        int y = (int) mainGui.rootElement.getY();
        getActiveGraphics().renderItem(new ItemStack(BCCoreItems.GEAR_IRON.get()), x + 78, y + 22);
        getActiveGraphics().renderItem(new ItemStack(BCCoreItems.GEAR_GOLD.get()), x + 101, y + 22);

        getActiveGraphics().setColor(1.0F, 1.0F, 1.0F, 0.65F);
        OVERLAY.drawAt(getActiveGraphics(), mainGui.rootElement.offset(57, 18));
        getActiveGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

}
