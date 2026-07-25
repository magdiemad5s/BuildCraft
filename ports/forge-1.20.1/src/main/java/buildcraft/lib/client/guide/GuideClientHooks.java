/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.client.guide;

import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideDocument;
import buildcraft.lib.guide.GuideIndex;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client-only item-to-screen entry points. */
@OnlyIn(Dist.CLIENT)
public final class GuideClientHooks {
    private GuideClientHooks() {
    }

    public static void openGuide(String bookName) {
        Minecraft minecraft = Minecraft.getInstance();
        GuideIndex index = loadedIndex(minecraft);
        minecraft.setScreen(GuideScreen.guide(index, bookName));
    }

    public static void openNote(String noteId) {
        Minecraft minecraft = Minecraft.getInstance();
        GuideIndex index = loadedIndex(minecraft);
        GuideDocument document = index.lookup(noteId);
        if (document == null) {
            minecraft.setScreen(GuideScreen.guide(index, GuideContracts.DEFAULT_BOOK));
        } else {
            minecraft.setScreen(GuideScreen.note(index, document));
        }
    }

    private static GuideIndex loadedIndex(Minecraft minecraft) {
        GuideIndex index = GuideRepository.INSTANCE.current();
        if (index.isEmpty()) {
            index = GuideRepository.INSTANCE.loadNow(minecraft.getResourceManager());
        }
        return index;
    }
}
