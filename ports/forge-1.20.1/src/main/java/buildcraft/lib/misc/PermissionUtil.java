/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import buildcraft.api.core.IPlayerOwned;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Future class for checking to see if a given player can actually do something. */
public class PermissionUtil {
    // Just object types so that we can change these later without needing to change callers
    public static final Object PERM_VIEW = "buildcraft.view";
    public static final Object PERM_EDIT = "buildcraft.edit";
    public static final Object PERM_DESTROY = "buildcraft.destroy";

    private static final int MAX_INTERACT_DISTANCE = 8;
    private static final int MAX_INTERACT_DISTANCE_SQ = MAX_INTERACT_DISTANCE * MAX_INTERACT_DISTANCE;

    public static boolean hasPermission(Object type, PermissionBlock attempting, PermissionBlock target) {
        if (!isUsableTarget(target)) {
            return false;
        }
        if (!isOwnerRestricted(type)) {
            return true;
        }

        Ownership targetOwnership = resolveOwnership(target);
        if (targetOwnership.state == OwnershipState.OWNERLESS) {
            return true;
        }
        Ownership attemptingOwnership = resolveOwnership(attempting);
        return targetOwnership.state == OwnershipState.VALID
            && attemptingOwnership.state == OwnershipState.VALID
            && profilesMatch(attemptingOwnership.profile, targetOwnership.profile);
    }

    public static boolean hasPermission(Object type, GameProfile attempting, PermissionBlock target) {
        if (!isUsableTarget(target)) {
            return false;
        }
        if (!isOwnerRestricted(type)) {
            return true;
        }

        Ownership targetOwnership = resolveOwnership(target);
        return targetOwnership.state == OwnershipState.OWNERLESS
            || targetOwnership.state == OwnershipState.VALID && profilesMatch(attempting, targetOwnership.profile);
    }

    public static boolean hasPermission(Object type, Player attempting, PermissionBlock target) {
        if (attempting == null) {
            return false;
        }
        return hasPlayerPermission(
            type,
            attempting.getGameProfile(),
            attempting.blockPosition(),
            attempting.isCreative() || attempting.hasPermissions(2),
            target
        );
    }

    static boolean hasPlayerPermission(
        Object type,
        GameProfile attempting,
        BlockPos attemptingPos,
        boolean privileged,
        PermissionBlock target
    ) {
        if (!isUsableTarget(target) || attemptingPos == null
            || attemptingPos.distSqr(target.pos) > MAX_INTERACT_DISTANCE_SQ) {
            return false;
        }
        if (!isOwnerRestricted(type)) {
            return true;
        }

        Ownership targetOwnership = resolveOwnership(target);
        if (targetOwnership.state == OwnershipState.OWNERLESS || privileged) {
            return true;
        }
        return targetOwnership.state == OwnershipState.VALID
            && profilesMatch(attempting, targetOwnership.profile);
    }

    private static boolean isOwnerRestricted(Object type) {
        return PERM_DESTROY.equals(type) || PERM_EDIT.equals(type);
    }

    private static boolean isUsableTarget(PermissionBlock target) {
        return target != null && target.pos != null;
    }

    private static Ownership resolveOwnership(PermissionBlock block) {
        if (block == null) {
            return Ownership.INVALID;
        }
        if (block.owned == null) {
            return Ownership.OWNERLESS;
        }
        try {
            GameProfile profile = block.owned.getOwner();
            if (isNullProfile(profile)) {
                return Ownership.OWNERLESS;
            }
            return isUsableProfile(profile) ? new Ownership(OwnershipState.VALID, profile) : Ownership.INVALID;
        } catch (RuntimeException ignored) {
            return Ownership.INVALID;
        }
    }

    private static boolean profilesMatch(GameProfile attempting, GameProfile target) {
        return isUsableProfile(attempting)
            && isUsableProfile(target)
            && attempting.getId().equals(target.getId());
    }

    private static boolean isUsableProfile(GameProfile profile) {
        return profile != null && profile.isComplete() && !isNullProfile(profile);
    }

    private static boolean isNullProfile(GameProfile profile) {
        return profile != null
            && profile.getId() != null
            && profile.getId().equals(FakePlayerProvider.NULL_PROFILE.getId());
    }

    public static PermissionBlock createFrom(Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        IPlayerOwned owned = null;

        if (tile instanceof IPlayerOwned) {
            owned = (IPlayerOwned) tile;
        }

        return new PermissionBlock(owned, pos);
    }

    private enum OwnershipState {
        OWNERLESS,
        VALID,
        INVALID
    }

    private record Ownership(OwnershipState state, GameProfile profile) {
        private static final Ownership OWNERLESS = new Ownership(OwnershipState.OWNERLESS, null);
        private static final Ownership INVALID = new Ownership(OwnershipState.INVALID, null);
    }

    public static class PermissionBlock {
        public final IPlayerOwned owned;
        public final BlockPos pos;

        public PermissionBlock(IPlayerOwned owned, BlockPos pos) {
            this.owned = owned;
            this.pos = pos;
        }
    }
}
