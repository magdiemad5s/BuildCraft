package buildcraft.neo.forge1201;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LegacyIdentityManifestTest {
    @Test
    void machineReadableContractRetainsTheAuditedLegacyCounts() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream("LEGACY_IDENTITY_MANIFEST.json");
        assertNotNull(stream, "legacy identity contract must be present in test resources");
        String manifest = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(manifest.contains("\"declarationCount\": 182"));
        assertTrue(manifest.contains("\"item\": 116"));
        assertTrue(manifest.contains("\"block\": 33"));
        assertTrue(manifest.contains("\"blockEntity\": 33"));
        assertTrue(manifest.contains("\"buildcraftfactory:tank\""));
        assertTrue(manifest.contains("\"buildcraft_wire_systems\""));
        assertTrue(manifest.contains("\"acceptedNamespacePrefix\": \"buildcraft\""));
    }
}
