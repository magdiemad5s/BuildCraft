/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.factory.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class FactoryNbtSanitizerTest {
    @Test
    void fluidAmountsAreClampedInCurrentAndLegacyTankShapes() {
        CompoundTag direct = new CompoundTag();
        direct.putLong("Amount", Long.MAX_VALUE);
        FactoryNbtSanitizer.clampFluidAmount(direct, "input", 2_000);
        assertEquals(2_000, direct.getInt("Amount"));

        CompoundTag nested = new CompoundTag();
        CompoundTag nestedFluid = new CompoundTag();
        nestedFluid.putInt("Amount", -1);
        nested.put("input", nestedFluid);
        FactoryNbtSanitizer.clampFluidAmount(nested, "input", 2_000);
        assertEquals(0, nested.getCompound("input").getInt("Amount"));
    }

    @Test
    void batteryStateIsCopiedAndBounded() {
        CompoundTag overfilled = new CompoundTag();
        overfilled.putLong("stored", Long.MAX_VALUE);
        CompoundTag sanitized = FactoryNbtSanitizer.sanitizeBattery(overfilled, 1_024_000_000L);

        assertEquals(Long.MAX_VALUE, overfilled.getLong("stored"));
        assertEquals(1_024_000_000L, sanitized.getLong("stored"));

        CompoundTag negative = new CompoundTag();
        negative.putLong("stored", Long.MIN_VALUE);
        assertEquals(0, FactoryNbtSanitizer.sanitizeBattery(negative, 1_024L).getLong("stored"));
    }

    @Test
    void machineProgressCannotReachAnOverflowingOrAlreadyCompletedState() {
        assertEquals(0, FactoryNbtSanitizer.clampRecipeProgress(Long.MIN_VALUE, 32));
        assertEquals(31, FactoryNbtSanitizer.clampRecipeProgress(Long.MAX_VALUE, 32));
        assertEquals(0, FactoryNbtSanitizer.clampRecipeProgress(10, 0));

        assertEquals(0, FactoryNbtSanitizer.clampProgress(Integer.MIN_VALUE, 100_000));
        assertEquals(99_999, FactoryNbtSanitizer.clampProgress(Integer.MAX_VALUE, 100_000));
    }
}
