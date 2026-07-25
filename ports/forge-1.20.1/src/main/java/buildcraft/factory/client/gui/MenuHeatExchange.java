package buildcraft.factory.client.gui;

import javax.annotation.Nullable;
import java.util.Optional;


import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryGuis;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.gui.IMenuBCTile;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.neo.forge1201.factory.HeatExchangeMenuStatePolicy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class MenuHeatExchange extends MenuBC_Neptune implements IMenuBCTile {

    private final ContainerLevelAccess access;
    protected ContainerData data;
    @Nullable
    public final TileHeatExchange tile;
    public final DataSlot processState;

    public MenuHeatExchange(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemStackHandler(4), new SimpleContainerData(HeatExchangeMenuStatePolicy.CLIENT_DATA_COUNT), DataSlot.standalone(), createLevelAccess(playerInventory, buf));
    }

    public MenuHeatExchange(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(4), new SimpleContainerData(HeatExchangeMenuStatePolicy.CLIENT_DATA_COUNT), DataSlot.standalone(), ContainerLevelAccess.NULL);
    }

    public MenuHeatExchange(int containerId, Inventory playerInventory, IItemHandler item,
        ContainerData tank, DataSlot bg, ContainerLevelAccess access) {
        super(playerInventory, BCFactoryGuis.MENU_HEAT_EXCHANGE.get(), containerId);
        this.access = access;
        this.data = tank;
        this.processState = bg;
        this.tile = access.evaluate((level, pos) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileHeatExchange heatExchange) {
                if (!level.isClientSide) {
                    heatExchange.onPlayerOpen(playerInventory.player);
                }
                return Optional.of(heatExchange);
            }
            return Optional.<TileHeatExchange>empty();
        }, Optional.<TileHeatExchange>empty()).orElse(null);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 89 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 147));
        }
        addDataSlots(tank);
        addDataSlot(bg);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return buildcraft.lib.gui.BCMenuUtil.quickMoveStack(this, player, index);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (tile != null) {
            tile.onPlayerClose(player);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (tile != null) {
            tile.sendNetworkGuiTick(playerInventory.player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (tile != null) {
            return tile.canInteractWith(player);
        }
        return buildcraft.lib.gui.BCMenuUtil.stillValidBlock(this.access, player, BCFactoryBlocks.HEATEXCHANGE_BLOCK.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int index) {
        return access.evaluate((level, pos) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileHeatExchange heatExchange) {
                int tankIndex = HeatExchangeMenuStatePolicy.tankIndexForButton(index);
                if (tankIndex < 0) {
                    return false;
                }
                Tank tank = heatExchange.getSectionTank(tankIndex);
                if (tank != null) {
                    tank.onGuiClicked(this, player);
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    @Override
    public TileBC_Neptune getBCTile() {
        return tile;
    }
}
