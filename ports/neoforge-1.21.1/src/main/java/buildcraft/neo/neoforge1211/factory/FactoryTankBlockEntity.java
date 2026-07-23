// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Fluid storage and capability boundary for the Factory Tank.
 *
 * <p>The public NBT key remains {@code tank}; the 1.12.2 nested
 * {@code tanks.tank} shape is accepted while loading. Unknown legacy fluid
 * identifiers are retained verbatim rather than silently discarded.</p>
 */
public final class FactoryTankBlockEntity extends BlockEntity implements MenuProvider {
    private final FluidTank tank = new FluidTank(FactoryTankContract.CAPACITY_MILLIBUCKETS) {
        @Override
        protected void onContentsChanged() {
            onTankContentsChanged();
        }
    };
    private final TankFluidHandler fluidHandler = new TankFluidHandler();
    @Nullable
    private CompoundTag unresolvedFluidPayload;
    private int lastComparatorLevel;

    public FactoryTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryTankRegistries.TANK_BLOCK_ENTITY.get(), pos, state);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (tank.getFluid().isEmpty() && unresolvedFluidPayload != null) {
            tag.put(FactoryTankContract.FLUID_NBT_KEY, unresolvedFluidPayload.copy());
        } else {
            tag.put(FactoryTankContract.FLUID_NBT_KEY, tank.getFluid().saveOptional(registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.setFluid(FluidStack.EMPTY);
        unresolvedFluidPayload = null;
        if (tag.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND)) {
            readBoundedTank(registries, tag.getCompound(FactoryTankContract.FLUID_NBT_KEY));
        } else if (tag.contains("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag legacyTanks = tag.getCompound("tanks");
            if (legacyTanks.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND)) {
                readBoundedTank(registries, legacyTanks.getCompound(FactoryTankContract.FLUID_NBT_KEY));
            }
        }
        lastComparatorLevel = getComparatorLevel();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public IFluidHandler fluidHandler() {
        return fluidHandler;
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
        List<FactoryTankBlockEntity> order = ordered(tanks, isGaseous(fluid), true);
        for (int index = 1; index < order.size(); index++) {
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
            destination.tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void readBoundedTank(HolderLookup.Provider registries, CompoundTag serializedTank) {
        try {
            FluidStack loaded = decodeStoredFluid(registries, serializedTank);
            if (loaded.isEmpty()) {
                if (hasFluidIdentifier(serializedTank)) {
                    unresolvedFluidPayload = serializedTank.copy();
                }
                return;
            }
            int amount = TankAmountPolicy.clampStoredAmount(loaded.getAmount(), tank.getCapacity());
            if (amount <= 0) {
                return;
            }
            tank.setFluid(loaded.copyWithAmount(amount));
        } catch (RuntimeException ignored) {
            // Invalid external or edited NBT must not leave a negative/over-capacity tank behind.
            tank.setFluid(FluidStack.EMPTY);
        }
    }

    private static FluidStack decodeStoredFluid(HolderLookup.Provider registries, CompoundTag serializedTank) {
        FluidStack direct = FluidStack.parseOptional(registries, serializedTank);
        if (!direct.isEmpty()) {
            return direct;
        }

        // NeoForge's FluidTank helper wraps a modern stack inside a "Fluid" compound.
        if (serializedTank.contains("Fluid", Tag.TAG_COMPOUND)) {
            FluidStack wrapped = FluidStack.parseOptional(registries, serializedTank.getCompound("Fluid"));
            if (!wrapped.isEmpty()) {
                return wrapped;
            }
        }

        // BuildCraft 1.12.2 ultimately delegated to Forge FluidStack NBT:
        // {FluidName: "namespace:path", Amount: int, ...}.
        if (!serializedTank.contains("FluidName", Tag.TAG_STRING) || !serializedTank.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
            return FluidStack.EMPTY;
        }
        ResourceLocation fluidId = ResourceLocation.tryParse(serializedTank.getString("FluidName"));
        if (fluidId == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, serializedTank.getInt("Amount"));
    }

    private static boolean hasFluidIdentifier(CompoundTag serializedTank) {
        if (serializedTank.contains("FluidName", Tag.TAG_STRING) || serializedTank.contains("id", Tag.TAG_STRING)) {
            return true;
        }
        return serializedTank.contains("Fluid", Tag.TAG_COMPOUND)
            && hasFluidIdentifier(serializedTank.getCompound("Fluid"));
    }

    private void onTankContentsChanged() {
        setChanged();
        unresolvedFluidPayload = null;
        if (level != null && !level.isClientSide) {
            int currentComparatorLevel = getComparatorLevel();
            if (currentComparatorLevel != lastComparatorLevel) {
                lastComparatorLevel = currentComparatorLevel;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
            if (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, fluid)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGaseous(FluidStack fluid) {
        return fluid.getFluidType().isLighterThanAir();
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
            if (combined.isEmpty() || !FluidStack.isSameFluidSameComponents(combined, resource)) {
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
            FluidStack result = fluid.copyWithAmount(0);
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
            FluidStack result = fluid.copyWithAmount(0);
            for (FactoryTankBlockEntity tankEntity : tanks) {
                result.grow(tankEntity.tank.getFluidAmount());
            }
            return result;
        }
    }
}
