/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.tile;

import java.io.IOException;
import java.util.EnumMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.menu.ContainerDynamoMJ;
import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.collect.OrderedEnumMap;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.lib.tile.item.StackInsertionFunction;
import buildcraft.neo.energy.DynamoStatePolicy;
import buildcraft.neo.energy.EnergyConversionPolicy;
import buildcraft.neo.energy.EnergyConversionPolicy.Transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

/**
 * Converts BuildCraft MJ into Forge Energy while preserving the complete legacy Dynamo contract.
 *
 * <p>The selected direction is the FE output. All other faces accept MJ. Both storage buffers and
 * every conversion are integer-bounded, so malformed NBT or capability responses cannot duplicate
 * or destroy more energy than was offered.</p>
 */
public class TileDynamoMJ extends TileBC_Neptune implements MenuProvider {
    public static final int MAX_RF = 10_000;
    public static final long MAX_MJ = 1_000 * MjAPI.MJ;

    public static final double HEAT_RATE = 0.06;
    public static final double COOLDOWN_RATE = 0.01;
    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;

    private static final int MAX_CHAIN_LENGTH = 3;

    private final EnumMap<Direction, DirectionalCapabilities> directionalCapabilities =
        new EnumMap<>(Direction.class);

    private long storedMj;
    private int currentRF;
    public final ItemHandlerSimple invUpgrades;

    protected double heat = MIN_HEAT;
    private float progress;
    private float lastProgress;
    private int progressPart;

    protected EnumPowerStage powerStage = EnumPowerStage.BLUE;
    @Nullable
    protected Direction currentDirection = Direction.UP;

    /** Forge Energy generated during the most recent server tick. */
    public long currentOutput;
    public boolean isRedstonePowered;
    protected boolean isPumping;

    public TileDynamoMJ(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.DYNAMO_MJ_TILE.get(), pos, state);
        for (Direction direction : Direction.values()) {
            directionalCapabilities.put(direction, new DirectionalCapabilities(direction));
        }
        invUpgrades = itemManager.addInvHandler(
            "upgrades",
            4,
            this::isValidUpgrade,
            StackInsertionFunction.getInsertionFunction(1),
            EnumAccess.NONE
        );
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        currentDirection = NBTUtilBC.readEnum(nbt.get("currentDirection"), Direction.class);
        if (currentDirection == null) {
            currentDirection = Direction.UP;
        }
        isRedstonePowered = nbt.getBoolean("isRedstonePowered");
        heat = DynamoStatePolicy.clampHeat(nbt.getDouble("heat"), MIN_HEAT, MAX_HEAT);
        progress = DynamoStatePolicy.clampProgress(nbt.getFloat("progress"));
        lastProgress = progress;
        progressPart = DynamoStatePolicy.clampProgressPart(nbt.getInt("progressPart"));
        currentRF = DynamoStatePolicy.clampStoredForgeEnergy(nbt.getInt("currentRF"), MAX_RF);
        storedMj = DynamoStatePolicy.clampStoredMicroJoules(
            nbt.getCompound("mj").getLong("stored"),
            MAX_MJ
        );
        powerStage = computePowerStage();
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("currentDirection", NBTUtilBC.writeEnum(getCurrentDirection()));
        nbt.putBoolean("isRedstonePowered", isRedstonePowered);
        nbt.putDouble("heat", heat);
        nbt.putFloat("progress", progress);
        nbt.putInt("progressPart", progressPart);
        nbt.putInt("currentRF", currentRF);
        CompoundTag mj = new CompoundTag();
        mj.putLong("stored", storedMj);
        nbt.put("mj", mj);
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx)
        throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side != LogicalSide.CLIENT) {
            return;
        }
        if (id == NET_RENDER_DATA) {
            isPumping = buffer.readBoolean();
            Direction newDirection = buffer.readEnum(Direction.class);
            EnumPowerStage newStage = buffer.readEnum(EnumPowerStage.class);
            boolean directionChanged = newDirection != getCurrentDirection();
            currentDirection = newDirection;
            powerStage = newStage;
            progress = DynamoStatePolicy.clampProgress(buffer.readFloat());
            lastProgress = progress;
            if (directionChanged) {
                refreshRenderState();
            }
        } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
            heat = DynamoStatePolicy.clampHeat(buffer.readFloat(), MIN_HEAT, MAX_HEAT);
            currentOutput = Math.max(0L, buffer.readLong());
            currentRF = DynamoStatePolicy.clampStoredForgeEnergy(buffer.readInt(), MAX_RF);
            storedMj = DynamoStatePolicy.clampStoredMicroJoules(buffer.readLong(), MAX_MJ);
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side != LogicalSide.SERVER) {
            return;
        }
        if (id == NET_RENDER_DATA) {
            buffer.writeBoolean(isPumping);
            buffer.writeEnum(getCurrentDirection());
            buffer.writeEnum(powerStage);
            buffer.writeFloat(progress);
        } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
            buffer.writeFloat((float) heat);
            buffer.writeLong(currentOutput);
            buffer.writeInt(currentRF);
            buffer.writeLong(storedMj);
        }
    }

    @Override
    public void update() {
        deltaManager.tick();
        if (cannotUpdate()) {
            return;
        }

        if (level.isClientSide) {
            tickClientAnimation();
            return;
        }

        boolean overheat = getPowerStage() == EnumPowerStage.OVERHEAT;
        if (!isRedstonePowered && currentRF > 0) {
            currentRF--;
        }

        updateHeatLevel();
        getPowerStage();
        tickServerAnimation();

        if (isRedstonePowered && isActive()) {
            sendPower();
        }

        if (!overheat) {
            burn();
        } else {
            currentOutput = 0;
        }
        markChunkDirty();
    }

    private void tickClientAnimation() {
        lastProgress = progress;
        if (isPumping) {
            progress += getPistonSpeed();
            if (progress >= 1.0F) {
                progress = 0.0F;
            }
        } else if (progress > 0.0F) {
            progress = Math.max(0.0F, progress - 0.01F);
        }
    }

    private void tickServerAnimation() {
        if (progressPart != 0) {
            progress += getPistonSpeed();
            if (progress > 0.5F && progressPart == 1) {
                progressPart = 2;
            } else if (progress >= 1.0F) {
                progress = 0.0F;
                progressPart = 0;
            }
        } else if (isRedstonePowered && isActive() && getPowerToExtract() > 0) {
            progressPart = 1;
            setPumping(true);
        } else {
            setPumping(false);
        }
    }

    protected void burn() {
        currentOutput = 0;
        if (!isRedstonePowered || storedMj <= 0) {
            return;
        }

        long conversionRate = BCEnergyConfig.microMjPerForgeEnergy;
        Transfer transfer = EnergyConversionPolicy.mjToForgeEnergy(
            storedMj,
            MAX_RF - currentRF,
            getForgeEnergyGenerationRate(conversionRate),
            conversionRate
        );
        if (transfer.forgeEnergy() <= 0) {
            return;
        }

        storedMj -= transfer.microJoules();
        currentRF += transfer.forgeEnergy();
        currentOutput = transfer.forgeEnergy();
        heat = Math.min(200.0, heat + HEAT_RATE);
        setChanged();
    }

    private int getForgeEnergyGenerationRate(long conversionRate) {
        long rate = getMjPerTick() / conversionRate;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, rate));
    }

    public int getForgeEnergyGenerationRate() {
        return getForgeEnergyGenerationRate(BCEnergyConfig.microMjPerForgeEnergy);
    }

    private int getPowerToExtract() {
        IEnergyStorage receiver = getReceiverToPower(getCurrentDirection());
        if (receiver == null || currentRF <= 0) {
            return 0;
        }
        int offered = Math.min(currentRF, maxPowerExtracted());
        return DynamoStatePolicy.acceptedForgeEnergy(offered, receiver.receiveEnergy(offered, true));
    }

    private void sendPower() {
        IEnergyStorage receiver = getReceiverToPower(getCurrentDirection());
        if (receiver == null || currentRF <= 0) {
            return;
        }
        int offered = Math.min(currentRF, maxPowerExtracted());
        int accepted = DynamoStatePolicy.acceptedForgeEnergy(
            offered,
            receiver.receiveEnergy(offered, false)
        );
        if (accepted > 0) {
            currentRF -= accepted;
            setChanged();
        }
    }

    @Nullable
    public IEnergyStorage getReceiverToPower(Direction side) {
        if (level == null || side == null) {
            return null;
        }

        TileDynamoMJ dynamo = this;
        BlockEntity next = null;
        for (int length = 0; length <= MAX_CHAIN_LENGTH; length++) {
            next = dynamo.getNeighbourTile(side);
            if (next == null) {
                return null;
            }
            if (next instanceof TileDynamoMJ nextDynamo) {
                if (nextDynamo.getCurrentDirection() != side) {
                    return null;
                }
                dynamo = nextDynamo;
            } else {
                break;
            }
        }

        if (next == null || next instanceof TileDynamoMJ) {
            return null;
        }
        IEnergyStorage receiver = next.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
        return receiver != null && receiver.canReceive() ? receiver : null;
    }

    public InteractionResult attemptRotation() {
        OrderedEnumMap<Direction> possible = VanillaRotationHandlers.ROTATE_FACING;
        Direction current = currentDirection;
        for (int i = 0; i < possible.getOrderLength(); i++) {
            current = possible.next(current);
            if (!isFacingReceiver(current)) {
                continue;
            }
            if (current == currentDirection) {
                return InteractionResult.FAIL;
            }
            currentDirection = current;
            setChanged();
            sendNetworkUpdate(NET_RENDER_DATA);
            refreshRenderState();
            if (level != null) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private boolean isFacingReceiver(Direction direction) {
        return getReceiverToPower(direction) != null;
    }

    public void rotateIfInvalid() {
        if (currentDirection != null && isFacingReceiver(currentDirection)) {
            return;
        }
        attemptRotation();
        if (currentDirection == null) {
            currentDirection = Direction.UP;
            setChanged();
            sendNetworkUpdate(NET_RENDER_DATA);
            refreshRenderState();
        }
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level != null) {
            isRedstonePowered = level.hasNeighborSignal(worldPosition);
        }
        currentDirection = null;
        rotateIfInvalid();
    }

    @Override
    public void neighbourBlockChanged(BlockState state, BlockPos neighbour, boolean moving) {
        super.onNeighbourBlockChanged(state, neighbour);
        updateRedstoneState();
    }

    @Override
    public void onNeighbourBlockChanged(BlockState state, BlockPos neighbour) {
        super.onNeighbourBlockChanged(state, neighbour);
        updateRedstoneState();
    }

    private void updateRedstoneState() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != isRedstonePowered) {
            isRedstonePowered = powered;
            setChanged();
        }
    }

    private void refreshRenderState() {
        if (level == null) {
            return;
        }
        requestModelDataUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private boolean isValidUpgrade(int slot, ItemStack stack) {
        return TileEngineRF.getUpgradeMicroMj(stack) > 0;
    }

    public long getMjPerTick() {
        long value = 4 * MjAPI.MJ;
        for (int slot = 0; slot < invUpgrades.getSlots(); slot++) {
            value += TileEngineRF.getUpgradeMicroMj(invUpgrades.getStackInSlot(slot));
        }
        return value;
    }

    protected EnumPowerStage computePowerStage() {
        double heatLevel = getHeatLevel();
        if (heatLevel < 0.25) return EnumPowerStage.BLUE;
        if (heatLevel < 0.5) return EnumPowerStage.GREEN;
        if (heatLevel < 0.75) return EnumPowerStage.YELLOW;
        if (heatLevel < 0.85) return EnumPowerStage.RED;
        return EnumPowerStage.OVERHEAT;
    }

    public EnumPowerStage getPowerStage() {
        if (level != null && !level.isClientSide) {
            EnumPowerStage newStage = computePowerStage();
            if (newStage != powerStage) {
                powerStage = newStage;
                sendNetworkUpdate(NET_RENDER_DATA);
            }
        }
        return powerStage;
    }

    public void updateHeatLevel() {
        heat = Math.max(MIN_HEAT, heat - COOLDOWN_RATE);
    }

    public double getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public double getHeat() {
        return heat;
    }

    public double getPistonSpeed() {
        return switch (getPowerStage()) {
            case BLUE -> 0.04;
            case GREEN -> 0.05;
            case YELLOW -> 0.06;
            case RED -> 0.07;
            default -> 0.0;
        };
    }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean pumping) {
        if (isPumping == pumping) {
            return;
        }
        isPumping = pumping;
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    public boolean isEngineOn() {
        return isPumping;
    }

    public int maxPowerExtracted() {
        return MAX_RF / 10;
    }

    public int getCurrentOutput() {
        return (int) Math.min(Integer.MAX_VALUE, currentOutput);
    }

    public int getCurrentRF() {
        return currentRF;
    }

    public long getMjStored() {
        return storedMj;
    }

    public Direction getCurrentDirection() {
        return currentDirection == null ? Direction.UP : currentDirection;
    }

    public float getProgressClient(float partialTicks) {
        float last = lastProgress;
        float now = progress;
        if (last > 0.5F && now < 0.5F) {
            now += 1.0F;
        }
        return (last * (1.0F - partialTicks) + now * partialTicks) % 1.0F;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult parentResult = super.onActivated(player, hand, hit);
        if (parentResult.consumesAction()) {
            return parentResult;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && (held.getItem() instanceof IToolWrench || held.getItem() instanceof IItemPipe)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerDynamoMJ(
            id,
            inventory,
            invUpgrades,
            ContainerLevelAccess.create(level, worldPosition)
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("tile.mjDynamo.name");
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
        @Nonnull Capability<T> capability,
        @Nullable Direction side
    ) {
        if (side != null) {
            DirectionalCapabilities directional = directionalCapabilities.get(side);
            if (capability == ForgeCapabilities.ENERGY && side == getCurrentDirection()) {
                return directional.forgeEnergy.cast();
            }
            if (side != getCurrentDirection()) {
                if (capability == MjAPI.CAP_CONNECTOR) {
                    return directional.mjConnector.cast();
                }
                if (capability == MjAPI.CAP_RECEIVER) {
                    return directional.mjReceiver.cast();
                }
                if (capability == MjAPI.CAP_READABLE) {
                    return directional.mjReadable.cast();
                }
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        directionalCapabilities.values().forEach(DirectionalCapabilities::invalidate);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        directionalCapabilities.values().forEach(DirectionalCapabilities::revive);
    }

    private final class DirectionalCapabilities {
        private final DynamoMjInput mjInput;
        private final DynamoForgeEnergyOutput energyOutput;
        private LazyOptional<IMjConnector> mjConnector;
        private LazyOptional<IMjReceiver> mjReceiver;
        private LazyOptional<IMjReadable> mjReadable;
        private LazyOptional<IEnergyStorage> forgeEnergy;

        private DirectionalCapabilities(Direction side) {
            mjInput = new DynamoMjInput(side);
            energyOutput = new DynamoForgeEnergyOutput(side);
            revive();
        }

        private void invalidate() {
            mjConnector.invalidate();
            mjReceiver.invalidate();
            mjReadable.invalidate();
            forgeEnergy.invalidate();
        }

        private void revive() {
            mjConnector = LazyOptional.of(() -> mjInput);
            mjReceiver = LazyOptional.of(() -> mjInput);
            mjReadable = LazyOptional.of(() -> mjInput);
            forgeEnergy = LazyOptional.of(() -> energyOutput);
        }
    }

    private final class DynamoMjInput implements IMjReceiver, IMjReadable {
        private final Direction side;

        private DynamoMjInput(Direction side) {
            this.side = side;
        }

        private boolean isInputFace() {
            return side != getCurrentDirection();
        }

        @Override
        public boolean canConnect(@Nonnull IMjConnector other) {
            return isInputFace();
        }

        @Override
        public boolean canReceive() {
            return isInputFace() && storedMj < MAX_MJ;
        }

        @Override
        public long getPowerRequested() {
            return canReceive() ? MAX_MJ - storedMj : 0;
        }

        @Override
        public long receivePower(long microJoules, FluidAction action) {
            if (microJoules <= 0) {
                return 0;
            }
            if (!canReceive()) {
                return microJoules;
            }
            long accepted = Math.min(microJoules, MAX_MJ - storedMj);
            if (action == FluidAction.EXECUTE && accepted > 0) {
                storedMj += accepted;
                setChanged();
            }
            return microJoules - accepted;
        }

        @Override
        public long getStored() {
            return storedMj;
        }

        @Override
        public long getCapacity() {
            return MAX_MJ;
        }
    }

    private final class DynamoForgeEnergyOutput implements IEnergyStorage {
        private final Direction side;

        private DynamoForgeEnergyOutput(Direction side) {
            this.side = side;
        }

        private boolean isOutputFace() {
            return side == getCurrentDirection();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!isOutputFace() || maxExtract <= 0) {
                return 0;
            }
            int extracted = Math.min(maxExtract, currentRF);
            if (!simulate && extracted > 0) {
                currentRF -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return isOutputFace() ? currentRF : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_RF;
        }

        @Override
        public boolean canExtract() {
            return isOutputFace();
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
