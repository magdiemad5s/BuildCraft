// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FactoryTankVisualResourcesTest {
    @Test
    void packagesTankTexturesForTheBlockAtlasAndRetainsLegacyCopies() throws IOException {
        assertTextureSize("assets/buildcraftfactory/textures/block/tank/end.png", 16, 16);
        assertTextureSize("assets/buildcraftfactory/textures/block/tank/side.png", 16, 16);
        assertTextureSize("assets/buildcraftfactory/textures/block/tank/side_joined_below.png", 16, 16);
        assertTextureSize("assets/buildcraftfactory/textures/blocks/tank/end.png", 16, 16);
        assertTextureSize("assets/buildcraftfactory/textures/blocks/tank/side.png", 16, 16);
        assertTextureSize("assets/buildcraftfactory/textures/blocks/tank/side_joined_below.png", 16, 16);
        assertTextureCopiesMatch("end.png");
        assertTextureCopiesMatch("side.png");
        assertTextureCopiesMatch("side_joined_below.png");
        assertTextureSize("assets/buildcraftfactory/textures/gui/tank.png", 256, 256);
    }

    @Test
    void tankModelsReferenceBlockAtlasSpritesAndPreserveDisplayTransforms() throws IOException {
        String blockModel = readText("assets/buildcraftfactory/models/block/tank.json");
        String joinedModel = readText("assets/buildcraftfactory/models/block/tank_joined_below.json");
        String itemModel = readText("assets/buildcraftfactory/models/item/tank.json");

        assertTrue(blockModel.contains("buildcraftfactory:block/tank/end"));
        assertTrue(blockModel.contains("buildcraftfactory:block/tank/side"));
        assertTrue(joinedModel.contains("buildcraftfactory:block/tank/side_joined_below"));
        assertFalse(blockModel.contains("buildcraftfactory:blocks/tank/"));
        assertFalse(joinedModel.contains("buildcraftfactory:blocks/tank/"));
        for (String transform : new String[] {
            "gui", "ground", "fixed", "thirdperson_righthand", "firstperson_righthand", "firstperson_lefthand"
        }) {
            assertTrue(itemModel.contains("\"" + transform + "\""), "Missing Tank item transform: " + transform);
        }
    }

    private static void assertTextureSize(String path, int expectedWidth, int expectedHeight) throws IOException {
        try (InputStream stream = FactoryTankVisualResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing packaged resource: " + path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "Unreadable PNG resource: " + path);
            assertEquals(expectedWidth, image.getWidth(), "Unexpected width for " + path);
            assertEquals(expectedHeight, image.getHeight(), "Unexpected height for " + path);
        }
    }

    private static void assertTextureCopiesMatch(String fileName) throws IOException {
        byte[] blockAtlasCopy = readBytes("assets/buildcraftfactory/textures/block/tank/" + fileName);
        byte[] legacyCopy = readBytes("assets/buildcraftfactory/textures/blocks/tank/" + fileName);
        assertArrayEquals(legacyCopy, blockAtlasCopy, "Tank texture copies drifted: " + fileName);
    }

    private static byte[] readBytes(String path) throws IOException {
        try (InputStream stream = FactoryTankVisualResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing packaged resource: " + path);
            return stream.readAllBytes();
        }
    }

    private static String readText(String path) throws IOException {
        try (InputStream stream = FactoryTankVisualResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing packaged resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
