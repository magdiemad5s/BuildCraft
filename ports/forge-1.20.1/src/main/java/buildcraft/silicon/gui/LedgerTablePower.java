/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gui;

import java.util.function.Supplier;

import buildcraft.api.core.render.ISprite;
import buildcraft.lib.BCLibSprites;
import buildcraft.lib.gui.BuildCraftGui;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.config.GuiConfigManager;
import buildcraft.lib.gui.ledger.Ledger_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.silicon.tile.TileLaserTableBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LedgerTablePower extends Ledger_Neptune {
    private static final int OVERLAY_COLOUR = 0xFF_D4_6C_1F;
    private static final int SUB_HEADER_COLOUR = 0xFF_AA_AF_B8;
    private static final int TEXT_COLOUR = 0xFF_00_00_00;

    private final Supplier<? extends TileLaserTableBase> tileSupplier;

    public LedgerTablePower(BuildCraftGui gui, TileLaserTableBase tile, boolean expandPositive) {
        this(gui, () -> tile, expandPositive);
    }

    public LedgerTablePower(
        BuildCraftGui gui,
        Supplier<? extends TileLaserTableBase> tileSupplier,
        boolean expandPositive
    ) {
        super(gui, OVERLAY_COLOUR, expandPositive);
        this.tileSupplier = tileSupplier;
        title = Component.translatable("gui.power");

        appendText(Component.literal(LocaleUtil.localize("gui.assemblyCurrentRequired") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileLaserTableBase tile = getTile();
            return LocaleUtil.localizeMj(tile == null ? 0 : tile.getGuiTarget());
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.stored") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileLaserTableBase tile = getTile();
            return LocaleUtil.localizeMj(tile == null ? 0 : tile.power);
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.assemblyRate") + ":"), SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> {
            TileLaserTableBase tile = getTile();
            return LocaleUtil.localizeMjFlow(tile == null ? 0 : tile.avgPowerClient);
        }, TEXT_COLOUR);
        calculateMaxSize();

        setOpenProperty(GuiConfigManager.getOrAddBoolean(new ResourceLocation("buildcraftsilicon:all_tables"), "ledger.power.is_open", false));
    }

    private TileLaserTableBase getTile() {
        return tileSupplier.get();
    }

    @Override
    protected void drawIcon(GuiGraphics guiGraphics, double x, double y) {
        TileLaserTableBase tile = getTile();
        ISprite sprite = tile != null && tile.avgPowerClient > 0
            ? BCLibSprites.ENGINE_ACTIVE : BCLibSprites.ENGINE_INACTIVE;
        GuiIcon.draw(guiGraphics, sprite, x, y, x + 16, y + 16);
    }
}
