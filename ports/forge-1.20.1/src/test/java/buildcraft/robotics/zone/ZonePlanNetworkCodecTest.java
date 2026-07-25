package buildcraft.robotics.zone;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

class ZonePlanNetworkCodecTest {
    @Test
    void rejectsNegativeAndOversizedChunkCounts() {
        FriendlyByteBuf negative = new FriendlyByteBuf(Unpooled.buffer());
        negative.writeInt(-1);
        assertThrows(DecoderException.class, () -> new ZonePlan().readFromByteBuf(negative));
        negative.release();

        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeInt(ZoneMapRequestPolicy.MAX_ZONE_CHUNKS_PER_EDIT + 1);
        assertThrows(DecoderException.class, () -> new ZonePlan().readFromByteBuf(oversized));
        oversized.release();
    }

    @Test
    void rejectsDuplicateChunksTransactionally() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeInt(2);
        writeEmptyChunk(buffer, 3, 4);
        writeEmptyChunk(buffer, 3, 4);

        assertThrows(DecoderException.class, () -> new ZonePlan().readFromByteBuf(buffer));
        buffer.release();
    }

    @Test
    void rejectsOversizedOrContradictoryChunkBits() {
        FriendlyByteBuf oversizedBits = new FriendlyByteBuf(Unpooled.buffer());
        oversizedBits.writeByte(1);
        oversizedBits.writeVarInt(ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES + 1);
        oversizedBits.writeZero(ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES + 1);
        assertThrows(DecoderException.class, () -> new ZoneChunk().readFromByteBuf(oversizedBits));
        oversizedBits.release();

        FriendlyByteBuf contradictory = new FriendlyByteBuf(Unpooled.buffer());
        contradictory.writeByte(3);
        contradictory.writeByteArray(new byte[] { 1 });
        assertThrows(DecoderException.class, () -> new ZoneChunk().readFromByteBuf(contradictory));
        contradictory.release();
    }

    private static void writeEmptyChunk(FriendlyByteBuf buffer, int chunkX, int chunkZ) {
        buffer.writeInt(chunkX);
        buffer.writeInt(chunkZ);
        buffer.writeByte(0);
    }
}
