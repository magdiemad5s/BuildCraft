package buildcraft.silicon.plug;

import javax.annotation.Nullable;

import buildcraft.api.core.BCLog;
import buildcraft.api.facades.IFacadePhasedState;
import buildcraft.api.facades.IFacadeState;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.NBTUtilBC;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class FacadePhasedState implements IFacadePhasedState {
    public final FacadeBlockStateInfo stateInfo;

    @Nullable
    public final DyeColor activeColour;

    public FacadePhasedState(FacadeBlockStateInfo stateInfo, DyeColor activeColour) {
        this.stateInfo = stateInfo;
        this.activeColour = activeColour;
    }

    public static FacadePhasedState readFromNbt(CompoundTag nbt) {
        FacadeBlockStateInfo stateInfo = FacadeStateManager.defaultState;
        if (nbt.contains("state")) {
            try {
                BlockState blockState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("state"));
                stateInfo = FacadeStateManager.getInfoForState(blockState);
                if (stateInfo == null) {
                    stateInfo = FacadeStateManager.defaultState;
                }
            } catch (RuntimeException exception) {
                BCLog.logger.warn("Failed to read a saved facade state; using the default facade", exception);
                stateInfo = FacadeStateManager.defaultState;
            }
        }
        DyeColor colour = NBTUtilBC.readEnum(nbt.get("activeColour"), DyeColor.class);
        return new FacadePhasedState(stateInfo, colour);
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        try {
            nbt.put("state", NbtUtils.writeBlockState(stateInfo.state));
        } catch (Throwable t) {
            throw new IllegalStateException("Writing facade block state"//
                + "\n\tState = " + stateInfo//
                + "\n\tBlock = " + stateInfo.state.getBlock() + "\n\tBlock Class = "
                + stateInfo.state.getBlock().getClass(), t);
        }
        if (activeColour != null) {
            nbt.put("activeColour", NBTUtilBC.writeEnum(activeColour));
        }
        return nbt;
    }

    public static FacadePhasedState readFromBuffer(FriendlyByteBuf buf) {
        BlockState state = MessageUtil.readBlockState(buf);
        DyeColor colour = MessageUtil.readEnumOrNull(buf, DyeColor.class);
        FacadeBlockStateInfo info = FacadeStateManager.getInfoForState(state);
        if (info == null) {
            info = FacadeStateManager.defaultState;
        }
        return new FacadePhasedState(info, colour);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        try {
            MessageUtil.writeBlockState(buf, stateInfo.state);
        } catch (Throwable t) {
            throw new IllegalStateException("Writing facade block state\n\tState = " + stateInfo.state, t);
        }
        MessageUtil.writeEnumOrNull(buf, activeColour);
    }

    public FacadePhasedState withColour(DyeColor colour) {
        return new FacadePhasedState(stateInfo, colour);
    }

    public FacadePhasedState rotate(Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return this;
        }
        try {
            BlockState rotatedState = stateInfo.state.rotate(rotation);
            FacadeBlockStateInfo rotatedInfo = FacadeStateManager.getInfoForState(rotatedState);
            if (rotatedInfo != null) {
                return new FacadePhasedState(rotatedInfo, activeColour);
            }
            BCLog.logger.warn("No valid rotated facade state for {}; retaining its original state", stateInfo.state);
        } catch (RuntimeException exception) {
            BCLog.logger.warn("Failed to rotate facade state {}; retaining its original state", stateInfo.state, exception);
        }
        return this;
    }

    public boolean isSideSolid(Direction side) {
        return stateInfo.isSideSolid[side.ordinal()];
    }

/*    public BlockFaceShape getBlockFaceShape(Direction side) {
        return stateInfo.blockFaceShape[side.ordinal()];
    }*/

    @Override
    public String toString() {
        return (activeColour == null ? "" : activeColour + " ") + getState();
    }

    // IFacadePhasedState

    @Override
    public IFacadeState getState() {
        return stateInfo;
    }

    @Override
    public DyeColor getActiveColor() {
        return activeColour;
    }
}
