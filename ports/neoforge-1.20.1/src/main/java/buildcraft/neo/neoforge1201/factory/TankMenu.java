package buildcraft.neo.neoforge1201.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Server-authoritative Factory Tank menu.
 *
 * <p>There are deliberately no custom C2S packets: slot operations and the
 * validated vanilla menu-button path execute only on the logical server.</p>
 */
public final class TankMenu extends AbstractContainerMenu {
    public static final int GAUGE_TRANSFER_BUTTON = 0;
    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 36;

    private final ContainerLevelAccess access;
    private final BlockPos tankPos;

    public TankMenu(int containerId, Inventory playerInventory, BlockPos tankPos) {
        super(FactoryTankRegistries.tankMenu(), containerId);
        this.tankPos = tankPos.immutable();
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), this.tankPos);
        addPlayerInventory(playerInventory);
    }

    public static TankMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new TankMenu(containerId, playerInventory, buffer.readBlockPos());
    }

    public BlockPos tankPos() {
        return tankPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
            player.level() == level
                && level.getBlockEntity(pos) instanceof FactoryTankBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
        ).orElse(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < PLAYER_INVENTORY_START || slotIndex >= PLAYER_INVENTORY_END) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || player.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem().copy();
        ContainerTransfer transfer = withTank(player, tank -> transferFluidContainer(player, slot.getItem(), tank));
        if (!transfer.success()) {
            return ItemStack.EMPTY;
        }
        slot.set(transfer.result());
        slot.setChanged();
        broadcastChanges();
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != GAUGE_TRANSFER_BUTTON || player.level().isClientSide) {
            return false;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return false;
        }
        ContainerTransfer transfer = withTank(player, tank -> transferFluidContainer(player, carried, tank));
        if (!transfer.success()) {
            return false;
        }
        setCarried(transfer.result());
        broadcastChanges();
        return true;
    }

    private ContainerTransfer withTank(Player player, TankOperation operation) {
        if (!stillValid(player)) {
            return ContainerTransfer.failure();
        }
        return access.evaluate((level, pos) -> {
            if (!(level.getBlockEntity(pos) instanceof FactoryTankBlockEntity tank)) {
                return ContainerTransfer.failure();
            }
            return tank.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .map(operation::apply)
                .orElse(ContainerTransfer.failure());
        }).orElse(ContainerTransfer.failure());
    }

    private static ContainerTransfer transferFluidContainer(Player player, ItemStack stack, IFluidHandler tank) {
        return player.getCapability(ForgeCapabilities.ITEM_HANDLER).map(inventory -> {
            // Legacy Tank.transferStackToTank prioritised emptying a held
            // container into the tank before filling it from the tank.
            FluidActionResult operation = FluidUtil.tryEmptyContainerAndStow(
                stack,
                tank,
                inventory,
                Integer.MAX_VALUE,
                player,
                true
            );
            if (!operation.isSuccess()) {
                operation = FluidUtil.tryFillContainerAndStow(
                    stack,
                    tank,
                    inventory,
                    Integer.MAX_VALUE,
                    player,
                    true
                );
            }
            return operation.isSuccess() ? ContainerTransfer.success(operation.getResult()) : ContainerTransfer.failure();
        }).orElse(ContainerTransfer.failure());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 99 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 157));
        }
    }

    @FunctionalInterface
    private interface TankOperation {
        ContainerTransfer apply(IFluidHandler tank);
    }

    private record ContainerTransfer(boolean success, ItemStack result) {
        private static ContainerTransfer success(ItemStack result) {
            return new ContainerTransfer(true, result);
        }

        private static ContainerTransfer failure() {
            return new ContainerTransfer(false, ItemStack.EMPTY);
        }
    }
}
