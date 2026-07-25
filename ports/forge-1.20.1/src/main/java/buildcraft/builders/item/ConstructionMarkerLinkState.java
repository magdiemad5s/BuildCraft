/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.item;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Pure, deterministic storage policy for the Construction Marker two-click link state. */
public final class ConstructionMarkerLinkState {
    public static final int MAX_REACH = 64;
    public static final long MAX_REACH_SQUARED = (long) MAX_REACH * MAX_REACH;

    static final String TAG_X = "x";
    static final String TAG_Y = "y";
    static final String TAG_Z = "z";
    static final String TAG_DIMENSION = "dimension";
    static final String TAG_RECORDING = "recording";

    private ConstructionMarkerLinkState() {
    }

    public static boolean hasOriginCoordinates(CompoundTag tag) {
        return tag != null
            && tag.contains(TAG_X, Tag.TAG_INT)
            && tag.contains(TAG_Y, Tag.TAG_INT)
            && tag.contains(TAG_Z, Tag.TAG_INT);
    }

    public static boolean isRecording(CompoundTag tag) {
        return hasOriginCoordinates(tag) || tag != null && tag.getBoolean(TAG_RECORDING);
    }

    public static void start(CompoundTag tag, int x, int y, int z, String dimension) {
        tag.putInt(TAG_X, x);
        tag.putInt(TAG_Y, y);
        tag.putInt(TAG_Z, z);
        tag.putString(TAG_DIMENSION, dimension == null ? "" : dimension);
        // The predicate now reads the historical coordinates. Do not perpetuate the incomplete port's shadow state.
        tag.remove(TAG_RECORDING);
    }

    public static Optional<Origin> readOrigin(CompoundTag tag) {
        return readOrigin(tag, null);
    }

    /** Reads pre-dimension legacy x/y/z state in the supplied current-dimension fallback. */
    public static Optional<Origin> readOrigin(CompoundTag tag, String fallbackDimension) {
        if (!hasOriginCoordinates(tag)) {
            return Optional.empty();
        }
        String dimension = tag.contains(TAG_DIMENSION, Tag.TAG_STRING)
            ? tag.getString(TAG_DIMENSION)
            : fallbackDimension;
        if (dimension == null || dimension.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Origin(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z), dimension));
    }

    public static void clear(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        tag.remove(TAG_X);
        tag.remove(TAG_Y);
        tag.remove(TAG_Z);
        tag.remove(TAG_DIMENSION);
        tag.remove(TAG_RECORDING);
    }

    public static boolean withinReach(int x1, int y1, int z1, int x2, int y2, int z2) {
        long dx = (long) x2 - x1;

        long dy = (long) y2 - y1;
        long dz = (long) z2 - z1;
        return dx * dx + dy * dy + dz * dz <= MAX_REACH_SQUARED;
    }

    /** Applies the intentionally asymmetric released link completion policy. */
    public static void complete(CompoundTag tag, Completion completion) {
        if (completion != Completion.RECOVERABLE_FAILURE) {
            clear(tag);
        }
    }

    public enum Completion {
        SUCCESS,
        INVALID_BLOCK,
        RECOVERABLE_FAILURE
    }


    public record Origin(int x, int y, int z, String dimension) {
    }
}
