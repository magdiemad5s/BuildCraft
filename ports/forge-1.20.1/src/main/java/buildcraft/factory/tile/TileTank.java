/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.IFluidFilter;
import buildcraft.api.core.IFluidHandlerAdv;
import buildcraft.api.items.FluidItemDrops;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerTank;
import buildcraft.lib.fluid.FluidSmoother;
import buildcraft.lib.fluid.FluidSmoother.FluidStackInterp;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.FluidUtilBC;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.neo.forge1201.factory.FactoryTankContract;
import buildcraft.neo.forge1201.factory.TankAmountPolicy;
import buildcraft.neo.forge1201.factory.VerticalTankPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class TileTank extends TileBC_Neptune implements IDebuggable, IFluidHandlerAdv, MenuProvider {
    public static final int NET_FLUID_DELTA = IDS.allocId("FLUID_DELTA");

    private static final ResourceLocation ADVANCEMENT_STORE_FLUIDS = new ResourceLocation(
        "buildcraftfactory:fluid_storage"
    );

    public final Tank tank;
    public final FluidSmoother smoothedTank;

    private int lastComparatorLevel;
    private FluidStack lastObservedFluid = FluidStack.EMPTY;
    private int lastObservedCapacity = -1;
    private boolean hasObservedLocalState;

    public TileTank(BlockPos pos, BlockState state) {
        this(BCFactoryBlocks.ENTITYBLOCKTANK.get(), pos, state);
    }

    public TileTank(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, 16 * FluidType.BUCKET_VOLUME, pos, state);
    }

    protected TileTank(int capacity, BlockPos pos, BlockState state) {
        this(BCFactoryBlocks.ENTITYBLOCKTANK.get(), capacity, pos, state);
    }

    public TileTank(BlockEntityType<?> type, int capacity, BlockPos pos, BlockState state) {
        this(type, new Tank("tank", capacity, null), pos, state);
    }

    protected TileTank(Tank tank, BlockPos pos, BlockState state) {
        this(BCFactoryBlocks.ENTITYBLOCKTANK.get(), tank, pos, state);
    }

    public TileTank(BlockEntityType<?> type, Tank tank, BlockPos pos, BlockState state) {
    	super(type, pos, state);
        tank.setBlockEntity(this);
        this.tank = tank;
        tankManager.addLast(tank);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, this, EnumPipePart.VALUES);
        smoothedTank = new FluidSmoother(w -> createAndSendMessage(NET_FLUID_DELTA, w), tank);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public int getComparatorLevel() {
        return VerticalTankPolicy.comparatorLevel(tank.getFluidAmount(), tank.getCapacity());
    }

    /**
     * Sets the capacity of the local tank in millibuckets.
     * <p>
     * Addons that expose different tank tiers should normally prefer the
     * {@link #TileTank(BlockEntityType, int, BlockPos, BlockState)} constructor so the capacity is already correct when
     * the block entity is created. This method is kept as a small compatibility helper for upgrade-style code.
     */
    public void setTankCapacity(int capacity) {
        if (capacity == tank.getCapacity()) {
            return;
        }
        tank.setCapacity(capacity);
        onLocalTankContentsChanged();
    }

    // ITickable

    public void update() {
        if (level == null) {
            return;
        }
        smoothedTank.tick(level);

        if (!level.isClientSide) {
            detectDirectTankMutation();
            int compLevel = getComparatorLevel();
            if (compLevel != lastComparatorLevel) {
                lastComparatorLevel = compLevel;
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    // BlockEntity

    @Override
    public void onPlacedBy(LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (!placer.level().isClientSide) {
            balanceTankFluids();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            lastComparatorLevel = getComparatorLevel();
            rememberLocalState();
            balanceTankFluids();
        }
    }

    /** Moves fluids around to their preferred positions. (For gaseous fluids this will move everything as high as
     * possible, for liquid fluids this will move everything as low as possible.) */
    public void balanceTankFluids() {
        List<TileTank> tanks = getConnectedTanks();
        FluidStack fluid = FluidStack.EMPTY;
        for (TileTank tile : tanks) {
            FluidStack held = tile.tank.getFluid();
            if (held.isEmpty()) {
                continue;
            }
            if (fluid.isEmpty()) {
                fluid = held;
            } else if (!FluidCompatRegistry.areEquivalent(fluid, held)) {
                return;
            }
        }
        if (fluid.isEmpty()) {
            return;
        }
        tanks = orderedTanks(tanks, fluid.getFluid().getFluidType().isLighterThanAir(), true);
        int[] amountsBefore = new int[tanks.size()];
        for (int index = 0; index < tanks.size(); index++) {
            amountsBefore[index] = tanks.get(index).tank.getFluidAmount();
        }
        TileTank prev = null;
        for (TileTank tile : tanks) {
            if (prev != null) {
                FluidUtilBC.move(tile.tank, prev.tank);
            }
            prev = tile;
        }
        for (int index = 0; index < tanks.size(); index++) {
            TileTank tile = tanks.get(index);
            if (tile.tank.getFluidAmount() != amountsBefore[index]) {
                tile.onLocalTankContentsChanged();
            }
        }
    }

    
    
    @Override
	public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        int amountBefore = getCombinedFluidAmount();
        boolean didChange = FluidUtilBC.onTankActivated(player, worldPosition, hand, this);
        if (didChange && !player.level().isClientSide && amountBefore < getCombinedFluidAmount()) {
            AdvancementUtil.unlockAdvancement(player, ADVANCEMENT_STORE_FLUIDS);
        }
        if (!didChange && !player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, worldPosition);
        }
        return InteractionResult.SUCCESS;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new ContainerTank(id, inventory, this, ContainerLevelAccess.create(level, worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
	}


    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                writePayload(NET_FLUID_DELTA, buffer, side);
            } else if (id == NET_FLUID_DELTA) {
                smoothedTank.writeInit(buffer);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                readPayload(NET_FLUID_DELTA, buffer, side, ctx);
                smoothedTank.resetSmoothing(level);
            } else if (id == NET_FLUID_DELTA) {
                smoothedTank.handleMessage(level, buffer);
            }
        }
    }

    // IDebuggable

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("fluid = " + tank.getDebugString());
        smoothedTank.getDebugInfo(left, right, side);
    }

    // Rendering

    @OnlyIn(Dist.CLIENT)
    public FluidStackInterp getFluidForRender(float partialTicks) {
        return smoothedTank.getFluidForRender(partialTicks);
    }

    // Tank helper methods

    /** Tests to see if this tank can connect to the other one, in the given direction. BuildCraft itself only calls
     * with {@link Direction#UP} or {@link Direction#DOWN}, however addons are free to call with any of the other 4
     * non-null faces. (Although an addon calling from other faces must provide some way of transferring fluids around).
     * 
     * @param other The other tank.
     * @param direction The direction that the other tank is, from this tank.
     * @return True if this can connect, false otherwise. */
    public boolean canConnectTo(TileTank other, Direction direction) {
        return true;
    }

    /** Helper for {@link #canConnectTo(TileTank, Direction)} that only returns true if both tanks can connect to each
     * other.
     * 
     * @param from
     * @param to
     * @param direction The direction from the "from" tank, to the "to" tank, such that
     *            {@link Objects#equals(Object, Object) Objects.equals(}{@link TileTank#getPos()
     *            from.getPos()}.{@link BlockPos#offset(Direction) offset(direction)}, {@link TileTank#getPos()
     *            to.getPos()}) returns true.
     * @return True if both could connect, false otherwise. */
    public static boolean canTanksConnect(TileTank from, TileTank to, Direction direction) {
        return from.canConnectTo(to, direction) && to.canConnectTo(from, direction.getOpposite());
    }

    /** @return A list of all connected tanks around this block, ordered by position from bottom to top. */
    public List<TileTank> getConnectedTanks() {
        // double-ended queue rather than array list to avoid
        // the copy operation when we search downwards
        Deque<TileTank> tanks = new ArrayDeque<>();
        tanks.add(this);
        TileTank prevTank = this;
        while (true) {
            BlockEntity tileAbove = prevTank.getNeighbourTile(Direction.UP);
            if (!(tileAbove instanceof TileTank)) {
                break;
            }
            TileTank tankUp = (TileTank) tileAbove;
            if (tankUp != null && canTanksConnect(prevTank, tankUp, Direction.UP)) {
                tanks.addLast(tankUp);
            } else {
                break;
            }
            prevTank = tankUp;
        }
        prevTank = this;
        while (true) {
            BlockEntity tileBelow = prevTank.getNeighbourTile(Direction.DOWN);
            if (!(tileBelow instanceof TileTank)) {
                break;
            }
            TileTank tankBelow = (TileTank) tileBelow;
            if (tankBelow != null && canTanksConnect(prevTank, tankBelow, Direction.DOWN)) {
                tanks.addFirst(tankBelow);
            } else {
                break;
            }
            prevTank = tankBelow;
        }
        return new ArrayList<>(tanks);
    }

    private static List<TileTank> orderedTanks(List<TileTank> tanks, boolean gaseous, boolean filling) {
        int[] traversal = VerticalTankPolicy.traversal(tanks.size(), gaseous, filling);
        List<TileTank> ordered = new ArrayList<>(tanks.size());
        for (int index : traversal) {
            ordered.add(tanks.get(index));
        }
        return ordered;
    }

    private static FluidStack getCombinedFluid(List<TileTank> tanks) {
        FluidStack combined = FluidStack.EMPTY;
        long amount = 0;
        for (TileTank tile : tanks) {
            FluidStack local = tile.tank.getFluid();
            if (local.isEmpty() || local.getAmount() <= 0) {
                continue;
            }
            if (combined.isEmpty()) {
                combined = FluidCompatRegistry.canonicalize(local).copy();
            } else if (!FluidCompatRegistry.areEquivalent(combined, local)) {
                // A logical Forge tank cannot safely describe two incompatible fluids.
                return FluidStack.EMPTY;
            }
            amount = Math.min(Integer.MAX_VALUE, amount + local.getAmount());
        }
        if (!combined.isEmpty()) {
            combined.setAmount((int) amount);
        }
        return combined;
    }

    private int getCombinedFluidAmount() {
        return getCombinedFluid(getConnectedTanks()).getAmount();
    }

    // IFluidHandler -- a vertical stack is exposed as one logical tank.

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        return tankIndex == 0 ? getCombinedFluid(getConnectedTanks()) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        if (tankIndex != 0) {
            return 0;
        }
        long capacity = 0;
        for (TileTank tile : getConnectedTanks()) {
            capacity = Math.min(Integer.MAX_VALUE, capacity + Math.max(0, tile.tank.getCapacity()));
        }
        return (int) capacity;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        resource = FluidCompatRegistry.canonicalize(resource);
        if (resource.isEmpty() || resource.getAmount() <= 0) {
            return 0;
        }

        List<TileTank> tanks = getConnectedTanks();
        for (TileTank tile : tanks) {
            FluidStack current = tile.tank.getFluid();
            if (!current.isEmpty() && !FluidCompatRegistry.areEquivalent(current, resource)) {
                return 0;
            }
        }

        FluidStack remaining = resource.copy();
        int filled = 0;
        boolean gaseous = resource.getFluid().getFluidType().isLighterThanAir();
        for (TileTank tile : orderedTanks(tanks, gaseous, true)) {
            int accepted = tile.tank.fill(remaining, action);
            if (accepted <= 0) {
                continue;
            }
            filled += accepted;
            remaining.shrink(accepted);
            if (action.execute()) {
                tile.onLocalTankContentsChanged();
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return drain(stack -> true, maxDrain, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        resource = FluidCompatRegistry.canonicalize(resource);
        if (resource.isEmpty() || resource.getAmount() <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack requested = resource;
        return drain(
            stack -> FluidCompatRegistry.areEquivalent(requested, stack),
            requested.getAmount(),
            action
        );
    }

    // IFluidHandlerAdv

    @Override
    public FluidStack drain(IFluidFilter filter, int maxDrain, FluidAction action) {
        if (filter == null || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        List<TileTank> tanks = getConnectedTanks();
        FluidStack combined = getCombinedFluid(tanks);
        if (combined.isEmpty() || !filter.matches(combined)) {
            return FluidStack.EMPTY;
        }

        boolean gaseous = combined.getFluid().getFluidType().isLighterThanAir();
        FluidStack drainedTotal = combined.copy();
        drainedTotal.setAmount(0);
        for (TileTank tile : orderedTanks(tanks, gaseous, false)) {
            int remaining = maxDrain - drainedTotal.getAmount();
            if (remaining <= 0) {
                break;
            }
            FluidStack drained = tile.tank.drain(
                stack -> FluidCompatRegistry.areEquivalent(combined, stack),
                remaining,
                action
            );
            if (drained.isEmpty()) {
                continue;
            }
            drainedTotal.grow(drained.getAmount());
            if (action.execute()) {
                tile.onLocalTankContentsChanged();
            }
        }
        return drainedTotal.isEmpty() ? FluidStack.EMPTY : drainedTotal;
    }
    private void detectDirectTankMutation() {
        FluidStack current = tank.getFluid();
        boolean sameFluid = current.isEmpty() && lastObservedFluid.isEmpty()
            || !current.isEmpty() && !lastObservedFluid.isEmpty()
                && FluidCompatRegistry.areEquivalent(current, lastObservedFluid);
        if (!hasObservedLocalState
            || current.getAmount() != lastObservedFluid.getAmount()
            || tank.getCapacity() != lastObservedCapacity
            || !sameFluid) {
            onLocalTankContentsChanged();
        }
    }

    private void rememberLocalState() {
        FluidStack current = tank.getFluid();
        lastObservedFluid = current.isEmpty() ? FluidStack.EMPTY : current.copy();
        lastObservedCapacity = tank.getCapacity();
        hasObservedLocalState = true;
    }

    private void onLocalTankContentsChanged() {
        int previousLight = getFluidLight(lastObservedFluid);
        rememberLocalState();
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }

        int comparatorLevel = getComparatorLevel();
        if (comparatorLevel != lastComparatorLevel) {
            lastComparatorLevel = comparatorLevel;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (previousLight != getFluidLight(lastObservedFluid)) {
            level.getLightEngine().checkBlock(worldPosition);
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static int getFluidLight(FluidStack fluid) {
        return fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getLightLevel(fluid);
    }
	@Override
	public void addDrops(NonNullList<ItemStack> toDrop, int fortune) {
		super.addDrops(toDrop, fortune);
	}

    @Override
    public void saveAdditional(CompoundTag nbt) {
        // Keep the original 1.12.2/current format: {tanks:{tank:{FluidName,Amount,...}}}.
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        // Deserialize the base tile without its unbounded TankManager path, then read this tank defensively below.
        CompoundTag baseNbt = nbt.copy();
        baseNbt.remove("tanks");
        baseNbt.remove(FactoryTankContract.FLUID_NBT_KEY);
        super.load(baseNbt);

        boolean foundTankData = false;
        if (nbt.contains("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag tanks = nbt.getCompound("tanks");
            if (tanks.contains(tank.getTankName(), Tag.TAG_COMPOUND)) {
                readBoundedTank(tanks.getCompound(tank.getTankName()));
                foundTankData = true;
            } else if (!tanks.isEmpty()) {
                // Compatibility with the early API-layer build where "tanks" itself was a serialized FluidStack.
                readBoundedTank(tanks);
                foundTankData = true;
            }
        }
        if (!foundTankData && nbt.contains(FactoryTankContract.FLUID_NBT_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag legacyTank = nbt.getCompound(FactoryTankContract.FLUID_NBT_KEY);
            if (!legacyTank.isEmpty()) {
                // BuildCraft 7.99.0 and the first BuildCraft Neo tank prototype used a root-level "tank" tag.
                readBoundedTank(legacyTank);
                foundTankData = true;
            }
        }
        if (!foundTankData) {
            tank.setFluid(FluidStack.EMPTY);
        } else {
            boundLoadedFluid();
        }

        lastComparatorLevel = getComparatorLevel();
        lastObservedFluid = FluidStack.EMPTY;
        lastObservedCapacity = -1;
        hasObservedLocalState = false;
    }

    private void readBoundedTank(CompoundTag serializedTank) {
        try {
            tank.readFromNBT(serializedTank);
            boundLoadedFluid();
        } catch (RuntimeException malformedTankData) {
            tank.setFluid(FluidStack.EMPTY);
        }
    }

    private void boundLoadedFluid() {
        FluidStack loaded = tank.getFluid();
        if (loaded.isEmpty() || loaded.getAmount() <= 0) {
            tank.setFluid(FluidStack.EMPTY);
            return;
        }
        int amount = TankAmountPolicy.clampStoredAmount(loaded.getAmount(), tank.getCapacity());
        if (amount <= 0) {
            tank.setFluid(FluidStack.EMPTY);
        } else if (amount != loaded.getAmount()) {
            FluidStack bounded = loaded.copy();
            bounded.setAmount(amount);
            tank.setFluid(bounded);
        }
    }

    @Override
    public int getTanks() {
        // The vertical BuildCraft tank stack is exposed as one logical fluid handler tank. Returning one entry per
        // block would make external pipes/mods see the same combined contents and capacity multiple times.
        return 1;
    }

    @Override
    public boolean isFluidValid(int tankIndex, @NotNull FluidStack stack) {
        FluidStack normalized = FluidCompatRegistry.canonicalize(stack);
        if (tankIndex != 0 || normalized.isEmpty()) {
            return false;
        }
        boolean acceptedByAtLeastOneTank = false;
        for (TileTank tile : getConnectedTanks()) {
            FluidStack current = tile.tank.getFluid();
            if (!current.isEmpty() && !FluidCompatRegistry.areEquivalent(current, normalized)) {
                return false;
            }
            acceptedByAtLeastOneTank |= tile.tank.isFluidValid(normalized);
        }
        return acceptedByAtLeastOneTank;
    }

}
