package buildcraft.silicon.plug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

class FacadeSerializationRotationTest {
    private FacadeBlockStateInfo previousDefault;
    private FacadeBlockStateInfo previousNorth;
    private FacadeBlockStateInfo previousEast;
    private FacadeBlockStateInfo northInfo;
    private FacadeBlockStateInfo eastInfo;
    private BlockState northState;
    private BlockState eastState;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @BeforeEach
    void registerDirectionalStates() {
        northState = Blocks.WHITE_GLAZED_TERRACOTTA.defaultBlockState()
            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        eastState = northState.rotate(Rotation.CLOCKWISE_90);
        ItemStack required = new ItemStack(Blocks.WHITE_GLAZED_TERRACOTTA);
        northInfo = new FacadeBlockStateInfo(northState, required, ImmutableSet.of());
        eastInfo = new FacadeBlockStateInfo(eastState, required, ImmutableSet.of());

        previousDefault = FacadeStateManager.defaultState;
        previousNorth = FacadeStateManager.resolvableFacadeStates.put(northState, northInfo);
        previousEast = FacadeStateManager.resolvableFacadeStates.put(eastState, eastInfo);
        FacadeStateManager.defaultState = northInfo;
    }

    @AfterEach
    void restoreDirectionalStates() {
        restore(northState, previousNorth);
        restore(eastState, previousEast);
        FacadeStateManager.defaultState = previousDefault;
    }

    private static void restore(BlockState state, FacadeBlockStateInfo previous) {
        if (previous == null) {
            FacadeStateManager.resolvableFacadeStates.remove(state);
        } else {
            FacadeStateManager.resolvableFacadeStates.put(state, previous);
        }
    }

    @Test
    void blockLevelPassValidatesEveryStateIndependently() {
        InteractionResultHolder<String> blockResult = new InteractionResultHolder<>(InteractionResult.PASS, "");

        InteractionResultHolder<String> fullCube = FacadeStateManager.validateFacadeState(
            blockResult, Blocks.STONE.defaultBlockState()
        );
        InteractionResultHolder<String> partialCube = FacadeStateManager.validateFacadeState(
            blockResult, Blocks.SNOW.defaultBlockState()
        );

        assertEquals(InteractionResult.SUCCESS, fullCube.getResult());
        assertEquals(InteractionResult.FAIL, partialCube.getResult());
    }

    @Test
    void networkStateCountMustBeBetweenOneAndSeventeen() {
        assertFalse(FacadeInstance.isValidStateCount(0));
        assertTrue(FacadeInstance.isValidStateCount(1));
        assertTrue(FacadeInstance.isValidStateCount(17));
        assertFalse(FacadeInstance.isValidStateCount(18));

        for (int invalid : new int[] { -1, 0, 18, Short.MAX_VALUE }) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeBoolean(false);
                buffer.writeShort(invalid);
                assertThrows(DecoderException.class, () -> FacadeInstance.readFromBuffer(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void oversizedSavedFacadeIsTruncatedAndKeepsHollowFlag() {
        CompoundTag nbt = new CompoundTag();
        ListTag states = new ListTag();
        for (int index = 0; index < 18; index++) {
            states.add(new FacadePhasedState(northInfo, null).writeToNbt());
        }
        nbt.put("states", states);
        nbt.putBoolean("isHollow", true);

        FacadeInstance loaded = FacadeInstance.readFromNbt(nbt);

        assertEquals(17, loaded.phasedStates.length);
        assertTrue(loaded.isHollow);
    }

    @Test
    void emptyAndMalformedSavedStateFallBackWithoutLosingHollowFlag() {
        CompoundTag emptyFacade = new CompoundTag();
        emptyFacade.putBoolean("isHollow", true);
        FacadeInstance emptyLoaded = FacadeInstance.readFromNbt(emptyFacade);
        assertSame(northInfo, emptyLoaded.phasedStates[0].stateInfo);
        assertTrue(emptyLoaded.isHollow);

        CompoundTag malformed = new CompoundTag();
        CompoundTag malformedState = new CompoundTag();
        malformedState.putString("Name", "not a valid resource location %%%");
        malformed.put("state", malformedState);
        assertSame(northInfo, FacadePhasedState.readFromNbt(malformed).stateInfo);
    }

    @Test
    void rotationPreservesColourHollowFlagAndExactBlockOrientation() {
        FacadeInstance original = new FacadeInstance(
            new FacadePhasedState[] { new FacadePhasedState(northInfo, DyeColor.PURPLE) },
            true
        );

        FacadeInstance rotated = original.rotate(Rotation.CLOCKWISE_90);

        assertSame(eastInfo, rotated.phasedStates[0].stateInfo);
        assertEquals(DyeColor.PURPLE, rotated.phasedStates[0].activeColour);
        assertTrue(rotated.isHollow);
        assertEquals(Direction.EAST, rotated.phasedStates[0].stateInfo.state
            .getValue(HorizontalDirectionalBlock.FACING));
    }

    @Test
    void constructorRejectsMoreThanSeventeenStates() {
        FacadePhasedState[] states = new FacadePhasedState[18];
        java.util.Arrays.fill(states, new FacadePhasedState(northInfo, null));
        assertThrows(IllegalArgumentException.class, () -> new FacadeInstance(states, false));
    }
}
