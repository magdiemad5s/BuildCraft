/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.ToLongFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjPassiveProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.transport.pipe.IFlowPower;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeApi.PowerTransferInfo;
import buildcraft.api.transport.pipe.PipeEventPower;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.MathUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.AverageInt;
import buildcraft.lib.misc.data.AverageLong;
import buildcraft.neo.energy.PowerExtraction;
import buildcraft.neo.energy.PowerPipeBufferBounds;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.BCTransportConfig;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.LogicalSide;

public class PipeFlowPower extends PipeFlow implements IFlowPower, IDebuggable {
    private static final long DEFAULT_MAX_POWER = MjAPI.MJ * 10;
    public static final int NET_POWER_AMOUNTS = 2;

    public Vec3 clientDisplayFlowCentre = VecUtil.VEC_HALF;
    public Vec3 clientDisplayFlowCentreLast = VecUtil.VEC_HALF;
    public long clientLastDisplayTime = 0;

    private long maxPower = -1;
    private long powerLoss = -1;
    private long powerResistance = -1;
    private boolean disabled = false;

    private long currentWorldTime = Long.MIN_VALUE;

    private boolean isReceiver = false;
    private final EnumMap<Direction, Section> sections;

//    private final SafeTimeTracker tracker = new SafeTimeTracker(BCCoreConfig.networkUpdateRate);
//    private long[] transferQuery;

    public PipeFlowPower(IPipe pipe) {
        super(pipe);
        sections = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            sections.put(face, new Section(face));
        }
    }

    public PipeFlowPower(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        isReceiver = nbt.getBoolean("isReceiver");
        sections = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            sections.put(face, new Section(face));
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
        if (side == LogicalSide.SERVER) {
            if (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE) {
                for (Direction face : Direction.values()) {
                    Section s = sections.get(face);
                    buffer.writeInt(s.displayPower);
                    buffer.writeEnum(s.displayFlow);
                }
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side) throws IOException {
        super.readPayload(id, buffer, side);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE) {
                for (Direction face : Direction.values()) {
                    Section s = sections.get(face);
                    s.displayPower = buffer.readInt();
                    s.displayFlow = buffer.readEnum(EnumFlow.class);
                }
            }
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof PipeFlowPower;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        ensureConfigured();
        if (isReceiver) {
            LazyOptional<IMjPassiveProvider> provider = oTile.getCapability(MjAPI.CAP_PASSIVE_PROVIDER, face.getOpposite());
            if (provider.isPresent()) {
                return true;
            }
        }
        IMjConnector receiver = oTile.getCapability(MjAPI.CAP_CONNECTOR, face.getOpposite()).orElse(null);
        return receiver != null && receiver.canConnect(sections.get(face));
    }

    @Override
    public void reconfigure() {
        PipeEventPower.Configure configure = new PipeEventPower.Configure(pipe.getHolder(), this);
        boolean wasReceiver = isReceiver;
        boolean wasDisabled = disabled;
        PowerTransferInfo pti = PipeApi.getPowerTransferInfo(pipe.getDefinition());
        configure.setReceiver(pti.isReceiver);
        configure.setMaxPower(pti.transferPerTick);
        configure.setPowerLoss(pti.lossPerTick);
        configure.setPowerResistance(pti.resistancePerTick);
        pipe.getHolder().fireEvent(configure);
        isReceiver = configure.isReceiver();
        maxPower = configure.getMaxPower();
        disabled = configure.isTransferDisabled();
        if (maxPower <= 0) {
            maxPower = DEFAULT_MAX_POWER;
        }
        powerLoss = MathUtil.clamp(configure.getPowerLoss(), -1, maxPower);
        powerResistance = MathUtil.clamp(configure.getPowerResistance(), -1, MjAPI.MJ);

        if (powerLoss < 0) {
            if (powerResistance < 0) {
                // 1% resistance
                powerResistance = MjAPI.MJ / 100;
            }
            powerLoss = PowerTransferMath.multiplyDivideFloor(maxPower, powerResistance, MjAPI.MJ);
        } else if (powerResistance < 0) {
            powerResistance = PowerTransferMath.multiplyDivideFloor(powerLoss, MjAPI.MJ, maxPower);
        }
        if (wasReceiver != isReceiver || wasDisabled != disabled) {
            invalidateCapabilities();
            reviveCapabilities();
        }
    }

    @Override
    public long tryExtractPower(long maxExtracted, Direction from) {
        if (maxExtracted <= 0 || from == null) {
            return 0;
        }
        ensureConfigured();
        if (!isReceiver || disabled) {
            return 0;
        }
        step();

        BlockEntity tile = pipe.getConnectedTile(from);
        if (tile == null) {
            return 0;
        }
        IMjPassiveProvider provider =
            tile.getCapability(MjAPI.CAP_PASSIVE_PROVIDER, from.getOpposite()).orElse(null);
        if (provider == null) {
            return 0;
        }

        Section section = sections.get(from);
        long acceptable = PowerPipeBufferBounds.acceptable(
            maxExtracted, section.internalPower, section.internalNextPower, maxPower
        );
        if (acceptable <= 0) {
            return 0;
        }

        // Simulate first, then execute only the amount the provider advertised.
        // The shared buffer bound above guarantees that everything extracted
        // can be committed, so the transaction cannot duplicate power.
        long extracted = PowerExtraction.fromPassiveProvider(provider::extractPower, acceptable);
        long excess = section.receivePowerInternal(extracted, FluidAction.EXECUTE);
        return extracted - excess;
    }

    @Override
    public boolean onFlowActivate(Player player, BlockHitResult trace, Level level,
        EnumPipePart part) {
        return super.onFlowActivate(player, trace, level, part);
    }

    public Section getSection(Direction side) {
        return sections.get(side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        ensureConfigured();
        if (facing == null) {
            return LazyOptional.empty();
        } else if (capability == MjAPI.CAP_RECEIVER) {
            return isReceiver
                ? getCachedCapability(MjAPI.CAP_RECEIVER, facing, sections.get(facing)).cast()
                : LazyOptional.empty();
        } else if (capability == MjAPI.CAP_CONNECTOR) {
            return getCachedCapability(MjAPI.CAP_CONNECTOR, facing, sections.get(facing)).cast();
        } else {
            return LazyOptional.empty();
        }
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("maxPower = " + LocaleUtil.localizeMj(maxPower));
        left.add("isReceiver = " + isReceiver);
        left.add("disabled = " + disabled);
        left.add(
            "internalPower = " + arrayToString(s -> s.internalPower) + " <- " + arrayToString(s -> s.internalNextPower)
        );
        left.add("- powerQuery: " + arrayToString(s -> s.powerQuery) + " <- " + arrayToString(s -> s.nextPowerQuery));
        left.add(
            "- power: IN " + arrayToString(s -> s.debugPowerInput) + ", OUT " + arrayToString(s -> s.debugPowerOutput)
        );
        left.add("- power: OFFERED " + arrayToString(s -> s.debugPowerOffered));
    }

    private String arrayToString(ToLongFunction<Section> getter) {
        long[] arr = new long[6];
        for (Direction face : Direction.values()) {
            arr[face.ordinal()] = getter.applyAsLong(sections.get(face)) / MjAPI.MJ;
        }
        return Arrays.toString(arr);
    }

    @Override
    public void onTick() {
        ensureConfigured();
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            clientDisplayFlowCentreLast = clientDisplayFlowCentre;
            for (Direction face : Direction.values()) {
                Section s = sections.get(face);
                s.clientDisplayFlowLast = s.clientDisplayFlow;
                double diff = s.displayFlow.value * 2.4 * face.getAxisDirection().getStep();
                s.clientDisplayFlow += 16 + diff;
                s.clientDisplayFlow %= 16;

                double cVal = VecUtil.getValue(clientDisplayFlowCentre, face.getAxis());
                cVal += 16 + diff / 2;
                cVal %= 16;
                clientDisplayFlowCentre = VecUtil.replaceValue(clientDisplayFlowCentre, face.getAxis(), cVal);
            }
            return;
        }

        EnumFlow[] lastFlows = new EnumFlow[6];
        int[] lastDisplayPower = new int[6];

        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            int i = face.ordinal();
            lastFlows[i] = s.displayFlow;
            lastDisplayPower[i] = s.displayPower;
        }

        step();

        init();

        pullFromAdjacentProviders();

        for (Direction face : Direction.values()) {
            Section source = sections.get(face);
            if (source.internalPower <= 0) {
                continue;
            }

            BigInteger totalQuery = BigInteger.ZERO;
            for (Direction outputFace : Direction.values()) {
                if (face != outputFace) {
                    totalQuery = totalQuery.add(BigInteger.valueOf(sections.get(outputFace).powerQuery));
                }
            }

            boolean returnPower = false;
            if (totalQuery.signum() <= 0 && source.powerQuery > 0) {
                totalQuery = BigInteger.valueOf(source.powerQuery);
                returnPower = true;
            }
            if (totalQuery.signum() <= 0) {
                continue;
            }

            BigInteger remainingQuery = totalQuery;
            for (Direction outputFace : Direction.values()) {
                if ((face == outputFace && !returnPower) || source.internalPower <= 0) {
                    continue;
                }
                Section destination = sections.get(outputFace);
                if (destination.powerQuery <= 0) {
                    continue;
                }

                long grossBudget = BigInteger.valueOf(source.internalPower)
                    .multiply(BigInteger.valueOf(destination.powerQuery))
                    .divide(remainingQuery)
                    .min(BigInteger.valueOf(source.internalPower))
                    .longValue();
                remainingQuery = remainingQuery.subtract(BigInteger.valueOf(destination.powerQuery));
                TransferOutcome outcome = transferPower(outputFace, grossBudget);
                long consumed = Math.min(source.internalPower, outcome.consumed());
                source.internalPower -= consumed;
                destination.debugPowerOutput = PowerTransferMath.saturatingAdd(
                    destination.debugPowerOutput, outcome.delivered()
                );
                source.powerAverage.push(outcome.delivered());
                destination.powerAverage.push(outcome.delivered());
                if (outcome.delivered() > 0) {
                    source.displayFlow = EnumFlow.OUT;
                    destination.displayFlow = EnumFlow.IN;
                }
            }
        }
        // Render compute goes here
        for (Section s : sections.values()) {
            s.powerAverage.tick();
            double value = s.powerAverage.getAverage() / maxPower;
            value = Math.sqrt(value);
            s.displayPower = (int) (value * MjAPI.MJ);
        }

        // Compute local consumers requesting power. This includes both external tiles and internal pluggables such as
        // robot stations. A robot station blocks the pipe side, so it never appears as ConnectedType.TILE, but it still
        // needs to contribute a request to the power network just like the old 1.7 pluggable IEnergyReceiver did.
        for (Direction face : Direction.values()) {
            IMjReceiver recv = getPowerSink(face);
            if (recv != null && recv.canReceive()) {
                long requested = recv.getPowerRequested();
                if (requested > 0) {
                    requestPower(face, requested);
                }
            }
        }

        // Sum the amount of power requested on each side
        long[] transferQueryTemp = new long[6];
        for (Direction face : Direction.values()) {
            if (!pipe.isConnected(face)) {
                continue;
            }
            long query = 0;
            for (Direction face2 : Direction.values()) {
                if (face != face2) {
                    query = PowerTransferMath.saturatingAdd(query, sections.get(face2).powerQuery);
                }
            }
            transferQueryTemp[face.ordinal()] = query;
        }

        // Transfer requested power to neighbouring pipes
        for (Direction face : Direction.values()) {
            if (disabled) {
                continue;
            }
            if (transferQueryTemp[face.ordinal()] <= 0 || !pipe.isConnected(face)) {
                continue;
            }
            IPipe oPipe = pipe.getHolder().getNeighbourPipe(face);
            if (oPipe == Pipe.EMPTY || !(oPipe.getFlow() instanceof PipeFlowPower)) {
                continue;
            }
            PipeFlowPower oFlow = (PipeFlowPower) oPipe.getFlow();
            oFlow.requestPower(face.getOpposite(), transferQueryTemp[face.ordinal()]);
        }
        // Networking
        boolean didChange = false;
        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            int i = face.ordinal();
            if (lastFlows[i] != s.displayFlow || lastDisplayPower[i] != s.displayPower) {
                didChange = true;
                break;
            }
        }

        // if (tracker.markTimeIfDelay(pipe.getHolder().getPipeWorld())) {
        if (didChange) {
            sendPayload(NET_POWER_AMOUNTS);
        }

//        transferQuery = transferQueryTemp;
        // }
    }

    private void pullFromAdjacentProviders() {
        if (!isReceiver || disabled) {
            return;
        }
        for (Direction face : Direction.values()) {
            long requested = getPowerRequested(face);
            if (requested <= 0) {
                continue;
            }
            long extracted = tryExtractPower(Math.min(maxPower, requested), face);
            Section section = sections.get(face);
            section.debugPowerInput = PowerTransferMath.saturatingAdd(section.debugPowerInput, extracted);
        }
    }

    private TransferOutcome transferPower(Direction face, long grossBudget) {
        long netBudget = PowerTransferMath.netBudget(
            grossBudget, BCTransportConfig.lossMode, powerLoss, powerResistance
        );
        if (netBudget <= 0) {
            return TransferOutcome.NONE;
        }

        long leftover = netBudget;
        IPipe neighbour = pipe.getConnectedPipe(face);
        if (
            neighbour != null && neighbour != Pipe.EMPTY
                && neighbour.getFlow() instanceof PipeFlowPower other
                && neighbour.isConnected(face.getOpposite())
        ) {
            leftover = other.sections.get(face.getOpposite()).receivePowerInternal(netBudget);
        } else {
            IMjReceiver receiver = getPowerSink(face);
            if (receiver != null && receiver.canReceive()) {
                leftover = receiver.receivePower(netBudget, FluidAction.EXECUTE);
            }
        }
        leftover = MathUtil.clamp(leftover, 0, netBudget);
        long delivered = netBudget - leftover;
        long lost = PowerTransferMath.lossForAccepted(
            delivered, BCTransportConfig.lossMode, powerLoss, powerResistance
        );
        return new TransferOutcome(delivered, lost);
    }

    private record TransferOutcome(long delivered, long lost) {
        private static final TransferOutcome NONE = new TransferOutcome(0, 0);

        long consumed() {
            return PowerTransferMath.saturatingAdd(delivered, lost);
        }
    }

    private void step() {
        long now = pipe.getHolder().getPipeWorld().getGameTime();
        if (currentWorldTime != now) {
            currentWorldTime = now;
            sections.values().forEach(Section::step);
        }
    }

    private void ensureConfigured() {
        if (maxPower == -1) {
            reconfigure();
        }
    }

    private void init() {
        // TODO: use this for initialising the tile cache
    }

    private void requestPower(Direction from, long amount) {
        ensureConfigured();
        if (disabled || amount <= 0) {
            return;
        }
        step();

        Section s = sections.get(from);
        long requested = amount;
        if (pipe.getBehaviour() instanceof IPipeTransportPowerHook) {
            requested = ((IPipeTransportPowerHook) pipe.getBehaviour()).requestPower(from, amount);
        }
        if (requested > 0) {
            s.nextPowerQuery = Math.min(maxPower, PowerTransferMath.saturatingAdd(s.nextPowerQuery, requested));
        }
    }

    @Nullable
    private IMjReceiver getPowerSink(Direction face) {
        PipePluggable plug = pipe.getHolder().getPluggable(face);
        if (plug != null && plug != PipePluggable.EMPTY) {
            LazyOptional<IMjReceiver> pluggableReceiver = plug.getInternalCapability(MjAPI.CAP_RECEIVER);
            if (pluggableReceiver.isPresent()) {
                return pluggableReceiver.orElse(null);
            }
            if (plug.isBlocking()) {
                return null;
            }
        }

        if (pipe.getConnectedType(face) != ConnectedType.TILE) {
            return null;
        }

        LazyOptional<IMjReceiver> tileReceiver = pipe.getHolder().getCapabilityFromPipe(face, MjAPI.CAP_RECEIVER);
        return tileReceiver == null ? null : tileReceiver.orElse(null);
    }

    public long getPowerRequested(@Nullable Direction side) {
        ensureConfigured();
        if (disabled) {
            return 0;
        }
        long req = 0;
        for (Direction face : Direction.values()) {
            if (side == null || face != side) {
                req = PowerTransferMath.saturatingAdd(req, sections.get(face).powerQuery);
            }
        }
        return req;
    }

    public double getMaxTransferForRender(float partialTicks) {
        ensureConfigured();
//        if (true) 
        	return maxPower / (double) MjAPI.MJ;
/*        double max = 0;
        for (Section s : sections.values()) {
            double value = s.displayPower / (double) MjAPI.MJ;
            // value = MathUtil.interp(partialTicks, value, value);
            max = Math.max(max, value);
        }
        return max;*/
    }

    public class Section implements IMjReceiver, IMjRedstoneReceiver {
        public final Direction side;

        public final AverageInt clientDisplayAverage = new AverageInt(10);
        public double clientDisplayFlow, clientDisplayFlowLast;

        /** Range: 0 to {@link MjAPI#MJ} */
        public int displayPower;
        public EnumFlow displayFlow = EnumFlow.STATIONARY;
        public long nextPowerQuery;
        public long internalNextPower;
        public final AverageLong powerAverage = new AverageLong(10);

        long powerQuery;
        long internalPower;

        /** Debugging fields */
        long debugPowerInput, debugPowerOutput, debugPowerOffered;

        public Section(Direction side) {
            this.side = side;
            clientDisplayFlow = (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 7 : 1) / 8.0;
        }

        void step() {
            powerQuery = nextPowerQuery;
            nextPowerQuery = 0;

            internalPower = PowerTransferMath.saturatingAdd(internalPower, internalNextPower);
            internalNextPower = 0;
        }

        @Override
        public boolean canConnect(@Nonnull IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return PipeFlowPower.this.getPowerRequested(side);
        }

        long receivePowerInternal(long sent) {
            return receivePowerInternal(sent, FluidAction.EXECUTE);
        }

        long receivePowerInternal(long sent, FluidAction action) {
            PipeFlowPower.this.ensureConfigured();
            if (disabled || sent <= 0) {
                return sent;
            }

            long accepted = PowerPipeBufferBounds.acceptable(sent, internalPower, internalNextPower, maxPower);
            if (action == FluidAction.EXECUTE && accepted > 0) {
                PipeFlowPower.this.step();
                accepted = PowerPipeBufferBounds.acceptable(sent, internalPower, internalNextPower, maxPower);
                debugPowerOffered = PowerTransferMath.saturatingAdd(debugPowerOffered, accepted);
                internalNextPower = PowerTransferMath.saturatingAdd(internalNextPower, accepted);
            }
            return sent - accepted;
        }

        @Override
        public long receivePower(long microJoules, FluidAction action) {
            PipeFlowPower.this.ensureConfigured();
            if (isReceiver && !disabled) {
                return this.receivePowerInternal(microJoules, action);
            }
            return microJoules;
        }

        @Override
        public boolean canReceive() {
            PipeFlowPower.this.ensureConfigured();
            return isReceiver && !disabled;
        }
    }

    public enum EnumFlow {
        IN(-1),
        OUT(1),
        STATIONARY(0);

        public final int value;

        private EnumFlow(int value) {
            this.value = value;
        }
    }
}
