package buildcraft.transport.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;

class WirePersistenceValidationTest {
    private static final EnumWirePart PART = EnumWirePart.EAST_UP_SOUTH;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void malformedAndOddWirePartArraysAreSanitizedPerEntry() {
        int[] encoded = {
            PART.ordinal(), DyeColor.RED.getId(),
            -1, DyeColor.BLUE.getId(),
            PART.ordinal(), 99,
            PART.ordinal()
        };

        Map<EnumWirePart, DyeColor> decoded = WireManager.decodeParts(encoded);

        assertEquals(1, decoded.size());
        assertEquals(DyeColor.RED, decoded.get(PART));
    }

    @Test
    void malformedSavedElementOrdinalsAndPositionsAreRejected() {
        CompoundTag invalidType = validSystemTag();
        invalidType.getList("elements", Tag.TAG_COMPOUND).getCompound(0).putInt("type", 99);
        assertThrows(IllegalArgumentException.class, () -> new WireSystem(invalidType));

        CompoundTag invalidPart = validSystemTag();
        invalidPart.getList("elements", Tag.TAG_COMPOUND).getCompound(0).putInt("wirePart", 99);
        assertThrows(IllegalArgumentException.class, () -> new WireSystem(invalidPart));

        CompoundTag invalidPosition = validSystemTag();
        invalidPosition.getList("elements", Tag.TAG_COMPOUND).getCompound(0)
            .putIntArray("blockPos", new int[] { 1, 2 });
        assertThrows(IllegalArgumentException.class, () -> new WireSystem(invalidPosition));

        CompoundTag invalidColour = validSystemTag();
        invalidColour.putInt("color", 99);
        assertThrows(IllegalArgumentException.class, () -> new WireSystem(invalidColour));
    }

    @Test
    void malformedSavedSystemDoesNotDiscardFollowingValidSystem() {
        CompoundTag savedData = new CompoundTag();
        ListTag entries = new ListTag();

        CompoundTag malformedEntry = new CompoundTag();
        CompoundTag malformedSystem = validSystemTag();
        malformedSystem.getList("elements", Tag.TAG_COMPOUND).getCompound(0).putInt("wirePart", -1);
        malformedEntry.put("wireSystem", malformedSystem);
        malformedEntry.putBoolean("powered", true);
        entries.add(malformedEntry);

        CompoundTag validEntry = new CompoundTag();
        validEntry.put("wireSystem", validSystemTag());
        validEntry.putBoolean("powered", false);
        entries.add(validEntry);
        savedData.put("entries", entries);

        WorldSavedDataWireSystems loaded = WorldSavedDataWireSystems.load(savedData);

        assertEquals(1, loaded.wireSystems.size());
        assertFalse(loaded.wireSystems.values().iterator().next());
        assertTrue(loaded.wireSystems.keySet().iterator().next().hasElement(
            new WireSystem.WireElement(BlockPos.ZERO, PART)
        ));
    }

    @Test
    void malformedNetworkElementOrdinalIsRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeInt(99);
            assertThrows(DecoderException.class, () -> new WireSystem.WireElement(buffer));
        } finally {
            buffer.release();
        }
    }

    private static CompoundTag validSystemTag() {
        WireSystem system = new WireSystem(
            ImmutableList.of(new WireSystem.WireElement(BlockPos.ZERO, PART)),
            DyeColor.RED
        );
        return system.writeToNBT();
    }
}
