package buildcraft.lib.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LegacyItemMetadataTest {
    @Test
    void boundedOrdinalKeepsEveryValidOrdinal() {
        assertEquals(0, LegacyItemMetadata.boundedOrdinal(0, 5, 2));
        assertEquals(4, LegacyItemMetadata.boundedOrdinal(4, 5, 2));
    }

    @Test
    void boundedOrdinalUsesFallbackForMalformedLegacyMetadata() {
        assertEquals(2, LegacyItemMetadata.boundedOrdinal(-1, 5, 2));
        assertEquals(2, LegacyItemMetadata.boundedOrdinal(5, 5, 2));
        assertEquals(2, LegacyItemMetadata.boundedOrdinal(Integer.MAX_VALUE, 5, 2));
    }

    @Test
    void boundedOrdinalRejectsInvalidBoundsAndFallbacks() {
        assertThrows(IllegalArgumentException.class, () ->
            LegacyItemMetadata.boundedOrdinal(0, 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            LegacyItemMetadata.boundedOrdinal(0, 5, -1)
        );
        assertThrows(IllegalArgumentException.class, () ->
            LegacyItemMetadata.boundedOrdinal(0, 5, 5)
        );
    }
}
