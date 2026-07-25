/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Initialises the vanilla registries required by unit tests that construct Minecraft blocks or items.
 */
public final class MinecraftTestBootstrap {
    private static boolean bootstrapped;

    private MinecraftTestBootstrap() {
    }

    public static synchronized void bootStrap() {
        if (bootstrapped) {
            return;
        }
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            if (!isForgeUnitTestNetworkBootstrapFailure(error)) {
                throw error;
            }
            // Forge reaches NetworkHooks after vanilla has already registered blocks and items. Plain JUnit does not
            // provide the transformed NetworkEvent constructor that the game runtime supplies, so confirm that the
            // now-complete vanilla bootstrap is usable and continue.
            Bootstrap.bootStrap();
        }
        bootstrapped = true;
    }

    private static boolean isForgeUnitTestNetworkBootstrapFailure(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof NoSuchMethodException
                    && String.valueOf(current.getMessage()).contains("net.minecraftforge.network.NetworkEvent.<init>()")) {
                return true;
            }
        }
        return false;
    }
}
