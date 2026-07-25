/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.energy.generation.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import buildcraft.energy.BCEnergyConfig;

class OilWorldgenDataTest {
    private static final String OIL_DESERT = "buildcraftenergy:oil_desert";

    @Test
    void legacyOilDesertTagEntriesAreOptionalUntilTheCustomBiomeReturns() {
        assertOptionalOilDesertEntry("data/forge/tags/worldgen/biome/is_desert.json");
        assertOptionalOilDesertEntry("data/forge/tags/worldgen/biome/is_sandy.json");
    }

    @Test
    void biomeModifierPlacedFeatureAndConfiguredFeatureStayLinked() {
        JsonObject modifier = readObject("data/buildcraftenergy/forge/biome_modifier/add_oil.json");
        assertEquals("forge:add_features", string(modifier, "type"));
        assertEquals("#minecraft:is_overworld", string(modifier, "biomes"));
        assertEquals("buildcraftenergy:oil_placed_feature", string(modifier, "features"));
        assertEquals("fluid_springs", string(modifier, "step"));

        JsonObject placed = readObject(
            "data/buildcraftenergy/worldgen/placed_feature/oil_placed_feature.json"
        );
        JsonObject inlineFeature = placed.getAsJsonObject("feature");
        assertNotNull(inlineFeature, "The oil placed feature must contain its configured feature");
        assertEquals("buildcraftenergy:worldgen.feature.oil", string(inlineFeature, "type"));

        JsonObject configured = readObject(
            "data/buildcraftenergy/worldgen/configured_feature/oil_configured_feature.json"
        );
        assertEquals(string(inlineFeature, "type"), string(configured, "type"));
        assertEquals(
            configured.getAsJsonObject("config"),
            inlineFeature.getAsJsonObject("config"),
            "The stable configured-feature resource and the inline placed-feature config must not drift"
        );
    }

    @Test
    void packagedWorldgenDefaultsKeepEveryDepositSizeEnabled() {
        JsonObject placed = readObject(
            "data/buildcraftenergy/worldgen/placed_feature/oil_placed_feature.json"
        );
        JsonObject settings = placed.getAsJsonObject("feature")
            .getAsJsonObject("config")
            .getAsJsonObject("oilStructureSetting");

        assertEquals(
            BCEnergyConfig.DEFAULT_SMALL_OIL_GEN_PROB,
            settings.get("smallOilGenProb").getAsDouble(),
            0.0
        );
        assertEquals(
            BCEnergyConfig.DEFAULT_MEDIUM_OIL_GEN_PROB,
            settings.get("mediumOilGenProb").getAsDouble(),
            0.0
        );
        assertEquals(
            BCEnergyConfig.DEFAULT_LARGE_OIL_GEN_PROB,
            settings.get("largeOilGenProb").getAsDouble(),
            0.0
        );
        assertTrue(settings.get("smallOilGenProb").getAsDouble() > 0.0);
        assertTrue(settings.get("mediumOilGenProb").getAsDouble() > 0.0);
        assertTrue(settings.get("largeOilGenProb").getAsDouble() > 0.0);
    }

    private static void assertOptionalOilDesertEntry(String resourcePath) {
        JsonArray values = readObject(resourcePath).getAsJsonArray("values");
        assertNotNull(values, resourcePath + " must define a values array");

        boolean optionalEntryFound = false;
        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                assertFalse(
                    OIL_DESERT.equals(value.getAsString()),
                    resourcePath + " must not require the currently unregistered legacy oil-desert biome"
                );
                continue;
            }
            JsonObject entry = value.getAsJsonObject();
            if (OIL_DESERT.equals(string(entry, "id"))) {
                optionalEntryFound = true;
                assertFalse(
                    entry.get("required").getAsBoolean(),
                    resourcePath + " must mark the preserved legacy biome ID as optional"
                );
            }
        }
        assertTrue(optionalEntryFound, resourcePath + " must preserve the legacy oil-desert ID");
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        assertNotNull(value, "Missing JSON field " + key);
        return value.getAsString();
    }

    private static JsonObject readObject(String resourcePath) {
        InputStream input = OilWorldgenDataTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(input, "Missing packaged resource " + resourcePath);
        try (input; Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + resourcePath, exception);
        }
    }
}
