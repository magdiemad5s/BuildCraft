/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.snapshot;

import javax.annotation.Nullable;

import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.core.IFakePlayerProvider;
import buildcraft.lib.misc.FakePlayerProvider;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Places schematic blocks through Forge's normal cancellable placement event.
 *
 * <p>The snapshot is deliberately captured before the proposed state is installed. Forge 47's player placement event
 * reads the proposed state from the live world, so the event must run after {@link Level#setBlock}; cancellation then
 * restores both the replaced state and its original block-entity NBT.</p>
 */
final class SchematicPlacementUtil {
    private SchematicPlacementUtil() {
    }

    static boolean place(
        Level level,
        BlockPos blockPos,
        BlockState proposedState,
        int flags,
        @Nullable GameProfile owner,
        BlockPos actorPos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.setBlock(blockPos, proposedState, flags);
        }

        BlockSnapshot replaced = BlockSnapshot.create(serverLevel.dimension(), serverLevel, blockPos, flags);
        if (!serverLevel.setBlock(blockPos, proposedState, flags)) {
            return false;
        }

        FakePlayer actor = getActor(serverLevel, owner, actorPos);
        Direction placementDirection = directionFrom(actorPos, blockPos);
        try {
            if (ForgeEventFactory.onBlockPlace(actor, replaced, placementDirection)) {
                replaced.restore(true, true);
                return false;
            }
            return true;
        } catch (RuntimeException | Error exception) {
            replaced.restore(true, true);
            throw exception;
        }
    }

    private static FakePlayer getActor(ServerLevel level, @Nullable GameProfile owner, BlockPos actorPos) {
        IFakePlayerProvider provider = BuildCraftAPI.fakePlayerProvider;
        if (provider == null) {
            provider = FakePlayerProvider.INSTANCE;
        }
        GameProfile effectiveOwner = owner == null ? FakePlayerProvider.NULL_PROFILE : owner;
        return provider.getFakePlayer(level, effectiveOwner, actorPos);
    }

    private static Direction directionFrom(BlockPos actorPos, BlockPos blockPos) {
        int dx = blockPos.getX() - actorPos.getX();
        int dy = blockPos.getY() - actorPos.getY();
        int dz = blockPos.getZ() - actorPos.getZ();
        if (dx == 0 && dy == 0 && dz == 0) {
            return Direction.UP;
        }
        return Direction.getNearest(dx, dy, dz);
    }
}
