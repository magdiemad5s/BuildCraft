package buildcraft.builders.gui;



import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.ClientSnapshots;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.lib.gui.BuildCraftGui;
import buildcraft.lib.gui.help.DummyHelpElement;
import buildcraft.lib.gui.help.ElementHelpInfo;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.ledger.LedgerOwnership;
import buildcraft.lib.gui.pos.GuiRectangle;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenReplacer extends AbstractContainerScreen<MenuReplacer> {

    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftbuilders", "textures/gui/replacer.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 241;
    private static final int PREVIEW_X = 8;
    private static final int PREVIEW_Y = 9;
    private static final int PREVIEW_W = 160;
    private static final int PREVIEW_H = 100;
    private static final int STATUS_X = 30;
    private static final int STATUS_Y = 119;
    private static final int STATUS_W = 138;
    private static final int TEXT_COLOUR = 4210752;
    private static final int ERROR_COLOUR = 0xAA0000;
    private static final int OK_COLOUR = 0x2F6F2F;

    private final BuildCraftGui mainGui;

    public ScreenReplacer(MenuReplacer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.mainGui = new BuildCraftGui(this, BuildCraftGui.createWindowedArea(this));
        if (menu.tile != null) {
            this.mainGui.shownElements.add(new LedgerOwnership(this.mainGui, menu.tile, true));
        }
        this.mainGui.shownElements.add(new DummyHelpElement(
            new GuiRectangle(PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H).offset(this.mainGui.rootElement).expand(2),
            new ElementHelpInfo(
                "buildcraft.help.replacer.preview.title",
                0xFF_CC_AA_88,
                "buildcraft.help.replacer.preview.desc"
            )
        ));
        this.mainGui.shownElements.add(new DummyHelpElement(
            new GuiRectangle(8, 115, 18, 18).offset(this.mainGui.rootElement).expand(2),
            new ElementHelpInfo(
                "buildcraft.help.replacer.blueprint.title",
                0xFF_88_AA_CC,
                "buildcraft.help.replacer.blueprint.desc"
            )
        ));
        this.mainGui.shownElements.add(new DummyHelpElement(
            new GuiRectangle(8, 137, 18, 18).offset(this.mainGui.rootElement).expand(2),
            new ElementHelpInfo(
                "buildcraft.help.replacer.from.title",
                0xFF_CC_88_88,
                "buildcraft.help.replacer.from.desc"
            )
        ));
        this.mainGui.shownElements.add(new DummyHelpElement(
            new GuiRectangle(56, 137, 18, 18).offset(this.mainGui.rootElement).expand(2),
            new ElementHelpInfo(
                "buildcraft.help.replacer.to.title",
                0xFF_88_CC_88,
                "buildcraft.help.replacer.to.desc"
            )
        ));
        this.mainGui.shownElements.add(new LedgerHelp(this.mainGui, false));
        this.imageWidth = SIZE_X;
        this.imageHeight = SIZE_Y;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 148;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.mainGui.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE_BASE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderBlueprintPreview(guiGraphics);
        this.mainGui.drawBackgroundLayer(guiGraphics, partialTick, mouseX, mouseY, () -> {});
        this.mainGui.drawElementBackgrounds(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderReplacementStatus(guiGraphics);
        this.mainGui.preDrawForeground(guiGraphics.pose());
        this.mainGui.drawElementForegrounds(() -> {}, guiGraphics);
        this.mainGui.postDrawForeground(guiGraphics.pose());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        this.mainGui.onMouseClicked(mouseX, mouseY, button);
        return result;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean result = super.mouseReleased(mouseX, mouseY, button);
        this.mainGui.onMouseReleased(mouseX, mouseY, button);
        return result;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean result = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        this.mainGui.onMouseDragged(mouseX, mouseY, button, dragX, dragY);
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.mainGui.onKeyTyped(modifiers, InputConstants.getKey(keyCode, scanCode))) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderBlueprintPreview(GuiGraphics guiGraphics) {
        Snapshot snapshot = getInsertedSnapshot();
        if (snapshot == null) {
            renderPreviewMessage(guiGraphics, getPreviewMessage(), TEXT_COLOUR);
            return;
        }
        if (!(snapshot instanceof Blueprint blueprint)) {
            renderPreviewMessage(guiGraphics, Component.translatable("gui.buildcraftbuilders.replacer.preview.not_blueprint"), ERROR_COLOUR);
            return;
        }

        ClientSnapshots.INSTANCE.renderSnapshot(
            guiGraphics.pose(),
            blueprint,
            this.leftPos + PREVIEW_X,
            this.topPos + PREVIEW_Y,
            PREVIEW_W,
            PREVIEW_H
        );
    }

    private void renderReplacementStatus(GuiGraphics guiGraphics) {
        StatusLine status = getReplacementStatus();
        String text = this.font.plainSubstrByWidth(status.message.getString(), STATUS_W);
        guiGraphics.drawString(this.font, text, STATUS_X, STATUS_Y, status.colour, false);
    }

    private Snapshot getInsertedSnapshot() {
        ItemStack stack = this.menu.getSlot(0).getItem();
        Snapshot.Header header = ItemSnapshot.getHeader(stack);
        if (header == null) {
            return null;
        }
        return ClientSnapshots.INSTANCE.getSnapshot(header.key);
    }

    private Component getPreviewMessage() {
        ItemStack stack = this.menu.getSlot(0).getItem();
        Snapshot.Header header = ItemSnapshot.getHeader(stack);
        return header == null
            ? Component.translatable("gui.buildcraftbuilders.replacer.preview.no_blueprint")
            : Component.translatable("gui.buildcraftbuilders.replacer.preview.loading");
    }

    private StatusLine getReplacementStatus() {
        ItemStack blueprintStack = this.menu.getSlot(0).getItem();
        Snapshot.Header header = ItemSnapshot.getHeader(blueprintStack);
        if (header == null) {
            return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.no_blueprint"), TEXT_COLOUR);
        }
        ItemStack fromStack = this.menu.getSlot(1).getItem();
        if (fromStack.isEmpty()) {
            return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.missing_from"), TEXT_COLOUR);
        }
        ItemStack toStack = this.menu.getSlot(2).getItem();
        if (toStack.isEmpty()) {
            return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.missing_to"), TEXT_COLOUR);
        }
        Snapshot snapshot = ClientSnapshots.INSTANCE.getSnapshot(header.key);
        if (!(snapshot instanceof Blueprint blueprint)) {
            return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.loading"), TEXT_COLOUR);
        }
        try {
            ISchematicBlock from = ItemSchematicSingle.getSchematic(fromStack);
            ISchematicBlock to = ItemSchematicSingle.getSchematic(toStack);
            if (from == null || to == null) {
                return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.invalid_schematic"), ERROR_COLOUR);
            }
            if (Blueprint.schematicMatchesForReplacement(from, to)) {
                return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.same_schematic"), TEXT_COLOUR);
            }
            int matches = blueprint.countMatchingSchematic(from);
            return matches > 0
                ? new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.ready", matches), OK_COLOUR)
                : new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.no_match"), ERROR_COLOUR);
        } catch (InvalidInputDataException e) {
            return new StatusLine(Component.translatable("gui.buildcraftbuilders.replacer.status.invalid_schematic"), ERROR_COLOUR);
        }
    }

    private void renderPreviewMessage(GuiGraphics guiGraphics, Component message, int colour) {
        int x = this.leftPos + PREVIEW_X + (PREVIEW_W - this.font.width(message.getString())) / 2;
        int y = this.topPos + PREVIEW_Y + PREVIEW_H / 2 - 4;
        guiGraphics.drawString(this.font, message, x, y, colour, false);
    }

    private static final class StatusLine {
        private final Component message;
        private final int colour;

        private StatusLine(Component message, int colour) {
            this.message = message;
            this.colour = colour;
        }
    }
}
