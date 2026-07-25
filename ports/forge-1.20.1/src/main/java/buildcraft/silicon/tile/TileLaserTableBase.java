/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.tiles.TilesAPI;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.data.AverageLong;
import buildcraft.lib.recipe.IngredientStackAllocation;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public abstract class TileLaserTableBase extends TileBC_Neptune implements ILaserTarget, IDebuggable {
    private static final long MJ_FLOW_ROUND = MjAPI.MJ / 10;
    private final AverageLong avgPower = new AverageLong(120);
    public long avgPowerClient;
    public long targetClient;
    public long power;

    protected TileLaserTableBase(BlockEntityType<? extends TileLaserTableBase> type, BlockPos pos, BlockState state) {
    	super(type, pos, state);
        caps.addCapabilityInstance(TilesAPI.CAP_HAS_WORK, () -> getTarget() > 0, EnumPipePart.VALUES);
    }

    public abstract long getTarget();

    public long getGuiTarget() {
        return level != null && level.isClientSide ? targetClient : getTarget();
    }

    @Override
    public long getRequiredLaserPower() {
        return Math.max(0, getTarget() - power);
    }

    @Override
    public long receiveLaserPower(long microJoules) {
        if (microJoules <= 0) {
            return microJoules;
        }
        long received = Math.min(microJoules, getRequiredLaserPower());
        if (received > 0) {
            power += received;
            avgPower.push(received);
            markChunkDirty();
        }
        return microJoules - received;
    }

    @Override
    public boolean isInvalidTarget() {
        return isRemoved();
    }

    @Override
    public void update() {
        avgPower.tick();
        if (level.isClientSide) {
            return;
        }

        if (getTarget() <= 0) {
            power = 0;
            avgPower.clear();
        }
    }
    
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
        nbt.putLong("power", power);
	}
	
    @Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		power = Math.max(0, nbt.getLong("power"));
	}


    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_GUI_TICK) {
                buffer.writeLong(power);
                buffer.writeLong(getTarget());
                double avg = avgPower.getAverage();
                long pwrAvg = Math.round(avg);
                long div = pwrAvg / MJ_FLOW_ROUND;
                long mod = pwrAvg % MJ_FLOW_ROUND;
                int mj = (int) (div) + ((mod > MJ_FLOW_ROUND / 2) ? 1 : 0);
                buffer.writeInt(mj);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_GUI_TICK) {
                power = buffer.readLong();
                targetClient = buffer.readLong();
                avgPowerClient = buffer.readInt() * MJ_FLOW_ROUND;
            }
        }
    }


    protected long getDirectPowerRequested() {
        return Math.max(0, getTarget() - power);
    }

    protected long receiveDirectPower(long microJoules, FluidAction action) {
        if (microJoules <= 0) {
            return microJoules;
        }
        long accepted = Math.min(getDirectPowerRequested(), microJoules);
        if (accepted > 0 && action.execute()) {
            power += accepted;
            avgPower.push(accepted);
            markChunkDirty();
        }
        return microJoules - accepted;
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("power - " + LocaleUtil.localizeMj(power));
        left.add("target - " + LocaleUtil.localizeMj(getTarget()));
    }

    protected boolean extract(ItemHandlerSimple inv, Collection<IngredientStack> items, boolean simulate,
        boolean precise) {
        int[] consumption = IngredientStackAllocation.find(
            inv.getSlots(),
            inv::getStackInSlot,
            items,
            precise
        );
        if (consumption == null) {
            return false;
        }
        if (!simulate) {
            for (int slot = 0; slot < consumption.length; slot++) {
                if (consumption[slot] <= 0) {
                    continue;
                }
                ItemStack remaining = inv.getStackInSlot(slot).copy();
                remaining.shrink(consumption[slot]);
                inv.setStackInSlot(slot, remaining);
            }
        }
        return true;
    }
}
