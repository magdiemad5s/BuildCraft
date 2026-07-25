package buildcraft.transport.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import buildcraft.api.transport.EnumWirePart;
import net.minecraft.world.level.block.Rotation;

class EnumWireBetweenRotationTest {
    @Test
    void everyWireSegmentRotatesItsEndpointsAndExternalDirectionExactly() {
        for (EnumWireBetween original : EnumWireBetween.VALUES) {
            for (Rotation rotation : Rotation.values()) {
                EnumWireBetween rotated = original.rotate(rotation);
                Set<EnumWirePart> expectedParts = Set.of(
                    original.parts[0].rotate(rotation),
                    original.parts[1].rotate(rotation)
                );

                assertEquals(expectedParts, Set.of(rotated.parts));
                assertEquals(
                    original.to == null ? null : rotation.rotate(original.to),
                    rotated.to
                );
            }
        }
    }

    @Test
    void inverseAndFullTurnReturnEverySegmentToItsOriginalIdentity() {
        for (EnumWireBetween original : EnumWireBetween.VALUES) {
            assertSame(
                original,
                original.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.COUNTERCLOCKWISE_90)
            );
            assertSame(
                original,
                original.rotate(Rotation.CLOCKWISE_90)
                    .rotate(Rotation.CLOCKWISE_90)
                    .rotate(Rotation.CLOCKWISE_90)
                    .rotate(Rotation.CLOCKWISE_90)
            );
            assertTrue(original.parts[0] != original.parts[1]);
        }
    }
}
