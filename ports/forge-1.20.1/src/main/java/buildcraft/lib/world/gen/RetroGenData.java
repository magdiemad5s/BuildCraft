/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.world.gen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import buildcraft.api.core.BCLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Compatibility codec for the legacy {@code buildcraft_world_gen.dat} file.
 * Registry names and per-chunk byte arrays intentionally retain the 1.12.2
 * schema so backed-up worlds can round-trip without losing generation marks.
 */
public final class RetroGenData extends SavedData {
    public static final String NAME = "buildcraft_world_gen";
    private static final int MAX_LEGACY_REGISTRY_SIZE = 256;

    private final Map<ChunkPos, Set<String>> generatedChunks = new HashMap<>();

    public RetroGenData() {
    }

    public static RetroGenData load(CompoundTag tag) {
        RetroGenData result = new RetroGenData();
        ListTag registry = tag.getList("registry", Tag.TAG_STRING);
        List<String> names = new ArrayList<>(registry.size());
        for (int index = 0; index < registry.size(); index++) {
            names.add(registry.getString(index));
        }

        CompoundTag data = tag.getCompound("data");
        for (String key : data.getAllKeys()) {
            ChunkPos pos = deserializeChunkPos(key);
            if (pos == null) {
                BCLog.logger.warn("Ignoring invalid BuildCraft world-generation chunk key: " + key);
                continue;
            }
            Set<String> generated = new HashSet<>();
            for (byte rawId : data.getByteArray(key)) {
                int id = Byte.toUnsignedInt(rawId);
                if (id >= names.size()) {
                    BCLog.logger.warn(
                        "Ignoring invalid BuildCraft world-generation registry id " + id + " for chunk " + key
                    );
                    continue;
                }
                generated.add(names.get(id));
            }
            if (!generated.isEmpty()) {
                result.generatedChunks.put(pos, generated);
            }
        }
        return result;
    }

    public static RetroGenData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RetroGenData::load, RetroGenData::new, NAME);
    }

    public boolean hasGenerated(ChunkPos pos, String generatorName) {
        Set<String> generated = generatedChunks.get(pos);
        return generated != null && generated.contains(generatorName);
    }

    public void markGenerated(ChunkPos pos, String generatorName) {
        if (generatorName == null || generatorName.isBlank()) {
            throw new IllegalArgumentException("generatorName cannot be blank");
        }
        if (generatedChunks.computeIfAbsent(pos, ignored -> new HashSet<>()).add(generatorName)) {
            setDirty();
        }
    }

    public Map<ChunkPos, Set<String>> snapshot() {
        Map<ChunkPos, Set<String>> copy = new LinkedHashMap<>();
        generatedChunks.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<ChunkPos, Set<String>> entry) -> entry.getKey().x)
                .thenComparingInt(entry -> entry.getKey().z))
            .forEach(entry -> copy.put(entry.getKey(), Set.copyOf(entry.getValue())));
        return Map.copyOf(copy);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        List<String> names = generatedChunks.values().stream()
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .toList();
        if (names.size() > MAX_LEGACY_REGISTRY_SIZE) {
            throw new IllegalStateException(
                "The legacy BuildCraft world-generation format can encode at most " + MAX_LEGACY_REGISTRY_SIZE
                    + " generator names, but found " + names.size()
            );
        }

        Map<String, Integer> ids = new HashMap<>();
        ListTag registry = new ListTag();
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            ids.put(name, index);
            registry.add(StringTag.valueOf(name));
        }
        tag.put("registry", registry);

        CompoundTag data = new CompoundTag();
        for (Map.Entry<ChunkPos, Set<String>> entry : generatedChunks.entrySet()) {
            List<String> generated = entry.getValue().stream().sorted().toList();
            byte[] encoded = new byte[generated.size()];
            for (int index = 0; index < generated.size(); index++) {
                encoded[index] = (byte) (int) ids.get(generated.get(index));
            }
            data.putByteArray(serializeChunkPos(entry.getKey()), encoded);
        }
        tag.put("data", data);
        return tag;
    }

    static ChunkPos deserializeChunkPos(String key) {
        String[] parts = key.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String serializeChunkPos(ChunkPos pos) {
        return pos.x + "," + pos.z;
    }
}