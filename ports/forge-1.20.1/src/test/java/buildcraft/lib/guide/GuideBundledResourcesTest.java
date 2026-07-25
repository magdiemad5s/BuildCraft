package buildcraft.lib.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideBundledResourcesTest {
    private static final String MANIFEST_NAME = "GUIDE_SOURCE_MANIFEST.json";
    private static final String PINNED_COMMIT =
        "dc16f8b094fd35af1a9154055844ad58e4e98ab5";

    @Test
    void everyBundledGuideSourceMatchesThePinnedManifestHashAndSize() throws Exception {
        JsonObject manifest = readManifest();
        JsonObject source = manifest.getAsJsonObject("source");
        JsonObject counts = manifest.getAsJsonObject("counts");
        JsonArray files = manifest.getAsJsonArray("files");

        assertEquals("buildcraft-neo/guide-source-manifest/v1", manifest.get("schema").getAsString());
        assertEquals("https://github.com/BuildCraft/BuildCraftGuide", source.get("repository").getAsString());
        assertEquals(PINNED_COMMIT, source.get("commit").getAsString());
        assertEquals(120, files.size());

        long totalBytes = 0;
        int markdownFiles = 0;
        int textFiles = 0;
        Map<String, String> hashesByPath = new HashMap<>();
        for (var element : files) {
            JsonObject entry = element.getAsJsonObject();
            String path = entry.get("path").getAsString();
            byte[] bytes = resourceBytes(path);
            int expectedBytes = entry.get("bytes").getAsInt();
            String expectedHash = entry.get("sha256").getAsString();

            assertEquals(expectedBytes, bytes.length, path + " byte count");
            assertEquals(expectedHash, sha256(bytes), path + " SHA-256");
            assertTrue(hashesByPath.put(path, expectedHash) == null, "duplicate path: " + path);

            totalBytes += bytes.length;
            markdownFiles += path.endsWith(".md") ? 1 : 0;
            textFiles += path.endsWith(".txt") ? 1 : 0;
        }

        assertEquals(counts.get("totalFiles").getAsInt(), files.size());
        assertEquals(counts.get("markdown").getAsInt(), markdownFiles);
        assertEquals(counts.get("text").getAsInt(), textFiles);
        assertEquals(counts.get("totalBytes").getAsLong(), totalBytes);

        assertEquals(
            "880b5335c62d43a63c0f8dd0bb080571e77c18f7375b2fa9e7c036bd56a56e11",
            hashesByPath.get(
                "assets/buildcraftlib/compat/buildcraft/guide/en_us/item/guide.md"
            )
        );
        assertEquals(
            "a0c5b590fbf1a2ac4184ba6ddb17e1c7f77e31726df23d6d0901ff38d7d9b433",
            hashesByPath.get(
                "assets/buildcraftlib/compat/buildcraft/guide/en_us/config/guide_page_format.md"
            )
        );
        assertEquals(
            "1c50fbfab01b6606cf0b6647e8b1942b5648b93ba085863250160bf8afcbafbd",
            hashesByPath.get(
                "assets/buildcraftfactory/compat/buildcraft/guide/en_us/block/tank.md"
            )
        );
        assertEquals(
            "e2400f092382240d47f2a0f1cfa82bda2e2d2a724a25bbf2153b15b9647fe257",
            hashesByPath.get(
                "assets/buildcrafttransport/compat/buildcraft/guide/en_us/pipe/diamond_item.md"
            )
        );
    }

    @Test
    void representativeBundledPagesParseWithTheirLegacyFeaturesIntact() throws Exception {
        GuideDocument guide = parseResource(
            "buildcraftlib:item/guide",
            "assets/buildcraftlib/compat/buildcraft/guide/en_us/item/guide.md",
            false
        );
        assertTrue(guide.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.RECIPE
                && line.target().equals("buildcraftlib:guide")
        ));
        assertTrue(guide.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.PAGE_BREAK
        ));

        GuideDocument format = parseResource(
            "buildcraftlib:config/guide_page_format",
            "assets/buildcraftlib/compat/buildcraft/guide/en_us/config/guide_page_format.md",
            false
        );
        assertTrue(format.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.LINK
                && line.target().equals("buildcraftlib:config/json_insn_format")
        ));
        assertTrue(format.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.RECIPE
        ));
        assertTrue(format.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.IMAGE
                && line.target().equals("buildcraftcore:items/wrench")
        ));

        GuideDocument percentPage = parseResource(
            "buildcraftcore:trigger/fluid_below_50",
            "assets/buildcraftcore/compat/buildcraft/guide/en_us/trigger/fluid_below_50.md",
            false
        );
        assertTrue(percentPage.lines().stream().anyMatch(
            line -> line.text().contains("50%")
        ));

        GuideDocument tank = parseResource(
            "buildcraftfactory:block/tank",
            "assets/buildcraftfactory/compat/buildcraft/guide/en_us/block/tank.md",
            false
        );
        assertTrue(tank.lines().stream().anyMatch(line -> !line.text().isBlank()));

        GuideDocument diamondPipe = parseResource(
            "buildcrafttransport:pipe/diamond_item",
            "assets/buildcrafttransport/compat/buildcraft/guide/en_us/pipe/diamond_item.md",
            false
        );
        assertTrue(diamondPipe.lines().stream().anyMatch(
            line -> line.style() == GuideLine.Style.RECIPE
                && line.target().equals("buildcrafttransport:pipe_diamond_item")
        ));
    }

    private static GuideDocument parseResource(String id, String path, boolean showDetail)
        throws IOException {
        return GuideDocumentParser.parse(
            id,
            new String(resourceBytes(path), StandardCharsets.UTF_8),
            showDetail
        );
    }

    private static JsonObject readManifest() throws IOException {
        Path manifest = Path.of(MANIFEST_NAME).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(manifest), "missing " + manifest);
        try (var reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static byte[] resourceBytes(String path) throws IOException {
        try (InputStream stream =
                 GuideBundledResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing bundled guide resource: " + path);
            return stream.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
