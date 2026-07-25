/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Preserves the metadata-based subtype contract used by BuildCraft 8 items.
 *
 * <p>Minecraft 1.20.1 no longer exposes item metadata as a first-class API, but
 * legacy BuildCraft worlds and recipes distinguish several variants through the
 * numeric {@code Damage} value. These helpers intentionally keep that exact key
 * and ordinal meaning instead of inventing separate registry entries.</p>
 */
public final class LegacyItemMetadata {
    public static final String TAG_METADATA = "Damage";

    private LegacyItemMetadata() {
    }

    public static int get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_METADATA, Tag.TAG_ANY_NUMERIC)
            ? tag.getInt(TAG_METADATA)
            : 0;
    }

    public static void set(ItemStack stack, int metadata) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putInt(TAG_METADATA, Math.max(0, metadata));
    }

    public static ItemStack create(Item item, int count, int metadata) {
        ItemStack stack = new ItemStack(item, count);
        set(stack, metadata);
        return stack;
    }

    public static int boundedOrdinal(int metadata, int valueCount, int fallback) {
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount must be positive");
        }
        if (fallback < 0 || fallback >= valueCount) {
            throw new IllegalArgumentException("fallback is outside the value range");
        }
        return metadata >= 0 && metadata < valueCount ? metadata : fallback;
    }
}
