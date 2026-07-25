/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */

package buildcraft.transport.pipe.flow;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.transport.pipe.IFlowRedstoneFlux;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeApi.RedstoneFluxTransferInfo;
import buildcraft.api.transport.pipe.PipeEventRedstoneFlux;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.AverageInt;
import buildcraft.neo.energy.PowerPipeBufferBounds;
import buildcraft.neo.energy.PowerExtraction;
import buildcraft.transport.pipe.Pipe;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.LogicalSide;

import org.jetbrains.annotations.NotNull;

/**
 * Forge Energy implementation of BuildCraft 8's optional Redstone Flux pipe
 * family. Energy remains integer FE throughout this flow; it is deliberately
 * separate from the microjoule power-pipe network.
 */
public class PipeFlowRedstoneFlux extends PipeFlow implements IFlowRedstoneFlux, IDebuggable {
    private static final int DEFAULT_MAX_POWER = 100;
    public static final int NET_POWER_AMOUNTS = 2;

    public Vec3 clientDisplayFlowCentre = VecUtil.VEC_HALF;
    public Vec3 clientDisplayFlowCentreLast = VecUtil.VEC_HALF;

    private int maxPower = -1;
    private boolean disabled;
    private boolean isReceiver;
    private long currentWorldTime = Long.MIN_VALUE;

    private final EnumMap<Direction, Section> sections = new EnumMap<>(Direction.class);

    public PipeFlowRedstoneFlux(IPipe pipe) {
        super(pipe);
        initializeSections();
    }

    public PipeFlowRedstoneFlux(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        isReceiver = nbt.getBoolean("isReceiver");
        initializeSections();
    }

    private void initializeSections() {
        for (Direction face : Direction.values()) {
            Section section = new Section(face);
            sections.put(face, section);
        }
    }

    @Override
    public CompoundTag writeToNbt() {
        ensureConfigured();
        CompoundTag nbt = super.writeToNbt();
        nbt.putBoolean("isReceiver", isReceiver);
        return nbt;
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE)) {
            for (Direction face : Direction.values()) {
                Section section = sections.get(face);
                buffer.writeInt(section.displayPower);
                buffer.writeEnum(section.displayFlow);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side) throws IOException {
        super.readPayload(id, buffer, side);
        if (side == LogicalSide.CLIENT && (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE)) {
            for (Direction face : Direction.values()) {
                Section section = sections.get(face);
                section.displayPower = buffer.readInt();
                section.displayFlow = buffer.readEnum(EnumFlow.class);
            }
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof PipeFlowRedstoneFlux;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity otherTile) {
        ensureConfigured();
        return otherTile.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).isPresent();
    }

    @Override
    public void reconfigure() {
        boolean wasReceiver = isReceiver;
        boolean wasDisabled = disabled;
        RedstoneFluxTransferInfo info = PipeApi.getRfTransferInfo(pipe.getDefinition());
        PipeEventRedstoneFlux.Configure configure =
            new PipeEventRedstoneFlux.Configure(pipe.getHolder(), this);
        configure.setReceiver(info.isReceiver);
        configure.setMaxPower(info.transferPerTick);
        pipe.getHolder().fireEvent(configure);

        isReceiver = configure.isReceiver();
        disabled = configure.isTransferDisabled() || configure.getMaxPower() <= 0;
        maxPower = disabled ? 1 : configure.getMaxPower();
        if (wasReceiver != isReceiver || wasDisabled != disabled) {
            invalidateCapabilities();
            reviveCapabilities();
        }
    }

    private void ensureConfigured() {
        if (maxPower < 0) {
            reconfigure();
        }
    }

    @Override
    public int tryExtractPower(int maximumForgeEnergy, Direction from) {
        if (maximumForgeEnergy <= 0 || from == null) {
            return 0;
        }
        ensureConfigured();
        if (!isReceiver || disabled) {
            return 0;
        }

        BlockEntity tile = pipe.getConnectedTile(from);
        if (tile == null) {
            return 0;
        }
        IEnergyStorage storage =
            tile.getCapability(ForgeCapabilities.ENERGY, from.getOpposite()).orElse(null);
        if (storage == null || !storage.canExtract()) {
            return 0;
        }

        step();
        Section section = sections.get(from);
        int acceptable = section.maximumAcceptable(maximumForgeEnergy);
        if (acceptable <= 0) {
            return 0;
        }

        int extracted = PowerExtraction.fromFeStorage(storage::extractEnergy, acceptable);
        return extracted - section.receivePowerInternal(extracted, false);
    }

    private static int clampExternal(int amount, int maximum) {
        return Math.max(0, Math.min(maximum, amount));
    }

    @Override
    public boolean onFlowActivate(
        Player player, BlockHitResult trace, Level level, EnumPipePart part
    ) {
        return super.onFlowActivate(player, trace, level, part);
    }

    public Section getSection(Direction side) {
        return sections.get(side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
        @Nonnull Capability<T> capability, Direction facing
    ) {
        if (facing == null || capability != ForgeCapabilities.ENERGY) {
            return LazyOptional.empty();
        }
        ensureConfigured();
        return isReceiver && !disabled
            ? getCachedCapability(ForgeCapabilities.ENERGY, facing, sections.get(facing)).cast()
            : LazyOptional.empty();
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("maxPower = " + maxPower + " FE/t");
        left.add("isReceiver = " + isReceiver);
        left.add("disabled = " + disabled);
        left.add(
            "internalPower = " + arrayToString(s -> s.internalPower)
                + " <- " + arrayToString(s -> s.internalNextPower)
        );
        left.add(
            "- powerQuery: " + arrayToString(s -> s.powerQuery)
                + " <- " + arrayToString(s -> s.nextPowerQuery)
        );
        left.add(
            "- power: IN " + arrayToString(s -> s.debugPowerInput)
                + ", OUT " + arrayToString(s -> s.debugPowerOutput)
        );
    }

    private String arrayToString(ToIntFunction<Section> getter) {
        int[] values = new int[Direction.values().length];
        for (Direction face : Direction.values()) {
            values[face.ordinal()] = getter.applyAsInt(sections.get(face));
        }
        return Arrays.toString(values);
    }

    @Override
    public void onTick() {
        ensureConfigured();
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            tickClientAnimation();
            return;
        }

        EnumFlow[] lastFlows = new EnumFlow[Direction.values().length];
        int[] lastDisplayPower = new int[Direction.values().length];
        for (Direction face : Direction.values()) {
            Section section = sections.get(face);
            lastFlows[face.ordinal()] = section.displayFlow;
            lastDisplayPower[face.ordinal()] = section.displayPower;
        }

        step();
        if (!disabled) {
            pullFromAdjacentSources();
            distributeStoredEnergy();
            requestLocalSinkEnergy();
            propagateRequests();
        }
        updateDisplayPower();

        boolean changed = false;
        for (Direction face : Direction.values()) {
            Section section = sections.get(face);
            if (
                lastFlows[face.ordinal()] != section.displayFlow
                    || lastDisplayPower[face.ordinal()] != section.displayPower
            ) {
                changed = true;
                break;
            }
        }
        if (changed) {
            sendPayload(NET_POWER_AMOUNTS);
        }
    }

    private void tickClientAnimation() {
        clientDisplayFlowCentreLast = clientDisplayFlowCentre;
        for (Direction face : Direction.values()) {
            Section section = sections.get(face);
            section.clientDisplayFlowLast = section.clientDisplayFlow;
            double direction = face.getAxisDirection().getStep();
            double difference = section.displayFlow.value * 2.4 * direction;
            section.clientDisplayFlow = (section.clientDisplayFlow + 16 + difference) % 16;

            double centre = VecUtil.getValue(clientDisplayFlowCentre, face.getAxis());
            centre = (centre + 16 + difference / 2) % 16;
            clientDisplayFlowCentre =
                VecUtil.replaceValue(clientDisplayFlowCentre, face.getAxis(), centre);
        }
    }

    private void pullFromAdjacentSources() {
        if (!isReceiver) {
            return;
        }
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) != ConnectedType.TILE) {
                continue;
            }
            int requested = getPowerRequested(face);
            if (requested > 0) {
                int extracted = tryExtractPower(Math.min(maxPower, requested), face);
                sections.get(face).debugPowerInput += extracted;
            }
        }
    }

    private void distributeStoredEnergy() {
        for (Direction inputFace : Direction.values()) {
            Section input = sections.get(inputFace);
            if (input.internalPower <= 0) {
                continue;
            }

            long totalQuery = 0;
            for (Direction outputFace : Direction.values()) {
                if (outputFace != inputFace) {
                    totalQuery += sections.get(outputFace).powerQuery;
                }
            }
            boolean returnPower = false;
            if (totalQuery <= 0 && input.powerQuery > 0) {
                totalQuery = input.powerQuery;
                returnPower = true;
            }
            if (totalQuery <= 0) {
                continue;
            }

            long remainingQuery = totalQuery;
            for (Direction outputFace : Direction.values()) {
                if ((outputFace == inputFace && !returnPower) || input.internalPower <= 0) {
                    continue;
                }
                Section output = sections.get(outputFace);
                if (output.powerQuery <= 0) {
                    continue;
                }

                int offered = (int) Math.min(
                    input.internalPower * (long) output.powerQuery / remainingQuery,
                    input.internalPower
                );
                remainingQuery -= output.powerQuery;
                if (offered <= 0) {
                    continue;
                }

                int unused = offerToOutput(outputFace, offered);
                int used = offered - unused;
                input.internalPower -= used;
                input.debugPowerOutput += used;
                output.debugPowerInput += used;
                input.powerAverage.push(used);
                output.powerAverage.push(used);
                if (used > 0) {
                    input.displayFlow = EnumFlow.OUT;
                    output.displayFlow = EnumFlow.IN;
                }
            }
        }
    }

    private int offerToOutput(Direction face, int offered) {
        IPipe neighbour = pipe.getConnectedPipe(face);
        if (
            neighbour != null
                && neighbour != Pipe.EMPTY
                && neighbour.getFlow() instanceof PipeFlowRedstoneFlux other
                && neighbour.isConnected(face.getOpposite())
        ) {
            return other.sections.get(face.getOpposite()).receivePowerInternal(offered, false);
        }

        LazyOptional<IEnergyStorage> capability =
            pipe.getHolder().getCapabilityFromPipe(face, ForgeCapabilities.ENERGY);
        IEnergyStorage receiver = capability == null ? null : capability.orElse(null);
        if (receiver == null || !receiver.canReceive()) {
            return offered;
        }
        int accepted = clampExternal(receiver.receiveEnergy(offered, false), offered);
        return offered - accepted;
    }

    private void requestLocalSinkEnergy() {
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) != ConnectedType.TILE) {
                continue;
            }
            LazyOptional<IEnergyStorage> capability =
                pipe.getHolder().getCapabilityFromPipe(face, ForgeCapabilities.ENERGY);
            IEnergyStorage receiver = capability == null ? null : capability.orElse(null);
            if (receiver != null && receiver.canReceive()) {
                int requested = clampExternal(receiver.receiveEnergy(maxPower, true), maxPower);
                requestPower(face, requested);
            }
        }
    }

    private void propagateRequests() {
        int[] outgoing = new int[Direction.values().length];
        for (Direction face : Direction.values()) {
            if (!pipe.isConnected(face)) {
                continue;
            }
            long query = 0;
            for (Direction other : Direction.values()) {
                if (face != other) {
                    query += sections.get(other).powerQuery;
                }
            }
            outgoing[face.ordinal()] = (int) Math.min(query, maxPower);
        }

        for (Direction face : Direction.values()) {
            if (outgoing[face.ordinal()] <= 0 || !pipe.isConnected(face)) {
                continue;
            }
            IPipe neighbour = pipe.getHolder().getNeighbourPipe(face);
            if (neighbour != null && neighbour.getFlow() instanceof PipeFlowRedstoneFlux other) {
                other.requestPower(face.getOpposite(), outgoing[face.ordinal()]);
            }
        }
    }

    private void updateDisplayPower() {
        for (Section section : sections.values()) {
            section.powerAverage.tick();
            double ratio = disabled ? 0 : section.powerAverage.getAverage() / (double) maxPower;
            section.displayPower = (int) (Math.sqrt(Math.max(0, ratio)) * MjAPI.MJ);
            if (section.displayPower == 0) {
                section.displayFlow = EnumFlow.STATIONARY;
            }
        }
    }

    private void step() {
        long now = pipe.getHolder().getPipeWorld().getGameTime();
        if (currentWorldTime == now) {
            return;
        }
        currentWorldTime = now;
        sections.values().forEach(Section::step);
    }

    private void requestPower(Direction from, int amount) {
        if (disabled || amount <= 0) {
            return;
        }
        step();
        Section section = sections.get(from);
        section.nextPowerQuery =
            (int) Math.min(maxPower, (long) section.nextPowerQuery + amount);
    }

    public int getPowerRequested(@Nullable Direction side) {
        if (disabled) {
            return 0;
        }
        long requested = 0;
        for (Direction face : Direction.values()) {
            if (side == null || face != side) {
                requested += sections.get(face).powerQuery;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, requested);
    }

    public double getMaxTransferForRender(float partialTicks) {
        return maxPower;
    }

    public final class Section implements IEnergyStorage {
        public final Direction side;
        public final AverageInt powerAverage = new AverageInt(10);

        public double clientDisplayFlow;
        public double clientDisplayFlowLast;
        public int displayPower;
        public EnumFlow displayFlow = EnumFlow.STATIONARY;
        public int nextPowerQuery;
        public int internalNextPower;

        int powerQuery;
        int internalPower;
        int debugPowerInput;
        int debugPowerOutput;

        private Section(Direction side) {
            this.side = side;
            clientDisplayFlow =
                (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 7 : 1) / 8.0;
        }

        private void step() {
            powerQuery = nextPowerQuery;
            nextPowerQuery = 0;

            internalPower += internalNextPower;
            internalNextPower = 0;
        }

        private int maximumAcceptable(int offered) {
            long accepted = PowerPipeBufferBounds.acceptable(
                offered, internalPower, internalNextPower, maxPower
            );
            return (int) accepted;
        }

        private int receivePowerInternal(int offered, boolean simulate) {
            ensureConfigured();
            if (disabled || offered <= 0) {
                return offered;
            }
            step();
            int accepted = maximumAcceptable(offered);
            if (!simulate && accepted > 0) {
                internalNextPower += accepted;
            }
            return offered - accepted;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            ensureConfigured();
            if (!isReceiver || disabled || maxReceive <= 0) {
                return 0;
            }
            return maxReceive - receivePowerInternal(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return internalPower + internalNextPower;
        }

        @Override
        public int getMaxEnergyStored() {
            ensureConfigured();
            return disabled ? 0 : maxPower;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            ensureConfigured();
            return isReceiver && !disabled;
        }
    }

    public enum EnumFlow {
        IN(-1),
        OUT(1),
        STATIONARY(0);

        public final int value;

        EnumFlow(int value) {
            this.value = value;
        }
    }
}
