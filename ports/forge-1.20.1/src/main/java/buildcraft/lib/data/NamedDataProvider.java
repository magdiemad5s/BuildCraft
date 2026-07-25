/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.data;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/** Gives otherwise final-named vanilla data providers a stable module-specific identity. */
public final class NamedDataProvider implements DataProvider {
    private final String name;
    private final DataProvider delegate;

    private NamedDataProvider(String name, DataProvider delegate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Data provider name must not be blank");
        }
        this.name = name;
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static NamedDataProvider of(String name, DataProvider delegate) {
        return new NamedDataProvider(name, delegate);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return delegate.run(output);
    }

    @Override
    public String getName() {
        return name;
    }
}
