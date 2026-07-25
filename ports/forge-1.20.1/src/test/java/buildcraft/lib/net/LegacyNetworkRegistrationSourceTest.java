/* Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0 */
package buildcraft.lib.net;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LegacyNetworkRegistrationSourceTest {
    @Test
    void legacyPacketReceptionSidesAreExplicit() throws IOException {
        String lib = read("src/main/java/buildcraft/lib/BCLibProxy.java");
        assertTrue(lib.contains("MessageMarker::new, Dist.CLIENT"));
        assertTrue(lib.contains("MessageObjectCacheRequest::new, Dist.DEDICATED_SERVER"));
        assertTrue(lib.contains("MessageObjectCacheResponse::new, Dist.CLIENT"));
        assertTrue(lib.contains("MessageDebugResponse::new, Dist.CLIENT"));

        String core = read("src/main/java/buildcraft/core/BCCore.java");
        assertTrue(core.contains("MessageVolumeBoxes::new, Dist.CLIENT"));

        String builders = read("src/main/java/buildcraft/builders/BCBuilders.java");
        assertTrue(builders.contains("MessageSnapshotRequest::new, Dist.DEDICATED_SERVER"));
        assertTrue(builders.contains("MessageSnapshotResponse::new, Dist.CLIENT"));

        String robotics = read("src/main/java/buildcraft/robotics/BCRobotics.java");
        assertTrue(robotics.contains("MessageZoneMapRequest::new, Dist.DEDICATED_SERVER"));
        assertTrue(robotics.contains("MessageZoneMapResponse::new, Dist.CLIENT"));
    }

    @Test
    void simpleChannelUsesDirectionConstraintAndOneStableDiscriminator() throws IOException {
        String manager = read("src/main/java/buildcraft/lib/net/MessageManager.java");
        assertTrue(manager.contains("new TreeMap<>(MESSAGE_CLASS_ORDER)"));
        assertTrue(manager.contains("expectedDirection(cl, sv)"));
        assertTrue(manager.contains("int messageId = 0"));
        assertTrue(manager.contains("messageId++"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
