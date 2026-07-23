package buildcraft.neo.forge1201.factory.client;

import buildcraft.neo.forge1201.LegacyModuleIds;
import buildcraft.neo.forge1201.factory.FactoryTankBlockEntity;
import buildcraft.neo.forge1201.factory.TankMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Factory Tank screen using the restored legacy background and gauge frame. */
public final class TankScreen extends AbstractContainerScreen<TankMenu> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(LegacyModuleIds.FACTORY, "textures/gui/tank.png");
    private static final int TANK_X = 80;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 64;

    public TankScreen(TankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 181;
        inventoryLabelX = 8;
        inventoryLabelY = 85;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.blit(GUI_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);

        int tankLeft = left + TANK_X;
        int tankTop = top + TANK_Y;
        FactoryTankBlockEntity tank = clientTank();
        if (tank != null) {
            FluidStack fluid = tank.getLocalFluid();
            int capacity = tank.getLocalCapacity();
            if (!fluid.isEmpty() && capacity > 0) {
                int height = Math.max(1, Math.min(TANK_HEIGHT, fluid.getAmount() * TANK_HEIGHT / capacity));
                int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid) | 0xFF000000;
                graphics.fill(tankLeft + 1, tankTop + TANK_HEIGHT - height, tankLeft + TANK_WIDTH - 1, tankTop + TANK_HEIGHT, color);
            }
        }
        graphics.blit(GUI_TEXTURE, tankLeft, tankTop, 176, 0, TANK_WIDTH, TANK_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!isPointWithinTank(mouseX, mouseY)) {
            return;
        }
        FactoryTankBlockEntity tank = clientTank();
        if (tank == null || tank.getLocalFluid().isEmpty()) {
            return;
        }
        FluidStack fluid = tank.getLocalFluid();
        graphics.renderTooltip(
            font,
            List.<FormattedCharSequence>of(
                fluid.getDisplayName().getVisualOrderText(),
                Component.literal(fluid.getAmount() + " / " + tank.getLocalCapacity() + " mB").getVisualOrderText()
            ),
            mouseX,
            mouseY
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && isPointWithinTank((int) mouseX, (int) mouseY)
            && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TankMenu.GAUGE_TRANSFER_BUTTON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isPointWithinTank(int mouseX, int mouseY) {
        return mouseX >= leftPos + TANK_X && mouseX < leftPos + TANK_X + TANK_WIDTH
            && mouseY >= topPos + TANK_Y && mouseY < topPos + TANK_Y + TANK_HEIGHT;
    }

    private FactoryTankBlockEntity clientTank() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        if (minecraft.level.getBlockEntity(menu.tankPos()) instanceof FactoryTankBlockEntity tank) {
            return tank;
        }
        return null;
    }
}
