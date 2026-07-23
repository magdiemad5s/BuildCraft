package buildcraft.neo.forge1201.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Fluid storage and capability boundary for the Factory Tank.
 *
 * <p>The public NBT key remains {@code tank}; an older nested {@code tanks.tank}
 * shape is accepted while loading so migration data is not silently discarded.</p>
 */
public final class FactoryTankBlockEntity extends BlockEntity implements MenuProvider {
    private final FluidTank tank = new FluidTank(FactoryTankContract.CAPACITY_MILLIBUCKETS) {
        @Override
        protected void onContentsChanged() {
            onTankContentsChanged();
        }
    };
    private final TankFluidHandler fluidHandler = new TankFluidHandler();
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidHandler);
    private int lastComparatorLevel;

    public FactoryTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryTankRegistries.tankBlockEntityType(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.buildcraftfactory.tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TankMenu(containerId, inventory, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            balanceVerticalTanks();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(FactoryTankContract.FLUID_NBT_KEY, tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.setFluid(FluidStack.EMPTY);
        if (tag.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND)) {
            readBoundedTank(tag.getCompound(FactoryTankContract.FLUID_NBT_KEY));
        } else if (tag.contains("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag legacyTanks = tag.getCompound("tanks");
            if (legacyTanks.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND)) {
                readBoundedTank(legacyTanks.getCompound(FactoryTankContract.FLUID_NBT_KEY));
            }
        }
        lastComparatorLevel = getComparatorLevel();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getComparatorLevel() {
        return VerticalTankPolicy.comparatorLevel(tank.getFluidAmount(), tank.getCapacity());
    }

    /** Returns a defensive snapshot; callers cannot mutate the internal tank directly. */
    public FluidStack getLocalFluid() {
        return tank.getFluid().copy();
    }

    public int getLocalCapacity() {
        return tank.getCapacity();
    }

    /** Rebalances a connected vertical stack without creating or destroying fluid. */
    public void balanceVerticalTanks() {
        List<FactoryTankBlockEntity> tanks = connectedTanks();
        FluidStack fluid = firstFluid(tanks);
        if (fluid.isEmpty() || !allContainOnly(tanks, fluid)) {
            return;
        }
        boolean gaseous = isGaseous(fluid);
        List<FactoryTankBlockEntity> order = ordered(tanks, gaseous, true);
        // A transfer can free capacity that was blocking a more distant tank.
        // Each pass moves fluid at least one place toward the settled end, so
        // no more than one fewer pass than the stack length is needed.
        for (int pass = 0; pass < order.size() - 1; pass++) {
            boolean moved = false;
            for (int index = order.size() - 1; index > 0; index--) {
                FactoryTankBlockEntity destination = order.get(index - 1);
                FactoryTankBlockEntity source = order.get(index);
                FluidStack offered = source.tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (offered.isEmpty()) {
                    continue;
                }
                int accepted = destination.tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) {
                    continue;
                }
                FluidStack drained = source.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                if (drained.isEmpty()) {
                    continue;
                }
                destination.tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                moved = true;
            }
            if (!moved) {
                break;
            }
        }
    }

    private void readBoundedTank(CompoundTag serializedTank) {
        try {
            FluidStack loaded = FluidStack.loadFluidStackFromNBT(serializedTank);
            if (loaded.isEmpty()) {
                return;
            }
            int amount = TankAmountPolicy.clampStoredAmount(loaded.getAmount(), tank.getCapacity());
            if (amount <= 0) {
                return;
            }
            FluidStack bounded = loaded.copy();
            bounded.setAmount(amount);
            tank.setFluid(bounded);
        } catch (RuntimeException ignored) {
            // Invalid external or edited NBT must not leave a negative/over-capacity tank behind.
            tank.setFluid(FluidStack.EMPTY);
        }
    }

    private void onTankContentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            int currentComparatorLevel = getComparatorLevel();
            if (currentComparatorLevel != lastComparatorLevel) {
                lastComparatorLevel = currentComparatorLevel;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private List<FactoryTankBlockEntity> connectedTanks() {
        if (level == null) {
            return List.of(this);
        }
        List<FactoryTankBlockEntity> tanks = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        while (true) {
            BlockEntity blockEntity = level.getBlockEntity(cursor);
            if (!(blockEntity instanceof FactoryTankBlockEntity tankEntity)) {
                break;
            }
            tanks.add(tankEntity);
            cursor.move(Direction.DOWN);
        }
        Collections.reverse(tanks);
        cursor.set(worldPosition);
        cursor.move(Direction.UP);
        while (true) {
            BlockEntity blockEntity = level.getBlockEntity(cursor);
            if (!(blockEntity instanceof FactoryTankBlockEntity tankEntity)) {
                break;
            }
            tanks.add(tankEntity);
            cursor.move(Direction.UP);
        }
        return tanks;
    }

    private static List<FactoryTankBlockEntity> ordered(
        List<FactoryTankBlockEntity> tanks,
        boolean gaseous,
        boolean filling
    ) {
        int[] indices = VerticalTankPolicy.traversal(tanks.size(), gaseous, filling);
        List<FactoryTankBlockEntity> result = new ArrayList<>(tanks.size());
        for (int index : indices) {
            result.add(tanks.get(index));
        }
        return result;
    }

    private static FluidStack firstFluid(List<FactoryTankBlockEntity> tanks) {
        for (FactoryTankBlockEntity tankEntity : tanks) {
            if (!tankEntity.tank.getFluid().isEmpty()) {
                return tankEntity.tank.getFluid();
            }
        }
        return FluidStack.EMPTY;
    }

    private static boolean allContainOnly(List<FactoryTankBlockEntity> tanks, FluidStack fluid) {
        for (FactoryTankBlockEntity tankEntity : tanks) {
            FluidStack held = tankEntity.tank.getFluid();
            if (!held.isEmpty() && !held.isFluidEqual(fluid)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGaseous(FluidStack fluid) {
        return fluid.getFluid().getFluidType().isLighterThanAir();
    }

    private final class TankFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? combinedFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            if (tankIndex != 0) {
                return 0;
            }
            long capacity = 0;
            for (FactoryTankBlockEntity tankEntity : connectedTanks()) {
                capacity += tankEntity.tank.getCapacity();
            }
            return (int) Math.min(Integer.MAX_VALUE, capacity);
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return tankIndex == 0 && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            List<FactoryTankBlockEntity> tanks = connectedTanks();
            if (!allContainOnly(tanks, resource)) {
                return 0;
            }
            FluidStack remaining = resource.copy();
            int filled = 0;
            for (FactoryTankBlockEntity tankEntity : ordered(tanks, isGaseous(resource), true)) {
                int accepted = tankEntity.tank.fill(remaining, action);
                if (accepted <= 0) {
                    continue;
                }
                filled += accepted;
                remaining.shrink(accepted);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack combined = combinedFluid();
            if (combined.isEmpty() || !combined.isFluidEqual(resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            List<FactoryTankBlockEntity> tanks = connectedTanks();
            FluidStack fluid = firstFluid(tanks);
            if (fluid.isEmpty() || !allContainOnly(tanks, fluid)) {
                return FluidStack.EMPTY;
            }
            FluidStack result = fluid.copy();
            result.setAmount(0);
            int remaining = maxDrain;
            for (FactoryTankBlockEntity tankEntity : ordered(tanks, isGaseous(fluid), false)) {
                FluidStack drained = tankEntity.tank.drain(remaining, action);
                if (drained.isEmpty()) {
                    continue;
                }
                result.grow(drained.getAmount());
                remaining -= drained.getAmount();
                if (remaining == 0) {
                    break;
                }
            }
            return result;
        }

        private FluidStack combinedFluid() {
            List<FactoryTankBlockEntity> tanks = connectedTanks();
            FluidStack fluid = firstFluid(tanks);
            if (fluid.isEmpty() || !allContainOnly(tanks, fluid)) {
                return FluidStack.EMPTY;
            }
            FluidStack result = fluid.copy();
            result.setAmount(0);
            for (FactoryTankBlockEntity tankEntity : tanks) {
                result.grow(tankEntity.tank.getFluidAmount());
            }
            return result;
        }
    }
}
