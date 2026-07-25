package buildcraft.transport.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LegacyWireVariantCodecTest {
    @Test
    void preservesBlackToWhiteLegacyMetadataOrder() {
        assertEquals(0, LegacyWireVariantCodec.legacyMetadataFromModernDyeId(15));
        assertEquals(15, LegacyWireVariantCodec.legacyMetadataFromModernDyeId(0));
        assertEquals(15, LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(0));
        assertEquals(0, LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(15));
    }

    @Test
    void everyVariantRoundTripsExactly() {
        for (int modernId = 0; modernId < LegacyWireVariantCodec.VARIANT_COUNT; modernId++) {
            int metadata = LegacyWireVariantCodec.legacyMetadataFromModernDyeId(modernId);
            assertEquals(
                modernId,
                LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(metadata)
            );
        }
    }

    @Test
    void malformedValuesAreRejectedOrClampedExplicitly() {
        assertThrows(
            IllegalArgumentException.class,
            () -> LegacyWireVariantCodec.legacyMetadataFromModernDyeId(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(16)
        );
        assertEquals(0, LegacyWireVariantCodec.clampLegacyMetadata(-20));
        assertEquals(15, LegacyWireVariantCodec.clampLegacyMetadata(40));
    }
}
