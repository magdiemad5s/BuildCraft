package buildcraft.transport.item;

/**
 * Converts between modern {@code DyeColor} IDs (white first) and the legacy
 * 1.12.2 wire metadata order (black first).
 */
public final class LegacyWireVariantCodec {
    public static final int VARIANT_COUNT = 16;

    private LegacyWireVariantCodec() {
    }

    public static int legacyMetadataFromModernDyeId(int modernDyeId) {
        requireVariant(modernDyeId, "modernDyeId");
        return VARIANT_COUNT - 1 - modernDyeId;
    }

    public static int modernDyeIdFromLegacyMetadata(int legacyMetadata) {
        requireVariant(legacyMetadata, "legacyMetadata");
        return VARIANT_COUNT - 1 - legacyMetadata;
    }

    public static int clampLegacyMetadata(int legacyMetadata) {
        return Math.max(0, Math.min(VARIANT_COUNT - 1, legacyMetadata));
    }

    private static void requireVariant(int value, String name) {
        if (value < 0 || value >= VARIANT_COUNT) {
            throw new IllegalArgumentException(name + " must be in [0, 15]: " + value);
        }
    }
}
