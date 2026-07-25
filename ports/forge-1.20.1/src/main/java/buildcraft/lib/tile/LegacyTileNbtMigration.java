/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.tile;

import net.minecraft.nbt.CompoundTag;

/** Compatibility migrations shared by all BuildCraft block entities. */
final class LegacyTileNbtMigration {
    private LegacyTileNbtMigration() {
    }

    /**
     * BuildCraft 7.99.0 stored a single managed tank directly under {@code tank}. Later versions moved it to
     * {@code tanks.tank}. Keep the released migration semantics so old-world fluid contents survive the port.
     */
    static void migrateLegacyTank(CompoundTag nbt) {
        CompoundTag modernTanks = nbt.getCompound("tanks");
        if (!modernTanks.isEmpty()) {
            return;
        }
        CompoundTag legacyTank = nbt.getCompound("tank");
        if (legacyTank.isEmpty()) {
            return;
        }
        CompoundTag tanks = modernTanks.copy();
        tanks.put("tank", legacyTank.copy());
        nbt.put("tanks", tanks);
    }
}
