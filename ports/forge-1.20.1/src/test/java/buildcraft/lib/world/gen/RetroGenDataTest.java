package buildcraft.lib.world.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

class RetroGenDataTest {
    @Test
    void readsAndRoundTripsTheLegacyRegistryAndDynamicChunkKeys() {
        CompoundTag input = new CompoundTag();
        ListTag registry = new ListTag();
        registry.add(StringTag.valueOf("buildcraftenergy:oil"));
        registry.add(StringTag.valueOf("buildcraftcore:spring"));
        input.put("registry", registry);

        CompoundTag chunks = new CompoundTag();
        chunks.putByteArray("12,-7", new byte[] { 1, 0 });
        input.put("data", chunks);

        RetroGenData loaded = RetroGenData.load(input);
        assertTrue(loaded.hasGenerated(new ChunkPos(12, -7), "buildcraftenergy:oil"));
        assertTrue(loaded.hasGenerated(new ChunkPos(12, -7), "buildcraftcore:spring"));
        assertFalse(loaded.hasGenerated(new ChunkPos(0, 0), "buildcraftenergy:oil"));

        CompoundTag encoded = loaded.save(new CompoundTag());
        RetroGenData roundTripped = RetroGenData.load(encoded);
        assertEquals(
            Set.of("buildcraftenergy:oil", "buildcraftcore:spring"),
            roundTripped.snapshot().get(new ChunkPos(12, -7))
        );
        assertEquals(Tag.TAG_STRING, encoded.getList("registry", Tag.TAG_STRING).getElementType());
        assertTrue(encoded.getCompound("data").contains("12,-7", Tag.TAG_BYTE_ARRAY));
    }

    @Test
    void invalidLegacyEntriesAreIgnoredWithoutDiscardingValidOnes() {
        CompoundTag input = new CompoundTag();
        ListTag registry = new ListTag();
        registry.add(StringTag.valueOf("valid"));
        input.put("registry", registry);

        CompoundTag chunks = new CompoundTag();
        chunks.putByteArray("bad", new byte[] { 0 });
        chunks.putByteArray("1,2", new byte[] { 0, 4 });
        input.put("data", chunks);

        RetroGenData loaded = RetroGenData.load(input);
        assertEquals(Set.of("valid"), loaded.snapshot().get(new ChunkPos(1, 2)));
        assertEquals(1, loaded.snapshot().size());
    }

    @Test
    void markingGenerationIsIdempotentAndDirty() {
        RetroGenData data = new RetroGenData();
        ChunkPos pos = new ChunkPos(-4, 9);
        data.markGenerated(pos, "oil");
        data.markGenerated(pos, "oil");

        assertTrue(data.isDirty());
        assertEquals(Set.of("oil"), data.snapshot().get(pos));
    }

    @Test
    void chunkKeyCodecRejectsMalformedCoordinates() {
        assertEquals(new ChunkPos(-1, 42), RetroGenData.deserializeChunkPos("-1,42"));
        assertNull(RetroGenData.deserializeChunkPos("1"));
        assertNull(RetroGenData.deserializeChunkPos("1,2,3"));
        assertNull(RetroGenData.deserializeChunkPos("a,2"));
    }
}