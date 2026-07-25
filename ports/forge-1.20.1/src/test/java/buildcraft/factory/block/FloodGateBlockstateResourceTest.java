package buildcraft.factory.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FloodGateBlockstateResourceTest {
    private static final String BLOCKSTATE = "assets/buildcraftfactory/blockstates/flood_gate.json";
    private static final Set<String> DIRECTIONS = Set.of("down", "north", "south", "west", "east");

    @Test
    void everyControllableFaceHasOpenAndClosedParseableModels() {
        JsonArray multipart = readObject(BLOCKSTATE).getAsJsonArray("multipart");
        assertNotNull(multipart, BLOCKSTATE + " must use vanilla multipart models");
        assertEquals(11, multipart.size(), "Expected a base plus open/closed models for five faces");
        Map<String, Set<String>> valuesByProperty = new HashMap<>();
        for (JsonElement element : multipart) {
            JsonObject part = element.getAsJsonObject();
            JsonObject apply = part.getAsJsonObject("apply");
            assertNotNull(apply, "Every multipart entry must apply a model");
            String model = apply.get("model").getAsString();
            readObject("assets/" + model.replace(":", "/models/") + ".json");
            JsonObject when = part.getAsJsonObject("when");
            if (when == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : when.entrySet()) {
                valuesByProperty.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>())
                    .add(entry.getValue().getAsString());
            }
        }
        assertEquals(5, valuesByProperty.size(), "Only the five controllable faces belong in the blockstate");
        for (String direction : DIRECTIONS) {
            assertEquals(Set.of("true", "false"), valuesByProperty.get("connected_" + direction));
        }
    }

    private static JsonObject readObject(String resource) {
        InputStream input = FloodGateBlockstateResourceTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(input, "Missing packaged resource " + resource);
        try (input; Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + resource, exception);
        }
    }
}
