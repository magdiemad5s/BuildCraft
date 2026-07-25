/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.Map;

import javax.annotation.Nullable;

/** Bounds and validates state exchanged by the Assembly Table menu. */
public final class AssemblyTableStatePolicy {
    public static final int MAX_SYNCED_RECIPES = 4096;
    public static final int MAX_RECIPE_ID_LENGTH = 256;

    private AssemblyTableStatePolicy() {
    }

    public static boolean isValidRecipeCount(int count) {
        return count >= 0 && count <= MAX_SYNCED_RECIPES;
    }

    @Nullable
    public static EnumAssemblyRecipeState stateByOrdinal(int ordinal) {
        EnumAssemblyRecipeState[] values = EnumAssemblyRecipeState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public static boolean isClientSelectable(EnumAssemblyRecipeState state) {
        return state == EnumAssemblyRecipeState.POSSIBLE || state == EnumAssemblyRecipeState.SAVED;
    }

    /** Recipe identity and state both drive the open Assembly Table screen. */
    public static boolean hasRecipeStateSnapshotChanged(Map<?, ?> before, Map<?, ?> after) {
        return !before.equals(after);
    }
}
