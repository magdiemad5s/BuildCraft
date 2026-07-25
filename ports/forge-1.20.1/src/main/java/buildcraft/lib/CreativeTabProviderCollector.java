/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated provider assembly logic used by BuildCraft's creative tabs. */
final class CreativeTabProviderCollector {
    private CreativeTabProviderCollector() {
    }

    static <T> void collect(
        String tabName,
        Iterable<? extends Supplier<? extends Collection<T>>> providers,
        Consumer<T> output,
        boolean failFast,
        FailureReporter reporter
    ) {
        for (Supplier<? extends Collection<T>> provider : providers) {
            try {
                Collection<T> provided = provider.get();
                if (provided == null) {
                    throw new IllegalStateException("Creative-tab provider returned null");
                }
                provided.forEach(output);
            } catch (RuntimeException exception) {
                reporter.report(tabName, provider, exception);
                if (failFast) {
                    String providerName = provider.getClass().getName();
                    throw new IllegalStateException(
                        "Failed to populate creative tab '" + tabName + "' from provider '" + providerName + "'",
                        exception
                    );
                }
            }
        }
    }

    @FunctionalInterface
    interface FailureReporter {
        void report(String tabName, Supplier<?> provider, RuntimeException exception);
    }
}