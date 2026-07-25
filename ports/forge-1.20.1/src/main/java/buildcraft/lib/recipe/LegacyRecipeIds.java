/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.recipe;

import net.minecraft.resources.ResourceLocation;

/**
 * Released BuildCraft recipe identities that must remain stable for recipe
 * advancements, scripts, datapacks, and existing worlds.
 */
public final class LegacyRecipeIds {
    public static final ResourceLocation REDSTONE_ENGINE =
        new ResourceLocation("buildcraftcore", "redstone_engine");
    public static final ResourceLocation STIRLING_ENGINE =
        new ResourceLocation("buildcraftenergy", "stirling_engine");
    public static final ResourceLocation COMBUSTION_ENGINE =
        new ResourceLocation("buildcraftenergy", "combustion_engine");
    public static final ResourceLocation AUTOWORKBENCH_ITEM =
        new ResourceLocation("buildcraftfactory", "autoworkbench_item");
    public static final ResourceLocation PIPE_STRUCTURE =
        new ResourceLocation("buildcrafttransport", "pipe_structure");
    public static final ResourceLocation PIPE_SEALANT =
        new ResourceLocation("buildcrafttransport", "pipe_sealant");
    public static final ResourceLocation RESIDUE_TO_PIPE_SEALANT =
        new ResourceLocation("buildcraftenergy", "residue_to_pipe_sealant");

    private LegacyRecipeIds() {
    }
}