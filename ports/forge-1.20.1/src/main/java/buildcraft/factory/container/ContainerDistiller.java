/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/
 */
package buildcraft.factory.container;

import java.io.IOException;
import java.util.Arrays;

import buildcraft.factory.BCFactoryGuis;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.neo.forge1201.factory.DistillerMenuStatePolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server-authoritative menu for the BuildCraft 8 Distiller.
 *
 * <p>Only the player inventory has slots. Shift-clicking a fluid container tries
 * the input tank, while clicking a gauge transfers the carried fluid container
 * to or from that exact tank, matching the legacy GUI.</p>
 */
public class ContainerDistiller extends ContainerBCTile<TileDistiller_BC8> {
    private static final IdAllocator IDS = MenuBC_Neptune.IDS.makeChild("distiller");
    private static final int NET_DISTILLER_STATE = IDS.allocId("DISTILLER_STATE");

    public final ContainerData data = new SyncedDistillerData();

    private final int[] fluidIds = new int[DistillerMenuStatePolicy.TANK_COUNT];
    private final int[] fluidAmounts = new int[DistillerMenuStatePolicy.TANK_COUNT];
    private final int[] capacities = new int[DistillerMenuStatePolicy.TANK_COUNT];

    private final int[] lastSentFluidIds = createUninitializedState();
    private final int[] lastSentFluidAmounts = createUninitializedState();
    private final int[] lastSentCapacities = createUninitializedState();
    private boolean active;
    private boolean lastSentActive;
    private boolean sentInitialState;

    public ContainerDistiller(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, null, createLevelAccess(playerInventory, buffer));
    }

    public ContainerDistiller(int containerId, Inventory playerInventory, TileDistiller_BC8 distiller,
        ContainerLevelAccess access) {
        super(BCFactoryGuis.MENU_DISTILLER.get(), playerInventory, containerId, access);
        addFullPlayerInventory(8, 79);

        TileDistiller_BC8 actualDistiller = tile != null ? tile : distiller;
        if (actualDistiller != null) {
            updateLocalState(actualDistiller);
        } else {
            Arrays.fill(capacities, DistillerMenuStatePolicy.TANK_CAPACITY_MILLIBUCKETS);
        }
    }

    private static int[] createUninitializedState() {
        int[] values = new int[DistillerMenuStatePolicy.TANK_COUNT];
        Arrays.fill(values, Integer.MIN_VALUE);
        return values;
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (playerInventory.player.level().isClientSide || tile == null) {
            return;
        }

        updateLocalState(tile);
        boolean changed = !sentInitialState
            || !Arrays.equals(fluidIds, lastSentFluidIds)
            || !Arrays.equals(fluidAmounts, lastSentFluidAmounts)
            || !Arrays.equals(capacities, lastSentCapacities)
            || active != lastSentActive;
        if (!changed) {
            return;
        }

        sentInitialState = true;
        System.arraycopy(fluidIds, 0, lastSentFluidIds, 0, fluidIds.length);
        System.arraycopy(fluidAmounts, 0, lastSentFluidAmounts, 0, fluidAmounts.length);
        System.arraycopy(capacities, 0, lastSentCapacities, 0, capacities.length);
        lastSentActive = active;

        sendMessage(NET_DISTILLER_STATE, buffer -> {
            for (int tank = 0; tank < DistillerMenuStatePolicy.TANK_COUNT; tank++) {
                buffer.writeVarInt(fluidIds[tank]);
                buffer.writeInt(fluidAmounts[tank]);
                buffer.writeInt(capacities[tank]);
            }
            buffer.writeBoolean(active);
        });
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx)
        throws IOException {
        if (side == LogicalSide.CLIENT && id == NET_DISTILLER_STATE) {
            for (int tank = 0; tank < DistillerMenuStatePolicy.TANK_COUNT; tank++) {
                updateLocalState(tank, buffer.readVarInt(), buffer.readInt(), buffer.readInt());
            }
            active = buffer.readBoolean();
            return;
        }
        super.readMessage(id, buffer, side, ctx);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide || !stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack before = slot.getItem().copy();
        ItemStack working = before.copy();
        ItemStack result = access.evaluate((level, pos) -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TileDistiller_BC8 distiller)) {
                return working;
            }
            return distiller.tankIn.transferStackToTank(player, working);
        }).orElse(working);

        if (ItemStack.matches(before, result)) {
            return ItemStack.EMPTY;
        }
        slot.set(result);
        slot.setChanged();
        broadcastChanges();
        return before;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!DistillerMenuStatePolicy.isTankButton(buttonId) || getCarried().isEmpty()) {
            return false;
        }
        // Let vanilla send the validated button request. All mutation happens below
        // on the logical server.
        if (player.level().isClientSide) {
            return true;
        }
        if (!stillValid(player)) {
            return false;
        }

        return access.evaluate((level, pos) -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TileDistiller_BC8 distiller)) {
                return false;
            }

            Tank target = switch (DistillerMenuStatePolicy.tankIndexForButton(buttonId)) {
                case 0 -> distiller.tankIn;
                case 1 -> distiller.tankGasOut;
                case 2 -> distiller.tankLiquidOut;
                default -> throw new IllegalStateException("Unreachable Distiller tank index");
            };
            ItemStack before = getCarried().copy();
            ItemStack after = target.transferStackToTank(player, getCarried());
            if (ItemStack.matches(before, after)) {
                return false;
            }
            setCarried(after);
            broadcastChanges();
            return true;
        }).orElse(false);
    }

    @Override
    public boolean stillValid(Player player) {
        // The client can receive the open-screen packet one tick before the block
        // entity is installed. The server always performs the normal distance and
        // ownership validation through ContainerBCTile.
        if (player.level().isClientSide) {
            return true;
        }
        return super.stillValid(player);
    }

    public boolean isActive() {
        return active;
    }

    public int getFluidId(int tank) {
        checkTankIndex(tank);
        return fluidIds[tank];
    }

    public int getFluidAmount(int tank) {
        checkTankIndex(tank);
        return fluidAmounts[tank];
    }

    public int getCapacity(int tank) {
        checkTankIndex(tank);
        return capacities[tank];
    }

    @OnlyIn(Dist.CLIENT)
    public Fluid getFluid(int tank) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(getFluidId(tank));
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @OnlyIn(Dist.CLIENT)
    public FluidStack getFluidStack(int tank) {
        Fluid fluid = getFluid(tank);
        int amount = getFluidAmount(tank);
        return fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private void updateLocalState(TileDistiller_BC8 distiller) {
        updateLocalState(0, distiller.tankIn);
        updateLocalState(1, distiller.tankGasOut);
        updateLocalState(2, distiller.tankLiquidOut);
        active = distiller.isActive();
    }

    private void updateLocalState(int tankIndex, Tank tank) {
        FluidStack fluid = tank.getFluid();
        updateLocalState(
            tankIndex,
            BuiltInRegistries.FLUID.getId(fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid()),
            fluid.isEmpty() ? 0 : fluid.getAmount(),
            tank.getCapacity()
        );
    }

    private void updateLocalState(int tankIndex, int fluidId, int amount, int capacity) {
        checkTankIndex(tankIndex);
        fluidIds[tankIndex] = DistillerMenuStatePolicy.sanitizeFluidId(fluidId);
        capacities[tankIndex] = DistillerMenuStatePolicy.sanitizeCapacity(capacity);
        fluidAmounts[tankIndex] = DistillerMenuStatePolicy.clampAmount(amount, capacities[tankIndex]);
    }

    private static void checkTankIndex(int tank) {
        if (tank < 0 || tank >= DistillerMenuStatePolicy.TANK_COUNT) {
            throw new IndexOutOfBoundsException("Distiller tank index " + tank);
        }
    }

    private final class SyncedDistillerData implements ContainerData {
        @Override
        public int get(int index) {
            if (index < 0 || index >= DistillerMenuStatePolicy.DATA_VALUE_COUNT) {
                return 0;
            }
            int tank = index / DistillerMenuStatePolicy.VALUES_PER_TANK;
            return switch (index % DistillerMenuStatePolicy.VALUES_PER_TANK) {
                case 0 -> fluidIds[tank];
                case 1 -> fluidAmounts[tank];
                case 2 -> capacities[tank];
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index < 0 || index >= DistillerMenuStatePolicy.DATA_VALUE_COUNT) {
                return;
            }
            int tank = index / DistillerMenuStatePolicy.VALUES_PER_TANK;
            switch (index % DistillerMenuStatePolicy.VALUES_PER_TANK) {
                case 0 -> fluidIds[tank] = DistillerMenuStatePolicy.sanitizeFluidId(value);
                case 1 -> fluidAmounts[tank] = DistillerMenuStatePolicy.clampAmount(value, capacities[tank]);
                case 2 -> {
                    capacities[tank] = DistillerMenuStatePolicy.sanitizeCapacity(value);
                    fluidAmounts[tank] =
                        DistillerMenuStatePolicy.clampAmount(fluidAmounts[tank], capacities[tank]);
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DistillerMenuStatePolicy.DATA_VALUE_COUNT;
        }
    }
}
