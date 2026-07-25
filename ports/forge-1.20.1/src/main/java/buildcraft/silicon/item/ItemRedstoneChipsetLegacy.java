/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.silicon.item;

import java.util.function.Consumer;

import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.lib.item.ICreativeTabItemProvider;
import buildcraft.lib.item.LegacyItemMetadata;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The single legacy {@code redstone_chipset} item and its metadata variants. */
public final class ItemRedstoneChipsetLegacy extends Item implements ICreativeTabItemProvider {
    public ItemRedstoneChipsetLegacy(Properties properties) {
        super(properties.stacksTo(16));
    }

    public EnumRedstoneChipset getType(ItemStack stack) {
        int ordinal = LegacyItemMetadata.boundedOrdinal(
            LegacyItemMetadata.get(stack),
            EnumRedstoneChipset.values().length,
            EnumRedstoneChipset.RED.ordinal()
        );
        return EnumRedstoneChipset.values()[ordinal];
    }

    public ItemStack getStack(EnumRedstoneChipset type, int count) {
        return LegacyItemMetadata.create(this, count, type.ordinal());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
            "item.buildcraftsilicon.redstone_" + getType(stack).getSerializedName() + "_chipset"
        );
    }

    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
        for (EnumRedstoneChipset type : EnumRedstoneChipset.values()) {
            output.accept(getStack(type, 1));
        }
    }
}
