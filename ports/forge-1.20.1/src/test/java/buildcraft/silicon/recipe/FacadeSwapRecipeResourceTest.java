/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.silicon.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FacadeSwapRecipeResourceTest {
    @Test
    void specialFacadeSwapSerializerIsExposedAsARecipe() {
        String path = "data/buildcraftsilicon/recipes/special/facade_swap.json";
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(input, "Missing packaged facade swap recipe");
        try (input; Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject recipe = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("buildcraftsilicon:facade_swap", recipe.get("type").getAsString());
            assertEquals("buildcraftsilicon:special/facade_swap", FacadeSwapRecipe.ID.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + path, exception);
        }
    }
}
