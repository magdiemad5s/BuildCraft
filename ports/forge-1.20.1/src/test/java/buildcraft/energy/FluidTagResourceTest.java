/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

class FluidTagResourceTest {
    private static final List<String> OILS = List.of(
        "oil", "oil_residue", "oil_heavy", "oil_dense", "oil_distilled"
    );
    private static final List<String> FUELS = List.of(
        "fuel_dense", "fuel_mixed_heavy", "fuel_light", "fuel_mixed_light", "fuel_gaseous"
    );

    @Test
    void aggregateOilTagContainsEveryTemperatureAndFlowingVariant() {
        assertTag("data/buildcraftenergy/tags/fluids/is_oil.json", OILS);
    }

    @Test
    void aggregateFuelTagContainsEveryTemperatureAndFlowingVariant() {
        assertTag("data/buildcraftenergy/tags/fluids/is_fuel.json", FUELS);
    }

    private static void assertTag(String path, List<String> families) {
        Set<String> expected = new LinkedHashSet<>();
        for (String family : families) {
            for (String temperature : List.of("", "_heat_1", "_heat_2")) {
                String source = "buildcraftenergy:" + family + temperature;
                expected.add(source);
                expected.add(source + "_flowing");
            }
        }

        Set<String> actual = new LinkedHashSet<>();
        for (JsonElement value : read(path).getAsJsonArray()) {
            actual.add(value.getAsString());
        }
        assertEquals(expected, actual, path + " must contain every source and flowing fluid exactly once");
        assertEquals(30, actual.size());
    }

    private static JsonElement read(String path) {
        InputStream input = FluidTagResourceTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(input, "Missing packaged fluid tag " + path);
        try (input; Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            assertNotNull(root.getAsJsonObject().get("values"), path + " has no values array");
            return root.getAsJsonObject().get("values");
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + path, exception);
        }
    }
}
