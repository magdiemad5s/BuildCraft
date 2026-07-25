/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.gui;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.builders.filler.FillerStatementContext;
import buildcraft.builders.menu.ContainerFillerPlanner;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.button.IButtonBehaviour;
import buildcraft.lib.gui.button.IButtonClickEventListener;
import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.json.SpriteDelegate;
import buildcraft.lib.misc.collect.TypedKeyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The original compact, JSON-driven Filler Planner screen. */
public class GuiFillerPlanner extends GuiBC8<ContainerFillerPlanner> {
    private static final ResourceLocation LOCATION =
        new ResourceLocation("buildcraftbuilders:gui/filler_planner.json");
    private static final SpriteDelegate SPRITE_PATTERN = new SpriteDelegate();

    public GuiFillerPlanner(ContainerFillerPlanner container, Inventory inventory, Component title) {
        super(container, LOCATION, inventory, title);

        BuildCraftJsonGui jsonGui = (BuildCraftJsonGui) mainGui;
        preLoad(jsonGui);
        jsonGui.load();
        imageWidth = jsonGui.getSizeX();
        imageHeight = jsonGui.getSizeY();
    }

    protected void preLoad(BuildCraftJsonGui json) {
        TypedKeyMap<String, Object> properties = json.properties;
        FunctionContext context = json.context;

        properties.put("filler.possible", FillerStatementContext.CONTEXT_ALL);
        properties.put("filler.pattern", container.getPatternStatementClient());
        properties.put("filler.pattern.sprite", SPRITE_PATTERN);

        context.put_b("filler.invert", container::isInverted);
        properties.put("filler.invert", IButtonBehaviour.TOGGLE);
        properties.put("filler.invert", container.isInverted());
        properties.put(
            "filler.invert",
            (IButtonClickEventListener) (button, key) -> container.sendInverted(button.isButtonActive())
        );
    }

    @Override
    public void containerTick() {
        super.containerTick();
        IFillerPattern pattern = container.getPatternStatementClient().get();
        SPRITE_PATTERN.delegate = pattern == null ? null : pattern.getSprite();
    }
}
