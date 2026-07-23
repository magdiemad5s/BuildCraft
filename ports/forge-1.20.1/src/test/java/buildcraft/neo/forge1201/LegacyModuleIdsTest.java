package buildcraft.neo.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyModuleIdsTest {
    @Test
    void preservesEveryLegacyModuleIdInLegacyOrder() {
        assertEquals(List.of(
            "buildcraftlib",
            "buildcraftcore",
            "buildcraftbuilders",
            "buildcraftenergy",
            "buildcraftfactory",
            "buildcraftsilicon",
            "buildcrafttransport",
            "buildcraftrobotics"
        ), LegacyModuleIds.ordered());
    }

    @Test
    void distinguishesKnownAndUnknownModuleIds() {
        assertTrue(LegacyModuleIds.isKnown("buildcrafttransport"));
        assertFalse(LegacyModuleIds.isKnown("buildcraftneo"));
    }

    @Test
    void keepsTheLegacyResourceNamespace() {
        assertEquals("buildcraft", BuildCraftNeo.RESOURCE_NAMESPACE);
    }
}
