package buildcraft.transport.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import buildcraft.api.transport.EnumWirePart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;

class WireNetworkIdentityTest {
    private static ImmutableList<WireSystem.WireElement> elements() {
        return ImmutableList.of(
            new WireSystem.WireElement(BlockPos.ZERO, EnumWirePart.EAST_UP_SOUTH),
            new WireSystem.WireElement(new BlockPos(1, 0, 0), EnumWirePart.WEST_UP_SOUTH)
        );
    }

    @Test
    void serverSystemsUseMonotonicIdentityInsteadOfTheirCollidableHash() {
        WireSystem first = new WireSystem(elements(), null);
        WireSystem second = new WireSystem(elements(), null);

        assertEquals(first.hashCode(), second.hashCode(), "precondition: systems intentionally have equal hashes");
        assertEquals(first.getWiresHashCode(), second.getWiresHashCode(), "precondition: old packet IDs collide");
        assertNotEquals(first.networkId, second.networkId, "network identity must never come from either hash");
    }

    @Test
    void clientPacketConstructorPreservesTheServerIdentity() {
        WireSystem decoded = new WireSystem(0x13572468, elements(), null);
        assertEquals(0x13572468, decoded.networkId);
    }

    @Test
    void nbtRoundTripPreservesElementOrderAndNetworkEquality() {
        WireSystem original = new WireSystem(elements(), DyeColor.RED);

        WireSystem restored = new WireSystem(original.writeToNBT());

        assertEquals(original.elements, restored.elements);
        assertEquals(original, restored);
    }
}