package buildcraft.builders.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class ConstructionMarkerLinkStateTest {
    @Test
    void startUsesReleasedCoordinateKeysAndRoundTripsDimension() {
        CompoundTag tag = new CompoundTag();
        tag.putString("unrelated", "kept");
        tag.putBoolean("recording", true);

        ConstructionMarkerLinkState.start(tag, 12, -4, 37, "minecraft:overworld");

        assertEquals(12, tag.getInt("x"));
        assertEquals(-4, tag.getInt("y"));
        assertEquals(37, tag.getInt("z"));
        assertFalse(tag.contains("recording"));
        assertEquals(
            new ConstructionMarkerLinkState.Origin(12, -4, 37, "minecraft:overworld"),
            ConstructionMarkerLinkState.readOrigin(tag).orElseThrow()
        );
        assertEquals("kept", tag.getString("unrelated"));
    }

    @Test
    void incompletePortRecordingFlagIsDisplayEvidenceNotAnOrigin() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("recording", true);

        assertTrue(ConstructionMarkerLinkState.isRecording(tag));
        assertFalse(ConstructionMarkerLinkState.hasOriginCoordinates(tag));
        assertTrue(ConstructionMarkerLinkState.readOrigin(tag).isEmpty());
    }

    @Test
    void legacyCoordinatesCanBeSafelyBoundToTheCurrentDimension() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", 4);
        tag.putInt("y", 5);
        tag.putInt("z", 6);

        assertEquals(
            new ConstructionMarkerLinkState.Origin(4, 5, 6, "minecraft:overworld"),
            ConstructionMarkerLinkState.readOrigin(tag, "minecraft:overworld").orElseThrow()
        );
        assertTrue(ConstructionMarkerLinkState.readOrigin(tag).isEmpty());
    }

    @Test
    void reachIsInclusiveAtExactlySixtyFourBlocks() {
        assertTrue(ConstructionMarkerLinkState.withinReach(0, 0, 0, 64, 0, 0));
        assertTrue(ConstructionMarkerLinkState.withinReach(5, 9, -3, 5, -55, -3));
        assertFalse(ConstructionMarkerLinkState.withinReach(0, 0, 0, 64, 1, 0));
        assertFalse(ConstructionMarkerLinkState.withinReach(0, 0, 0, 65, 0, 0));
    }

    @Test
    void recoverableFailureKeepsRecordingStateForRetry() {
        CompoundTag tag = recordingTag();

        ConstructionMarkerLinkState.complete(
            tag,
            ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE
        );

        assertTrue(ConstructionMarkerLinkState.hasOriginCoordinates(tag));
        assertTrue(ConstructionMarkerLinkState.readOrigin(tag).isPresent());
    }

    @Test
    void successAndExplicitInvalidBlockCancelClearOnlyLinkKeys() {
        for (ConstructionMarkerLinkState.Completion completion : new ConstructionMarkerLinkState.Completion[] {
            ConstructionMarkerLinkState.Completion.SUCCESS,
            ConstructionMarkerLinkState.Completion.INVALID_BLOCK
        }) {
            CompoundTag tag = recordingTag();
            tag.putString("unrelated", "kept");

            ConstructionMarkerLinkState.complete(tag, completion);

            assertFalse(ConstructionMarkerLinkState.isRecording(tag));
            assertFalse(tag.contains("x"));
            assertFalse(tag.contains("y"));
            assertFalse(tag.contains("z"));
            assertFalse(tag.contains("dimension"));
            assertEquals("kept", tag.getString("unrelated"));
        }
    }

    private static CompoundTag recordingTag() {
        CompoundTag tag = new CompoundTag();
        ConstructionMarkerLinkState.start(tag, 1, 2, 3, "minecraft:overworld");
        return tag;
    }
}
