/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.util.Optional;

import javax.annotation.Nullable;

import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;

public abstract class ContainerBCTile<T extends TileBC_Neptune> extends MenuBC_Neptune implements IMenuBCTile {
    /**
     * The backing tile can briefly be absent on the physical client when Forge's
     * menu-open packet wins the race with the chunk/block-entity packet.
     *
     * <p>Keep this public field for source compatibility with the restored
     * BuildCraft screens, but allow {@link #getTile()} to populate it once the
     * client level catches up.</p>
     */
    @Nullable
	public T tile;
    protected final ContainerLevelAccess access;

	public ContainerBCTile(MenuType<?> type, Inventory playerInventory, int id, ContainerLevelAccess access) {
        super(playerInventory, type, id);
        this.access = access;
        this.tile = resolveTile();
    }

    @Nullable
    @SuppressWarnings({ "unchecked" })
    private T resolveTile() {
        Optional<T> resolved = access.evaluate((level, pos) -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TileBC_Neptune b)) {
                return Optional.<T>empty();
            }
            if (!level.isClientSide) {
                b.onPlayerOpen(playerInventory.player);
            }
            return Optional.of((T) b);
        }, Optional.empty());
        return resolved.orElse(null);
    }

    /**
     * Resolves a tile that arrived after the client menu packet.
     *
     * <p>Server menus normally resolve during construction. Re-resolution is
     * intentionally limited to a missing tile so {@code onPlayerOpen} is never
     * invoked twice for the same menu.</p>
     */
    @Nullable
    public T getTile() {
        if (tile == null) {
            tile = resolveTile();
        }
        return tile;
    }

    @Override
    public TileBC_Neptune getBCTile() {
        return getTile();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if(tile != null)
        	tile.onPlayerClose(player);
    }

    @Override
    public  boolean stillValid(Player player) {
        T resolved = getTile();
        // The server remains authoritative and rejects a menu whose backing
        // tile is missing. The client must stay alive long enough for its
        // block-entity packet to arrive.
		return resolved != null ? resolved.canInteractWith(player) : player.level().isClientSide;
    }

	@Override
    public void broadcastChanges() {
        super.broadcastChanges();
        T resolved = getTile();
        if(resolved != null)
			resolved.sendNetworkGuiTick(playerInventory.player);
    }
}
