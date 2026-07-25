/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0
 */
package buildcraft.lib.misc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import buildcraft.lib.misc.PermissionUtil.PermissionBlock;
import com.mojang.authlib.GameProfile;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class PermissionUtilTest {
    private static final BlockPos TARGET_POS = new BlockPos(10, 64, 10);
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void ownerlessTargetsRemainUsableForRestrictedActions() {
        GameProfile arbitrary = profile(OTHER_ID, "Other");

        assertTrue(PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, arbitrary, ownerlessTarget()));
        assertTrue(PermissionUtil.hasPermission(
            PermissionUtil.PERM_DESTROY,
            arbitrary,
            ownedTarget(FakePlayerProvider.NULL_PROFILE)
        ));
    }

    @Test
    void ownedEditAndDestroyRequireTheSameStableOwnerId() {
        GameProfile originalName = profile(OWNER_ID, "OriginalName");
        GameProfile renamedOwner = profile(OWNER_ID, "RenamedOwner");
        GameProfile other = profile(OTHER_ID, "Other");
        PermissionBlock target = ownedTarget(originalName);

        assertTrue(PermissionUtil.hasPermission(new String("buildcraft.edit"), renamedOwner, target));
        assertTrue(PermissionUtil.hasPermission(
            PermissionUtil.PERM_DESTROY,
            ownedTarget(renamedOwner),
            target
        ));
        assertFalse(PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, other, target));
        assertFalse(PermissionUtil.hasPermission(
            PermissionUtil.PERM_DESTROY,
            ownedTarget(other),
            target
        ));
        assertTrue(PermissionUtil.hasPermission(PermissionUtil.PERM_VIEW, other, target));
    }

    @Test
    void malformedOwnershipNeverGrantsAnOrdinaryRestrictedAction() {
        GameProfile valid = profile(OWNER_ID, "Owner");
        GameProfile missingId = new GameProfile(null, "Owner");

        assertFalse(PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, valid, ownedTarget(null)));
        assertFalse(PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, valid, ownedTarget(missingId)));
        assertFalse(PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, missingId, ownedTarget(valid)));
        assertFalse(PermissionUtil.hasPermission(
            PermissionUtil.PERM_DESTROY,
            new PermissionBlock(() -> {
                throw new IllegalStateException("corrupt owner data");
            }, TARGET_POS),
            ownedTarget(valid)
        ));
    }

    @Test
    void playerDistanceIsEnforcedBeforePrivilegeOverride() {
        GameProfile owner = profile(OWNER_ID, "Owner");
        GameProfile other = profile(OTHER_ID, "Other");
        PermissionBlock target = ownedTarget(owner);

        assertFalse(PermissionUtil.hasPlayerPermission(
            PermissionUtil.PERM_EDIT,
            other,
            TARGET_POS.offset(9, 0, 0),
            true,
            target
        ));
        assertFalse(PermissionUtil.hasPlayerPermission(
            PermissionUtil.PERM_EDIT,
            other,
            TARGET_POS.offset(8, 0, 0),
            false,
            target
        ));
        assertTrue(PermissionUtil.hasPlayerPermission(
            PermissionUtil.PERM_EDIT,
            other,
            TARGET_POS.offset(8, 0, 0),
            true,
            target
        ));
    }

    private static PermissionBlock ownerlessTarget() {
        return new PermissionBlock(null, TARGET_POS);
    }

    private static PermissionBlock ownedTarget(GameProfile profile) {
        return new PermissionBlock(() -> profile, TARGET_POS);
    }

    private static GameProfile profile(UUID id, String name) {
        return new GameProfile(id, name);
    }
}
