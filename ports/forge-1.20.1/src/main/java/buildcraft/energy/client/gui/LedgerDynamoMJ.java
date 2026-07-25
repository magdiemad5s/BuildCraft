/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.client.gui;

import java.util.function.Supplier;

import buildcraft.api.core.render.ISprite;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.lib.BCLibSprites;
import buildcraft.lib.gui.BuildCraftGui;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.config.GuiConfigManager;
import buildcraft.lib.gui.ledger.Ledger_Neptune;
import buildcraft.lib.misc.LocaleUtil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Power ledger whose output unit is FE while its input storage remains MJ. */
public class LedgerDynamoMJ extends Ledger_Neptune {
    private static final int OVERLAY_COLOUR = 0xFF_D4_6C_1F;
    private static final int HEADER_COLOUR = 0xFF_E1_C9_2F;
    private static final int SUB_HEADER_COLOUR = 0xFF_AA_AF_B8;
    private static final int TEXT_COLOUR = 0xFF_00_00_00;

    private final Supplier<? extends TileDynamoMJ> dynamoSupplier;

    public LedgerDynamoMJ(BuildCraftGui gui, TileDynamoMJ dynamo, boolean expandPositive) {
        this(gui, () -> dynamo, expandPositive);
    }

    public LedgerDynamoMJ(BuildCraftGui gui, Supplier<? extends TileDynamoMJ> dynamoSupplier, boolean expandPositive) {
        super(gui, OVERLAY_COLOUR, expandPositive);
        this.dynamoSupplier = dynamoSupplier;
        title = Component.translatable("gui.power");

        appendText(Component.literal(LocaleUtil.localize("gui.currentOutput") + ":"), SUB_HEADER_COLOUR)
            .setDropShadow(true);
        appendText(() -> {
            TileDynamoMJ dynamo = getDynamo();
            return Component.literal((dynamo == null ? 0 : dynamo.getCurrentOutput()) + " FE/t");
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.stored") + ":"), SUB_HEADER_COLOUR)
            .setDropShadow(true);
        appendText(() -> {
            TileDynamoMJ dynamo = getDynamo();
            return LocaleUtil.localizeMj(dynamo == null ? 0 : dynamo.getMjStored());
        }, TEXT_COLOUR);
        appendText(Component.literal(LocaleUtil.localize("gui.heat") + ":"), SUB_HEADER_COLOUR)
            .setDropShadow(true);
        appendText(() -> {
            TileDynamoMJ dynamo = getDynamo();
            return LocaleUtil.localizeHeat(dynamo == null ? 0 : dynamo.getHeat());
        }, TEXT_COLOUR);
        calculateMaxSize();

        setOpenProperty(GuiConfigManager.getOrAddBoolean(
            new ResourceLocation("buildcraftlib", "engine"),
            "ledger.power.is_open",
            false
        ));
    }

    private TileDynamoMJ getDynamo() {
        return dynamoSupplier.get();
    }

    @Override
    public int getTitleColour() {
        return HEADER_COLOUR;
    }

    @Override
    protected void drawIcon(GuiGraphics graphics, double x, double y) {
        TileDynamoMJ dynamo = getDynamo();
        ISprite sprite = dynamo == null ? BCLibSprites.ENGINE_INACTIVE : switch (dynamo.getPowerStage()) {
            case OVERHEAT -> BCLibSprites.ENGINE_OVERHEAT;
            case RED, YELLOW -> BCLibSprites.ENGINE_WARM;
            default -> dynamo.isEngineOn() ? BCLibSprites.ENGINE_ACTIVE : BCLibSprites.ENGINE_INACTIVE;
        };
        GuiIcon.draw(graphics, sprite, x, y, x + 16, y + 16);
    }
}
