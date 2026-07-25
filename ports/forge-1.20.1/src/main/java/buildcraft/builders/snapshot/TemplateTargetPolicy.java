/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.snapshot;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Target classification for a template/filler cell.
 *
 * <p>Templates describe whether a cell should contain a block, rather than the exact block state. Replaceable
 * states such as water, lava, snow layers, and tall grass therefore do not satisfy a filled cell. They are valid
 * direct placement targets and are replaced through the normal {@code ItemStack#useOn} path, which lets Forge fire
 * and enforce its block-place event.</p>
 */
final class TemplateTargetPolicy {
    private TemplateTargetPolicy() {
    }

    static boolean canPlaceInto(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    static boolean countsAsFilled(BlockState state) {
        return !state.isAir() && !state.canBeReplaced();
    }
}
