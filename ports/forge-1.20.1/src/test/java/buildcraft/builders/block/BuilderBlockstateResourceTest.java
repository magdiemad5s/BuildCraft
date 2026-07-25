/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class BuilderBlockstateResourceTest {
    private static final String RESOURCE = "assets/buildcraftbuilders/blockstates/builder.json";
    private static final String MAIN_MODEL = "buildcraftbuilders:block/builder/main";
    private static final Map<String, Integer> ROTATION_BY_FACING = Map.of(
        "north", 0,
        "east", 90,
        "south", 180,
        "west", 270
    );
    private static final Map<String, String> SLOT_MODEL_BY_TYPE = Map.of(
        "none", "buildcraftbuilders:block/builder/slot_empty",
        "blueprint", "buildcraftbuilders:block/builder/slot_blueprint",
        "template", "buildcraftbuilders:block/builder/slot_template"
    );

    @Test
    void everyFacingAndSnapshotTypeHasExactlyOneMainAndOneSlotModel() {
        JsonArray multipart = readObject().getAsJsonArray("multipart");
        assertNotNull(multipart, RESOURCE + " must define a multipart array");

        for (Map.Entry<String, Integer> facing : ROTATION_BY_FACING.entrySet()) {
            for (Map.Entry<String, String> snapshot : SLOT_MODEL_BY_TYPE.entrySet()) {
                List<JsonObject> matches = matchingApplications(
                    multipart,
                    facing.getKey(),
                    snapshot.getKey()
                );

                assertEquals(
                    2,
                    matches.size(),
                    () -> facing.getKey() + "/" + snapshot.getKey()
                        + " must resolve uniquely to the Builder body and one slot overlay"
                );
                assertModel(matches, MAIN_MODEL, facing.getValue(), facing.getKey(), snapshot.getKey());
                assertModel(matches, snapshot.getValue(), facing.getValue(), facing.getKey(), snapshot.getKey());
            }
        }
    }

    private static List<JsonObject> matchingApplications(
        JsonArray multipart,
        String facing,
        String snapshotType
    ) {
        List<JsonObject> matches = new ArrayList<>();
        for (JsonElement element : multipart) {
            JsonObject part = element.getAsJsonObject();
            JsonObject when = part.getAsJsonObject("when");
            if (matches(when, "facing", facing) && matches(when, "snapshot_type", snapshotType)) {
                matches.add(part.getAsJsonObject("apply"));
            }
        }
        return matches;
    }

    private static boolean matches(JsonObject when, String property, String value) {
        JsonElement allowedValues = when == null ? null : when.get(property);
        if (allowedValues == null) {
            return true;
        }
        for (String allowed : allowedValues.getAsString().split("\\|")) {
            if (allowed.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void assertModel(
        List<JsonObject> applications,
        String expectedModel,
        int expectedRotation,
        String facing,
        String snapshotType
    ) {
        List<JsonObject> matchingModels = applications.stream()
            .filter(application -> expectedModel.equals(application.get("model").getAsString()))
            .toList();
        assertEquals(
            1,
            matchingModels.size(),
            () -> facing + "/" + snapshotType + " must contain exactly one " + expectedModel
        );
        JsonElement rotation = matchingModels.get(0).get("y");
        assertEquals(
            expectedRotation,
            rotation == null ? 0 : rotation.getAsInt(),
            () -> facing + "/" + snapshotType + " has the wrong model rotation for " + expectedModel
        );
    }

    private static JsonObject readObject() {
        InputStream input = BuilderBlockstateResourceTest.class.getClassLoader().getResourceAsStream(RESOURCE);
        assertNotNull(input, "Missing packaged resource " + RESOURCE);
        try (input; Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + RESOURCE, exception);
        }
    }
}
