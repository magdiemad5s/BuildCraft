/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.cap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

/** Provides a simple way of mapping {@link Capability}'s to instances. Also allows for additional providers. */
public class CapabilityHelper implements ICapabilityProvider {
    private final Map<EnumPipePart, Map<Capability<?>, NonNullSupplier<?>>> caps = new EnumMap<>(EnumPipePart.class);
    private final Map<EnumPipePart, Map<Capability<?>, LazyOptional<?>>> cachedCaps =
        new EnumMap<>(EnumPipePart.class);
    private final List<ICapabilityProvider> additional = new ArrayList<>();
    private boolean valid = true;

    public CapabilityHelper() {
        for (EnumPipePart face : EnumPipePart.VALUES) {
            caps.put(face, new HashMap<>());
            cachedCaps.put(face, new HashMap<>());
        }
    }

    private EnumPipePart getPart(Direction facing) {
        return EnumPipePart.fromFacing(facing);
    }

    public <T> void addCapabilityInstance(@Nullable Capability<T> cap, T instance, EnumPipePart... parts) {
        NonNullSupplier<T> supplier = () -> instance;
        addCapability(cap, supplier, parts);
    }

    public <T> void addCapability(@Nullable Capability<T> cap, NonNullSupplier<T> getter, EnumPipePart... parts) {
        if (cap == null) {
            return;
        }
        for (EnumPipePart part : parts) {
            caps.get(part).put(cap, getter);
            invalidateCached(part, cap);
        }
    }

    public <T> void addCapability(@Nullable Capability<T> cap, Function<Direction, T> getter, EnumPipePart... parts) {
        if (cap == null) {
            return;
        }
        for (EnumPipePart part : parts) {
            caps.get(part).put(cap, () -> getter.apply(part.face));
            invalidateCached(part, cap);
        }
    }

    public <T extends ICapabilityProvider> T addProvider(T provider) {
        if (provider != null) {
            additional.add(provider);
            invalidateAllCached();
        }
        return provider;
    }

    /**
     * Invalidates every capability handle previously exposed by this helper.
     *
     * <p>A Forge {@link LazyOptional} is terminal once invalidated. Caching the handles here gives callers a stable
     * identity while the owning block entity is alive, and lets {@link #revive()} create fresh handles after Forge
     * revives the block entity.</p>
     */
    public void invalidate() {
        valid = false;
        invalidateAllCached();
    }

    /** Allows fresh capability handles to be created after the owning provider has been revived. */
    public void revive() {
        valid = true;
        invalidateAllCached();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (!valid) {
            return LazyOptional.empty();
        }

        EnumPipePart part = getPart(facing);
        Map<Capability<?>, LazyOptional<?>> cachedMap = cachedCaps.get(part);
        LazyOptional<?> cached = cachedMap.get(capability);
        if (cached != null) {
            return cached.cast();
        }

        NonNullSupplier<?> supplier = caps.get(part).get(capability);
        if (supplier != null) {
            Object instance = supplier.get();
            if (instance == null) {
                return LazyOptional.empty();
            }
            LazyOptional<?> optional = LazyOptional.of(() -> instance);
            cachedMap.put(capability, optional);
            return optional.cast();
        }

        for (ICapabilityProvider provider : additional) {
            LazyOptional<T> provided = provider.getCapability(capability, facing);
            if (provided == null) {
                BCLog.logger.warn("Capability provider {} returned null for {}", provider, capability);
                continue;
            }
            if (provided.isPresent()) {
                T instance = provided.orElseThrow(
                    () -> new IllegalStateException("Present capability did not resolve: " + capability)
                );
                LazyOptional<T> optional = LazyOptional.of(() -> instance);
                cachedMap.put(capability, optional);
                return optional;
            }
        }
        return LazyOptional.empty();
    }

    private void invalidateCached(EnumPipePart part, Capability<?> capability) {
        LazyOptional<?> optional = cachedCaps.get(part).remove(capability);
        if (optional != null) {
            optional.invalidate();
        }
    }

    private void invalidateAllCached() {
        for (Map<Capability<?>, LazyOptional<?>> byCapability : cachedCaps.values()) {
            for (LazyOptional<?> optional : byCapability.values()) {
                optional.invalidate();
            }
            byCapability.clear();
        }
    }
}