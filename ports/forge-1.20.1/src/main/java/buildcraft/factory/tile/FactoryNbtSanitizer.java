/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.factory.tile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Bounds persistence values before they enter legacy machine implementations.
 *
 * <p>Released BuildCraft worlds use the same keys retained by this port, so normalization is deliberately applied to
 * values rather than by replacing or renaming the serialized structure.</p>
 */
final class FactoryNbtSanitizer {
    private static final String FLUID_AMOUNT_KEY = "Amount";
    private static final String BATTERY_STORED_KEY = "stored";

    private FactoryNbtSanitizer() {
    }

    static void clampFluidAmount(CompoundTag tankTag, String legacyTankName, int capacity) {
        CompoundTag fluidTag = tankTag;
        if (tankTag.contains(legacyTankName, Tag.TAG_COMPOUND)) {
            fluidTag = tankTag.getCompound(legacyTankName);
        }
        if (fluidTag.contains(FLUID_AMOUNT_KEY, Tag.TAG_ANY_NUMERIC)) {
            fluidTag.putInt(FLUID_AMOUNT_KEY, (int) clamp(fluidTag.getLong(FLUID_AMOUNT_KEY), 0, capacity));
        }
    }

    static CompoundTag sanitizeBattery(CompoundTag batteryTag, long capacity) {
        CompoundTag sanitized = batteryTag.copy();
        sanitized.putLong(BATTERY_STORED_KEY, clamp(sanitized.getLong(BATTERY_STORED_KEY), 0, capacity));
        return sanitized;
    }

    static long clampRecipeProgress(long progress, long recipePowerRequired) {
        if (recipePowerRequired <= 0) {
            return 0;
        }
        return clamp(progress, 0, recipePowerRequired - 1);
    }

    static int clampProgress(int progress, int targetExclusive) {
        if (targetExclusive <= 0) {
            return 0;
        }
        return (int) clamp(progress, 0, targetExclusive - 1L);
    }

    static int clampAmount(int amount, int capacity) {
        return (int) clamp(amount, 0, capacity);
    }

    private static long clamp(long value, long minimum, long maximum) {
        long safeMaximum = Math.max(minimum, maximum);
        return Math.max(minimum, Math.min(value, safeMaximum));
    }
}
