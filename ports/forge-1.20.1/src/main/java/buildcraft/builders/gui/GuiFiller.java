package buildcraft.builders.gui;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.tiles.IControllable.Mode;
import buildcraft.builders.filler.FillerStatementContext;
import buildcraft.builders.menu.ContainerFiller;
import buildcraft.builders.tile.TileFiller;
import buildcraft.core.BCCoreSprites;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.button.IButtonBehaviour;
import buildcraft.lib.gui.button.IButtonClickEventListener;
import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.json.InventorySlotHolder;
import buildcraft.lib.gui.json.SpriteDelegate;
import buildcraft.lib.misc.collect.TypedKeyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import buildcraft.lib.gui.help.GuiHelpUtil;

public class GuiFiller extends GuiBC8<ContainerFiller> {
    private static final ResourceLocation LOCATION = new ResourceLocation("buildcraftbuilders:gui/filler.json");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 241;
    private static final SpriteDelegate SPRITE_PATTERN = new SpriteDelegate();
    private static final SpriteDelegate SPRITE_CONTROL_MODE = new SpriteDelegate();
    private boolean jsonLoaded;

    public GuiFiller(ContainerFiller container, Inventory inv, Component title) {
        super(container, LOCATION, inv, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        tryLoadJson();
    }

    private boolean tryLoadJson() {
        if (jsonLoaded) {
            return true;
        }
        TileFiller tile = container.getTile();
        if (tile == null) {
            return false;
        }

        BuildCraftJsonGui jsonGui = (BuildCraftJsonGui) mainGui;
        preLoad(jsonGui, tile);
        jsonGui.load();
        imageWidth = jsonGui.getSizeX();
        imageHeight = jsonGui.getSizeY();
        GuiHelpUtil.addRoot(mainGui, 8, 85, 162, 54, "buildcraft.help.filler.resources.title", 0xFF_88_CC_88, "buildcraft.help.filler.resources.desc");
        GuiHelpUtil.addRoot(mainGui, 9, 35, 158, 30, "buildcraft.help.filler.pattern.title", 0xFF_66_AA_FF, "buildcraft.help.filler.pattern.desc");
        GuiHelpUtil.addRoot(mainGui, 8, 64, 162, 14, "buildcraft.help.filler.work.title", 0xFF_DD_CC_55, "buildcraft.help.filler.work.desc");
        jsonLoaded = true;
        return true;
    }

    protected void preLoad(BuildCraftJsonGui json, TileFiller tile) {
        TypedKeyMap<String, Object> properties = json.properties;
        FunctionContext context = json.context;
        properties.put("filler.inventory", new InventorySlotHolder(container, container.getResources()));
        properties.put("statement.container", tile);
        properties.put("controllable", tile);
        properties.put("controllable.sprite", SPRITE_CONTROL_MODE);
        context.put_o("controllable.mode", Mode.class, tile::getControlMode);
        context.put_b("filler.is_finished", tile::isFinished);
        context.put_b("filler.is_locked", container::isLocked);
        context.put_l("filler.to_break", tile::getCountToBreak);
        context.put_l("filler.to_place", tile::getCountToPlace);
        properties.put("filler.possible", FillerStatementContext.CONTEXT_ALL);
        properties.put("filler.pattern", container.getPatternStatementClient());
        properties.put("filler.pattern.sprite", SPRITE_PATTERN);

        context.put_b("filler.invert", container::isInverted);
        properties.put("filler.invert", IButtonBehaviour.TOGGLE);
        properties.put("filler.invert", container.isInverted());
        properties.put("filler.invert",
            (IButtonClickEventListener) (b, k) -> container.sendInverted(b.isButtonActive()));

        context.put_b("filler.excavate", tile::canExcavate);
        properties.put("filler.excavate", IButtonBehaviour.TOGGLE);
        properties.put("filler.excavate", tile.canExcavate());
        properties.put("filler.excavate",
            (IButtonClickEventListener) (b, k) -> tile.sendCanExcavate(b.isButtonActive()));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        tryLoadJson();
        IFillerPattern pattern = container.getPatternStatementClient().get();
        SPRITE_PATTERN.delegate = pattern == null ? null : pattern.getSprite();
        TileFiller tile = container.getTile();
        SPRITE_CONTROL_MODE.delegate = BCCoreSprites.ACTION_MACHINE_CONTROL.get(
            tile == null ? Mode.OFF : tile.getControlMode()
        );
    }
}
