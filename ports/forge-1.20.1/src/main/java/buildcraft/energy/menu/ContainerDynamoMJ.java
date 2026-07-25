/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.menu;

import buildcraft.energy.BCEnergyGuis;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Four legacy upgrade slots plus the complete player inventory. */
public class ContainerDynamoMJ extends ContainerBCTile<TileDynamoMJ> {
    private final ItemHandlerSimple fallbackUpgrades;

    public ContainerDynamoMJ(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, null, createLevelAccess(playerInventory, buffer));
    }

    public ContainerDynamoMJ(
        int containerId,
        Inventory playerInventory,
        IItemHandlerAdv upgrades,
        ContainerLevelAccess access
    ) {
        super(BCEnergyGuis.DYNAMO_MJ.get(), playerInventory, containerId, access);
        fallbackUpgrades = new ItemHandlerSimple(4);
        IItemHandlerAdv actualUpgrades = tile != null
            ? tile.invUpgrades
            : upgrades != null ? upgrades : fallbackUpgrades;

        addFullPlayerInventory(95);
        for (int slot = 0; slot < 4; slot++) {
            addSlot(new SlotBase(actualUpgrades, slot, 44 + 18 * slot, 44));
        }
    }
}
