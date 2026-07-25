/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui.ledger;

import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;


import buildcraft.api.core.render.ISprite;
import buildcraft.lib.BCLibSprites;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.gui.BuildCraftGui;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.config.GuiConfigManager;
import buildcraft.lib.misc.LocaleUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LedgerEngine extends Ledger_Neptune {
    private static final int OVERLAY_COLOUR = 0xFF_D4_6C_1F;
    private static final int HEADER_COLOUR = 0xFF_E1_C9_2F;
    private static final int SUB_HEADER_COLOUR = 0xFF_AA_AF_B8;
    private static final int TEXT_COLOUR = 0xFF_00_00_00;

    private final Supplier<? extends TileEngineBase_BC8> engineSupplier;

    public LedgerEngine(BuildCraftGui gui, TileEngineBase_BC8 engine, boolean expandPositive) {
        this(gui, () -> engine, expandPositive);
    }

    public LedgerEngine(BuildCraftGui gui, Supplier<? extends TileEngineBase_BC8> engineSupplier, boolean expandPositive) {
        super(gui, OVERLAY_COLOUR, expandPositive);
        this.engineSupplier = engineSupplier;
        this.title = Component.translatable("gui.power");

        appendText(Component.literal(LocaleUtil.localize("gui.currentOutput") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileEngineBase_BC8 engine = getEngine();
            return LocaleUtil.localizeMjFlow(engine == null ? 0 : engine.currentOutput);
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.stored") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileEngineBase_BC8 engine = getEngine();
            return LocaleUtil.localizeMj(engine == null ? 0 : engine.getEnergyStored());
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.heat") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileEngineBase_BC8 engine = getEngine();
            return LocaleUtil.localizeHeat(engine == null ? 0 : engine.getHeat());
        }, TEXT_COLOUR);
        calculateMaxSize();

        setOpenProperty(GuiConfigManager.getOrAddBoolean(new ResourceLocation("buildcraftlib:engine"),
            "ledger.power.is_open", false));
    }

    private TileEngineBase_BC8 getEngine() {
        return engineSupplier.get();
    }

    @Override
    public int getTitleColour() {
        return HEADER_COLOUR;
    }

    @Override
    protected void drawIcon(GuiGraphics guiGraphics, double x, double y) {
        TileEngineBase_BC8 engine = getEngine();
        ISprite sprite = engine == null ? BCLibSprites.ENGINE_INACTIVE : switch (engine.getPowerStage()) {
            case OVERHEAT -> BCLibSprites.ENGINE_OVERHEAT;
            case RED, YELLOW -> BCLibSprites.ENGINE_WARM;
            default -> engine.isEngineOn() ? BCLibSprites.ENGINE_ACTIVE : BCLibSprites.ENGINE_INACTIVE;
        };
        GuiIcon.draw(guiGraphics, sprite, x, y, x + 16, y + 16);
    }
}
