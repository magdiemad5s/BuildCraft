/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import buildcraft.lib.client.guide.GuideClientHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Physical-side boundary for guide screens.
 *
 * <p>Common item classes call this bridge without directly linking Minecraft
 * client GUI classes. The client implementation is only executed on the
 * physical client, keeping item registration safe on dedicated servers.</p>
 */
public final class GuideClientBridge {
    private GuideClientBridge() {
    }

    public static void openGuide(String bookName) {
        DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> GuideClientHooks.openGuide(bookName)
        );
    }

    public static void openNote(String noteId) {
        DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> GuideClientHooks.openNote(noteId)
        );
    }
}
