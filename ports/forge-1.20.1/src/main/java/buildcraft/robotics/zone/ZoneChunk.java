/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec2;

public class ZoneChunk {
    public BitSet property;
    private boolean fullSet = false;

    public ZoneChunk() {}

    public ZoneChunk(ZoneChunk old) {
        fullSet = old.fullSet;
        if (old.property != null) {
            property = BitSet.valueOf(old.property.toLongArray());
        }
    }

    public boolean get(int xChunk, int zChunk) {
        if (fullSet) {
            return true;
        }
        return property != null && property.get(xChunk + zChunk * 16);
    }

    public void set(int xChunk, int zChunk, boolean value) {
        if (value) {
            if (fullSet) {
                return;
            }

            if (property == null) {
                property = new BitSet(16 * 16);
            }

            property.set(xChunk + zChunk * 16, true);

            if (property.cardinality() >= 16 * 16) {
                property = null;
                fullSet = true;
            }
        } else {
            if (fullSet) {
                property = new BitSet(16 * 16);
                property.flip(0, 16 * 16);
                fullSet = false;
            } else if (property == null) {
                // Note - ZonePlan should usually destroy such chunks
                property = new BitSet(16 * 16);
            }

            property.set(xChunk + zChunk * 16, false);
        }
    }

    public List<Vec2> getAll() {
        ImmutableList.Builder<Vec2> builder = ImmutableList.builder();
        for (int zChunk = 0; zChunk < 16; zChunk++) {
            for (int xChunk = 0; xChunk < 16; xChunk++) {
                if (get(xChunk, zChunk)) {
                    builder.add(new Vec2(xChunk, zChunk));
                }
            }
        }
        return builder.build();
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("fullSet", fullSet);

        if (property != null) {
            nbt.putByteArray("bits", property.toByteArray());
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        fullSet = nbt.getBoolean("fullSet");
        property = null;
        if (!fullSet && nbt.contains("bits")) {
            byte[] bits = nbt.getByteArray("bits");
            property = BitSet.valueOf(Arrays.copyOf(bits, Math.min(bits.length, ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES)));
        }
    }

    public BlockPos getRandomBlockPos(Random rand) {
        int x, z;

        if (fullSet) {
            x = rand.nextInt(16);
            z = rand.nextInt(16);
        } else {
            int bitId = rand.nextInt(property.cardinality());
            int bitPosition = property.nextSetBit(0);

            while (bitId > 0) {
                bitId--;

                bitPosition = property.nextSetBit(bitPosition + 1);
            }

            z = bitPosition / 16;
            x = bitPosition - 16 * z;
        }
        int y = rand.nextInt(255);

        return new BlockPos(x, y, z);
    }

    public boolean isEmpty() {
        return !fullSet && (property == null || property.isEmpty());
    }

    public ZoneChunk readFromByteBuf(FriendlyByteBuf buf) {
        int flags = buf.readUnsignedByte();
        if ((flags & ~3) != 0) {
            throw new DecoderException("Invalid Zone Planner chunk flags: " + flags);
        }
        BitSet decodedProperty = null;
        if ((flags & 1) != 0) {
            decodedProperty = BitSet.valueOf(buf.readByteArray(ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES));
        }
        boolean decodedFullSet = (flags & 2) != 0;
        if (decodedFullSet && decodedProperty != null) {
            throw new DecoderException("A Zone Planner chunk cannot be both full and bit-backed");
        }
        property = decodedProperty;
        fullSet = decodedFullSet;
        return this;
    }

    public int getNetworkEncodedSize() {
        if (fullSet || property == null) {
            return 1;
        }
        return 2 + Math.min(property.toByteArray().length, ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES);
    }

    public void writeToByteBuf(FriendlyByteBuf buf) {
        byte[] bits = fullSet || property == null
            ? null
            : Arrays.copyOf(
                property.toByteArray(),
                Math.min(property.toByteArray().length, ZoneMapRequestPolicy.MAX_ZONE_CHUNK_BYTES)
            );
        int flags = (fullSet ? 2 : 0) | (bits != null ? 1 : 0);
        buf.writeByte(flags);
        if (bits != null) {
            buf.writeByteArray(bits);
        }
    }
}
