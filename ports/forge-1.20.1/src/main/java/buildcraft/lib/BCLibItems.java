/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib;

import java.util.ArrayList;
import java.util.List;

import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.item.ItemGuide;
import buildcraft.lib.item.ItemGuideNote;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Library-owned items whose backing systems are present in the 1.20.1 port. */
public final class BCLibItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, BCLib.MODID);

    public static final RegistryObject<ItemGuide> GUIDE = ITEMS.register(
        GuideContracts.GUIDE_ITEM_PATH, () -> new ItemGuide(new Item.Properties())
    );
    public static final RegistryObject<ItemGuideNote> GUIDE_NOTE = ITEMS.register(
        GuideContracts.GUIDE_NOTE_ITEM_PATH, () -> new ItemGuideNote(new Item.Properties())
    );
    public static final RegistryObject<ItemDebugger> DEBUGGER = ITEMS.register(
        "debugger", () -> new ItemDebugger("item.debugger", new Item.Properties().stacksTo(1))
    );

    private BCLibItems() {
    }

    public static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static List<ItemStack> getCreativeTabItems() {
        List<ItemStack> items = new ArrayList<>();
        if (GUIDE.isPresent()) {
            items.add(GUIDE.get().getDefaultInstance());
            ItemStack configurationGuide = GUIDE.get().getDefaultInstance();
            ItemGuide.setBookName(configurationGuide, GuideContracts.META_BOOK);
            items.add(configurationGuide);
        }
        if (GUIDE_NOTE.isPresent()) {
            items.add(GUIDE_NOTE.get().getDefaultInstance());
        }
        if (DEBUGGER.isPresent()) {
            items.add(DEBUGGER.get().getDefaultInstance());
        }
        return List.copyOf(items);
    }
}
