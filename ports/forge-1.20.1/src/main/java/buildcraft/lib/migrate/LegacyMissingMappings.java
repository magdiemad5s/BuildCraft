/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.migrate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import buildcraft.api.core.BCLog;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Reinstates every legacy {@code oldReg(...)} migration declared by the
 * original 1.12.2 source. The old migration manager matched the lower-cased
 * path for every namespace beginning with {@code buildcraft}; that behavior is
 * preserved here so renamed module namespaces and historical casing continue
 * to load whenever Forge can represent the old resource location.
 */
public final class LegacyMissingMappings {
    private static final Map<String, ResourceLocation> ITEM_ALIASES = aliases(new String[][] {
        { "filling_planner", "buildcraftbuilders:filler_planner" },
        { "constructionmarkerblock", "buildcraftbuilders:marker_construction" },
        { "wrenchitem", "buildcraftcore:wrench" },
        { "woodengearitem", "buildcraftcore:gear_wood" },
        { "stonegearitem", "buildcraftcore:gear_stone" },
        { "irongearitem", "buildcraftcore:gear_iron" },
        { "goldgearitem", "buildcraftcore:gear_gold" },
        { "diamondgearitem", "buildcraftcore:gear_diamond" },
        { "listitem", "buildcraftcore:list" },
        { "maplocation", "buildcraftcore:map_location" },
        { "markerblock", "buildcraftcore:marker_volume" },
        { "pathmarkerblock", "buildcraftcore:marker_path" },
        { "glob_oil", "buildcraftenergy:glob_of_oil" },
        { "plasticsheet", "buildcraftfactory:plastic_sheet" },
        { "heat_exchange_start", "buildcraftfactory:heat_exchange" },
        { "heat_exchange_middle", "buildcraftfactory:heat_exchange" },
        { "heat_exchange_end", "buildcraftfactory:heat_exchange" },
        { "plug_gate", "buildcraftsilicon:plug_gate" },
        { "plug_lens", "buildcraftsilicon:plug_lens" },
        { "plug_pulsar", "buildcraftsilicon:plug_pulsar" },
        { "plug_light_sensor", "buildcraftsilicon:plug_light_sensor" },
        { "plug_timer", "buildcraftsilicon:plug_timer" },
        { "plug_facade", "buildcraftsilicon:plug_facade" },
        { "pipewaterproof", "buildcrafttransport:waterproof" }
    });

    private static final Map<String, ResourceLocation> BLOCK_ALIASES = aliases(new String[][] {
        { "constructionmarkerblock", "buildcraftbuilders:marker_construction" },
        { "engineblock", "buildcraftcore:engine" },
        { "markerblock", "buildcraftcore:marker_volume" },
        { "pathmarkerblock", "buildcraftcore:marker_path" },
        { "power_tester", "buildcraftcore:power_tester" },
        { "autoworkbenchblock", "buildcraftfactory:autoworkbench_item" },
        { "miningwellblock", "buildcraftfactory:mining_well" },
        { "pumpblock", "buildcraftfactory:pump" },
        { "tubeblock", "buildcraftfactory:tube" },
        { "floodgateblock", "buildcraftfactory:flood_gate" },
        { "tankblock", "buildcraftfactory:tank" },
        { "chuteblock", "buildcraftfactory:chute" },
        { "heat_exchange_start", "buildcraftfactory:heat_exchange" },
        { "heat_exchange_middle", "buildcraftfactory:heat_exchange" },
        { "heat_exchange_end", "buildcraftfactory:heat_exchange" },
        { "laserblock", "buildcraftsilicon:laser" },
        { "assemblytableblock", "buildcraftsilicon:assembly_table" },
        { "advancedcraftingtableblock", "buildcraftsilicon:advanced_crafting_table" },
        { "integrationtableblock", "buildcraftsilicon:integration_table" },
        { "chargingtableblock", "buildcraftsilicon:charging_table" },
        { "programmingtableblock", "buildcraftsilicon:programming_table" },
        { "zoneplannerblock", "buildcraftrobotics:zone_planner" },
        { "filteredbufferblock", "buildcrafttransport:filtered_buffer" },
        { "fluid_block_oil", "buildcraftenergy:oil" },
        { "fluid_block_fuel", "buildcraftenergy:fuel_light" },
        { "fluid_block_oil_heat_0", "buildcraftenergy:oil" },
        { "fluid_block_oil_heat_1", "buildcraftenergy:oil_heat_1" },
        { "fluid_block_oil_heat_2", "buildcraftenergy:oil_heat_2" },
        { "fluid_block_oil_residue_heat_0", "buildcraftenergy:oil_residue" },
        { "fluid_block_oil_residue_heat_1", "buildcraftenergy:oil_residue_heat_1" },
        { "fluid_block_oil_residue_heat_2", "buildcraftenergy:oil_residue_heat_2" },
        { "fluid_block_oil_heavy_heat_0", "buildcraftenergy:oil_heavy" },
        { "fluid_block_oil_heavy_heat_1", "buildcraftenergy:oil_heavy_heat_1" },
        { "fluid_block_oil_heavy_heat_2", "buildcraftenergy:oil_heavy_heat_2" },
        { "fluid_block_oil_dense_heat_0", "buildcraftenergy:oil_dense" },
        { "fluid_block_oil_dense_heat_1", "buildcraftenergy:oil_dense_heat_1" },
        { "fluid_block_oil_dense_heat_2", "buildcraftenergy:oil_dense_heat_2" },
        { "fluid_block_oil_distilled_heat_0", "buildcraftenergy:oil_distilled" },
        { "fluid_block_oil_distilled_heat_1", "buildcraftenergy:oil_distilled_heat_1" },
        { "fluid_block_oil_distilled_heat_2", "buildcraftenergy:oil_distilled_heat_2" },
        { "fluid_block_fuel_dense_heat_0", "buildcraftenergy:fuel_dense" },
        { "fluid_block_fuel_dense_heat_1", "buildcraftenergy:fuel_dense_heat_1" },
        { "fluid_block_fuel_dense_heat_2", "buildcraftenergy:fuel_dense_heat_2" },
        { "fluid_block_fuel_mixed_heavy_heat_0", "buildcraftenergy:fuel_mixed_heavy" },
        { "fluid_block_fuel_mixed_heavy_heat_1", "buildcraftenergy:fuel_mixed_heavy_heat_1" },
        { "fluid_block_fuel_mixed_heavy_heat_2", "buildcraftenergy:fuel_mixed_heavy_heat_2" },
        { "fluid_block_fuel_light_heat_0", "buildcraftenergy:fuel_light" },
        { "fluid_block_fuel_light_heat_1", "buildcraftenergy:fuel_light_heat_1" },
        { "fluid_block_fuel_light_heat_2", "buildcraftenergy:fuel_light_heat_2" },
        { "fluid_block_fuel_mixed_light_heat_0", "buildcraftenergy:fuel_mixed_light" },
        { "fluid_block_fuel_mixed_light_heat_1", "buildcraftenergy:fuel_mixed_light_heat_1" },
        { "fluid_block_fuel_mixed_light_heat_2", "buildcraftenergy:fuel_mixed_light_heat_2" },
        { "fluid_block_fuel_gaseous_heat_0", "buildcraftenergy:fuel_gaseous" },
        { "fluid_block_fuel_gaseous_heat_1", "buildcraftenergy:fuel_gaseous_heat_1" },
        { "fluid_block_fuel_gaseous_heat_2", "buildcraftenergy:fuel_gaseous_heat_2" }
    });

    private static final Map<String, ResourceLocation> BLOCK_ENTITY_ALIASES = aliases(new String[][] {
        { "buildcraft.builders.constructionmarker", "buildcraftbuilders:entity_marker_construction" },
        { "net.minecraft.src.builders.tileconstructionmarker", "buildcraftbuilders:entity_marker_construction" },
        { "buildcraft.builders.marker", "buildcraftcore:marker.volume" },
        { "marker", "buildcraftcore:marker.volume" },
        { "heat_exchange.start", "buildcraftfactory:heat_exchange" },
        { "heat_exchange.end", "buildcraftfactory:heat_exchange" }
    });

    private LegacyMissingMappings() {
    }

    public static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS, ITEM_ALIASES);
        remap(event, ForgeRegistries.Keys.BLOCKS, ForgeRegistries.BLOCKS, BLOCK_ALIASES);
        remap(
            event,
            ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
            ForgeRegistries.BLOCK_ENTITY_TYPES,
            BLOCK_ENTITY_ALIASES
        );
    }

    private static <T> void remap(
        MissingMappingsEvent event,
        ResourceKey<? extends Registry<T>> registryKey,
        IForgeRegistry<T> registry,
        Map<String, ResourceLocation> aliases
    ) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getAllMappings(registryKey)) {
            ResourceLocation missing = mapping.getKey();
            if (!missing.getNamespace().startsWith("buildcraft")) {
                continue;
            }
            ResourceLocation targetId = aliases.get(missing.getPath().toLowerCase(Locale.ROOT));
            if (targetId == null) {
                continue;
            }
            T target = registry.getValue(targetId);
            if (target == null) {
                BCLog.logger.warn(
                    "Unable to remap legacy BuildCraft registry id " + missing + " because target " + targetId
                        + " is not registered"
                );
                continue;
            }
            mapping.remap(target);
            BCLog.logger.info("Remapped legacy BuildCraft registry id " + missing + " to " + targetId);
        }
    }

    static ResourceLocation resolveItemAlias(String path) {
        return ITEM_ALIASES.get(path.toLowerCase(Locale.ROOT));
    }

    static ResourceLocation resolveBlockAlias(String path) {
        return BLOCK_ALIASES.get(path.toLowerCase(Locale.ROOT));
    }

    static ResourceLocation resolveBlockEntityAlias(String path) {
        return BLOCK_ENTITY_ALIASES.get(path.toLowerCase(Locale.ROOT));
    }

    static int itemAliasCount() {
        return ITEM_ALIASES.size();
    }

    static int blockAliasCount() {
        return BLOCK_ALIASES.size();
    }

    static int blockEntityAliasCount() {
        return BLOCK_ENTITY_ALIASES.size();
    }

    private static Map<String, ResourceLocation> aliases(String[][] values) {
        Map<String, ResourceLocation> aliases = new HashMap<>();
        for (String[] value : values) {
            ResourceLocation previous = aliases.put(value[0], new ResourceLocation(value[1]));
            if (previous != null) {
                throw new IllegalStateException("Duplicate legacy BuildCraft alias: " + value[0]);
            }
        }
        return Map.copyOf(aliases);
    }
}