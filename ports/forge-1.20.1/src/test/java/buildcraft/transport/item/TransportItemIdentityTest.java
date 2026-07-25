package buildcraft.transport.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TransportItemIdentityTest {
    @Test
    void exposesAllFortySixLiveLegacyPipeItemPathsExactlyOnce() {
        var paths = TransportItemIdentity.livePipeItemPaths();

        assertEquals(46, paths.size());
        assertEquals(46, new HashSet<>(paths).size());
        assertTrue(paths.stream().allMatch(path -> path.startsWith("pipe_")));
    }

    @Test
    void preservesTheHistoricalCobbleAbbreviationOnlyForItemIds() {
        assertEquals(
            "pipe_cobble_item",
            TransportItemIdentity.pipeItemPath("cobblestone_item")
        );
        assertEquals(
            "pipe_cobble_rf",
            TransportItemIdentity.pipeItemPath("cobblestone_rf")
        );
        assertEquals("pipe_structure", TransportItemIdentity.pipeItemPath("structure"));
        assertEquals("pipe_wood_item", TransportItemIdentity.pipeItemPath("wood_item"));
    }

    @Test
    void rejectsInvalidDefinitionPaths() {
        assertThrows(NullPointerException.class, () -> TransportItemIdentity.pipeItemPath(null));
        assertThrows(IllegalArgumentException.class, () -> TransportItemIdentity.pipeItemPath(" "));
    }
}
