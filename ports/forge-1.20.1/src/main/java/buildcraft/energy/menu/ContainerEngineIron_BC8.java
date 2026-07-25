/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.menu;

import buildcraft.energy.BCEnergyGuis;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.widget.WidgetFluidTank;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class ContainerEngineIron_BC8 extends ContainerBCTile<TileEngineIron_BC8> {
    public final WidgetFluidTank widgetTankFuel;
    public final WidgetFluidTank widgetTankCoolant;
    public final WidgetFluidTank widgetTankResidue;

    public ContainerEngineIron_BC8(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, createLevelAccess(playerInventory, buf));
    }

    public ContainerEngineIron_BC8(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(BCEnergyGuis.MENU_IRON.get(), playerInventory, containerId, access);

        addFullPlayerInventory(95);

        Tank fuel = tile == null ? new Tank("fuel", TileEngineIron_BC8.MAX_FLUID, null) : tile.tankFuel;
        Tank coolant = tile == null ? new Tank("coolant", TileEngineIron_BC8.MAX_FLUID, null) : tile.tankCoolant;
        Tank residue = tile == null ? new Tank("residue", TileEngineIron_BC8.MAX_FLUID, null) : tile.tankResidue;
        widgetTankFuel = addWidget(new WidgetFluidTank(this, fuel));
        widgetTankCoolant = addWidget(new WidgetFluidTank(this, coolant));
        widgetTankResidue = addWidget(new WidgetFluidTank(this, residue));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || super.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The only slots are player slots -- try to interact with all of the tanks.
        // Menu slot order is inventory rows followed by the hotbar, so the menu
        // slot itself must be used rather than indexing PlayerInventory directly.
        if (player.level().isClientSide || !stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        net.minecraft.world.inventory.Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack original = source.getItem().copy();
        ItemStack remainder = tile.tankFuel.transferStackToTank(player, original.copy());
        if (ItemStack.matches(remainder, original)) {
            remainder = tile.tankCoolant.transferStackToTank(player, original.copy());
        }
        if (ItemStack.matches(remainder, original)) {
            remainder = tile.tankResidue.transferStackToTank(player, original.copy());
        }
        if (!ItemStack.matches(remainder, original)) {
            source.set(remainder);
            source.setChanged();
            broadcastChanges();
            return original;
        }

        return super.quickMoveStack(player, index);
    }
}
