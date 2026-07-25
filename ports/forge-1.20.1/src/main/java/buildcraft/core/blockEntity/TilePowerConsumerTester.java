/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.core.blockEntity;

import java.util.List;

import javax.annotation.Nonnull;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.core.BCCoreBlocks;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Legacy MJ diagnostic sink. Its NBT keys are intentionally unchanged:
 * {@code last}, {@code nt}, {@code lt}, and {@code total}.
 */
public class TilePowerConsumerTester extends TileBC_Neptune implements IMjReceiver, IDebuggable {
    private static final long MAX_REQUEST = 100_000L * MjAPI.MJ;

    private long lastReceived;
    private long nextTickReceived;
    private long lastTickReceived;
    private long totalReceived;

    public TilePowerConsumerTester(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.POWER_TESTER_TILE.get(), pos, state);
        caps.addProvider(new MjCapabilityHelper(this));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastReceived = nonNegative(tag.getLong("last"));
        nextTickReceived = nonNegative(tag.getLong("nt"));
        lastTickReceived = nonNegative(tag.getLong("lt"));
        totalReceived = nonNegative(tag.getLong("total"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putLong("last", lastReceived);
        tag.putLong("nt", nextTickReceived);
        tag.putLong("lt", lastTickReceived);
        tag.putLong("total", totalReceived);
        super.saveAdditional(tag);
    }

    @Override
    public void update() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (lastTickReceived != nextTickReceived || nextTickReceived != 0) {
            lastTickReceived = nextTickReceived;
            nextTickReceived = 0;
            setChanged();
        }
    }

    @Override
    public boolean canConnect(@Nonnull IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return MAX_REQUEST;
    }

    @Override
    public long receivePower(long microJoules, FluidAction action) {
        if (microJoules <= 0) {
            return microJoules;
        }
        if (action.execute()) {
            lastReceived = microJoules;
            nextTickReceived = saturatedAdd(nextTickReceived, microJoules);
            totalReceived = saturatedAdd(totalReceived, microJoules);
            setChanged();
        }
        return 0;
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("");
        left.add("Last received = " + LocaleUtil.localizeMj(lastReceived));
        left.add("Tick received = " + LocaleUtil.localizeMj(lastTickReceived));
        left.add("Total received = " + LocaleUtil.localizeMj(totalReceived));
    }

    public long getLastReceived() {
        return lastReceived;
    }

    public long getLastTickReceived() {
        return lastTickReceived;
    }

    public long getTotalReceived() {
        return totalReceived;
    }

    private static long nonNegative(long value) {
        return Math.max(0, value);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}