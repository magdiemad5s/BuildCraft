/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.fluid;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

/**
 * Compatibility names and NBT loading for BuildCraft's 1.12.2 fluids.
 *
 * <p>Forge 1.12 stored fluid names as global strings such as {@code oil} and
 * {@code oil_heat_1}. Modern Forge parses an unqualified name as a
 * {@code minecraft} resource location, so those payloads otherwise deserialize
 * as an empty stack. Only known BuildCraft names are qualified here; vanilla
 * and third-party unqualified names retain Forge's normal behavior.</p>
 */
public final class LegacyFluidCompat {
    public static final String ENERGY_NAMESPACE = "buildcraftenergy";
    public static final int HEAT_STATE_COUNT = 3;

    private static final List<String> FAMILIES = List.of(
        "oil",
        "oil_residue",
        "oil_heavy",
        "oil_dense",
        "oil_distilled",
        "fuel_dense",
        "fuel_mixed_heavy",
        "fuel_light",
        "fuel_mixed_light",
        "fuel_gaseous"
    );
    private static final Map<String, ResourceLocation> LEGACY_FLUID_NAMES = createLegacyFluidNames();

    private LegacyFluidCompat() {
    }

    public static List<String> familyNames() {
        return FAMILIES;
    }

    public static String modernFluidPath(String family, int heat) {
        requireKnownFamily(family);
        requireValidHeat(heat);
        return heat == 0 ? family : family + "_heat_" + heat;
    }

    public static ResourceLocation modernFluidId(String family, int heat) {
        return new ResourceLocation(ENERGY_NAMESPACE, modernFluidPath(family, heat));
    }
    /**
     * Resolves a persisted fluid name using the same narrow legacy mapping as
     * {@link #loadFluidStackFromNBT(CompoundTag)}.
     */
    public static ResourceLocation resolveFluidIdForLoad(String persistedName) {
        if (persistedName.indexOf(':') < 0) {
            ResourceLocation modernId = LEGACY_FLUID_NAMES.get(persistedName.toLowerCase(Locale.ROOT));
            if (modernId != null) {
                return modernId;
            }
        }
        return new ResourceLocation(persistedName);
    }
    /**
     * Lenient form for optional persisted fields: known BuildCraft names are
     * migrated, while malformed or absent values resolve to {@code null}.
     */
    @Nullable
    public static ResourceLocation tryResolveFluidIdForLoad(@Nullable String persistedName) {
        if (persistedName == null || persistedName.isEmpty()) {
            return null;
        }
        if (persistedName.indexOf(':') < 0) {
            ResourceLocation modernId = LEGACY_FLUID_NAMES.get(persistedName.toLowerCase(Locale.ROOT));
            if (modernId != null) {
                return modernId;
            }
        }
        return ResourceLocation.tryParse(persistedName);
    }


    public static String legacyFluidBlockPath(String family, int heat) {
        requireKnownFamily(family);
        requireValidHeat(heat);
        return "fluid_block_" + family + "_heat_" + heat;
    }

    /**
     * Loads a Forge fluid stack after qualifying a known unnamespaced
     * BuildCraft 1.12 fluid name.
     */
    public static FluidStack loadFluidStackFromNBT(@Nullable CompoundTag nbt) {
        return FluidStack.loadFluidStackFromNBT(normalizeForLoad(nbt));
    }

    /**
     * Returns the original tag when no migration is needed, or a migrated copy
     * when an unnamespaced BuildCraft fluid name is found.
     */
    @Nullable
    public static CompoundTag normalizeForLoad(@Nullable CompoundTag nbt) {
        if (nbt == null || !nbt.contains("FluidName", Tag.TAG_STRING)) {
            return nbt;
        }
        String legacyName = nbt.getString("FluidName");
        if (legacyName.indexOf(':') >= 0) {
            return nbt;
        }
        ResourceLocation modernId = LEGACY_FLUID_NAMES.get(legacyName.toLowerCase(Locale.ROOT));
        if (modernId == null) {
            return nbt;
        }
        CompoundTag migrated = nbt.copy();
        migrated.putString("FluidName", modernId.toString());
        return migrated;
    }

    private static Map<String, ResourceLocation> createLegacyFluidNames() {
        Map<String, ResourceLocation> names = new HashMap<>();
        for (String family : FAMILIES) {
            for (int heat = 0; heat < HEAT_STATE_COUNT; heat++) {
                String path = modernFluidPath(family, heat);
                ResourceLocation previous = names.put(path, modernFluidId(family, heat));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate legacy BuildCraft fluid name: " + path);
                }
            }
        }
        return Map.copyOf(names);
    }

    private static void requireKnownFamily(String family) {
        if (!FAMILIES.contains(family)) {
            throw new IllegalArgumentException("Unknown BuildCraft fluid family: " + family);
        }
    }

    private static void requireValidHeat(int heat) {
        if (heat < 0 || heat >= HEAT_STATE_COUNT) {
            throw new IllegalArgumentException("Invalid BuildCraft fluid heat index: " + heat);
        }
    }
}
