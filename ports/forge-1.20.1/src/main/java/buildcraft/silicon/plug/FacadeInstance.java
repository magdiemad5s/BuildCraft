package buildcraft.silicon.plug;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import buildcraft.api.core.BCLog;
import buildcraft.api.facades.FacadeType;
import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadePhasedState;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Rotation;

public class FacadeInstance implements IFacade {
    static final int MAX_PHASED_STATES = 17;
    public final FacadePhasedState[] phasedStates;
    public final FacadeType type;
    public final boolean isHollow;

    public FacadeInstance(FacadePhasedState[] phasedStates, boolean isHollow) {
        if (phasedStates == null) throw new NullPointerException("phasedStates");
        if (phasedStates.length == 0) throw new IllegalArgumentException("phasedStates.length was 0");
        // Maximum of 17 states - 16 for each colour, 1 for no colour
        if (phasedStates.length > MAX_PHASED_STATES) {
            throw new IllegalArgumentException("phasedStates.length was > " + MAX_PHASED_STATES);
        }
        for (int index = 0; index < phasedStates.length; index++) {
            Objects.requireNonNull(phasedStates[index], "phasedStates[" + index + "]");
        }
        this.phasedStates = phasedStates;
        if (phasedStates.length == 1) {
            type = FacadeType.Basic;
        } else {
            type = FacadeType.Phased;
        }
        this.isHollow = isHollow;
    }

    public static FacadeInstance createSingle(FacadeBlockStateInfo info, boolean isHollow) {
        return new FacadeInstance(new FacadePhasedState[] { new FacadePhasedState(info, null) }, isHollow);
    }

    public static FacadeInstance readFromNbt(CompoundTag nbt) {
        ListTag list = nbt.getList("states", Tag.TAG_COMPOUND);
        boolean hollow = nbt.getBoolean("isHollow");
        if (list.isEmpty()) {
            return FacadeInstance.createSingle(FacadeStateManager.defaultState, hollow);
        }
        int count = Math.min(list.size(), MAX_PHASED_STATES);
        if (list.size() > MAX_PHASED_STATES) {
            BCLog.logger.warn(
                "Saved facade has {} states; retaining the first {}",
                list.size(), MAX_PHASED_STATES
            );
        }
        FacadePhasedState[] states = new FacadePhasedState[count];
        for (int i = 0; i < count; i++) {
            states[i] = FacadePhasedState.readFromNbt(list.getCompound(i));
        }
        return new FacadeInstance(states, hollow);
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (FacadePhasedState state : phasedStates) {
            list.add(state.writeToNbt());
        }
        nbt.put("states", list);
        nbt.putBoolean("isHollow", isHollow);
        return nbt;
    }

    public static FacadeInstance readFromBuffer(FriendlyByteBuf buf) {
        boolean isHollow = buf.readBoolean();
        int count = buf.readShort();
        if (!isValidStateCount(count)) {
            throw new DecoderException("Invalid facade state count " + count);
        }
        FacadePhasedState[] states = new FacadePhasedState[count];
        for (int i = 0; i < count; i++) {
            states[i] = FacadePhasedState.readFromBuffer(buf);
        }
        return new FacadeInstance(states, isHollow);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(isHollow);
        buf.writeShort(phasedStates.length);
        for (FacadePhasedState phasedState : phasedStates) {
            phasedState.writeToBuffer(buf);
        }
    }

    static boolean isValidStateCount(int count) {
        return count >= 1 && count <= MAX_PHASED_STATES;
    }

    public boolean canAddColour(DyeColor colour) {
        for (FacadePhasedState state : phasedStates) {
            if (state.activeColour == colour) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public FacadeInstance withState(FacadePhasedState state) {
        if (phasedStates.length < MAX_PHASED_STATES && canAddColour(state.activeColour)) {
            FacadePhasedState[] newStates = Arrays.copyOf(phasedStates, phasedStates.length + 1);
            newStates[newStates.length - 1] = state;
            return new FacadeInstance(newStates, isHollow);
        } else {
            return null;
        }
    }

    public FacadePhasedState getCurrentStateForStack() {
        int count = phasedStates.length;
        if (count == 1) {
            return phasedStates[0];
        } else {
            int now = (int) (System.currentTimeMillis() % 100_000);
            return phasedStates[(now / 500) % count];
        }
    }

    public FacadeInstance withSwappedIsHollow() {
        return new FacadeInstance(phasedStates, !isHollow);
    }

    public FacadeInstance rotate(Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return this;
        }
        FacadePhasedState[] rotated = new FacadePhasedState[phasedStates.length];
        for (int index = 0; index < phasedStates.length; index++) {
            rotated[index] = phasedStates[index].rotate(rotation);
        }
        return new FacadeInstance(rotated, isHollow);
    }

    public boolean areAllStatesSolid(Direction side) {
        for (FacadePhasedState state : phasedStates) {
            if (!state.isSideSolid(side)) {
                return false;
            }
        }
        return true;
    }

/*    public BlockFaceShape getBlockFaceShape(Direction side) {
        if (isHollow()) {
            return BlockFaceShape.UNDEFINED;
        }
        switch (type) {
            case Basic:
                return phasedStates[0].getBlockFaceShape(side);
            case Phased: {
                BlockFaceShape shape = null;
                for (FacadePhasedState state : phasedStates) {
                    if (shape == null) {
                        shape = state.getBlockFaceShape(side);
                    } else if (shape != state.getBlockFaceShape(side)) {
                        return BlockFaceShape.UNDEFINED;
                    }
                }
                if (shape == null) {
                    return BlockFaceShape.UNDEFINED;
                }
                return shape;
            }
            default:
                throw new IllegalStateException("Unknown FacadeType " + type);
        }
    }*/

    // IFacade

    @Override
    public FacadeType getType() {
        return type;
    }

    @Override
    public boolean isHollow() {
        return isHollow;
    }

    @Override
    public IFacadePhasedState[] getPhasedStates() {
        return phasedStates;
    }
}
