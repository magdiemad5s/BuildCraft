/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.item;

import buildcraft.lib.guide.GuideClientBridge;
import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideNbtCodec;
import buildcraft.lib.misc.AdvancementUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The functional BuildCraft guide book, retaining its legacy item/NBT contract. */
public final class ItemGuide extends Item {
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftcore", "guide");

    public ItemGuide(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            GuideClientBridge.openGuide(getBookName(stack));
        } else {
            AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return GuideContracts.META_BOOK.equals(getBookName(stack))
            ? Component.translatable("buildcraft.guide.book.meta.name")
            : Component.translatable("item.buildcraftlib.guide");
    }

    /** The original guide acted as its own crafting container item. */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    /** Preserve the exact guide variant/NBT when it is used as a recipe tool. */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        remaining.setCount(1);
        return remaining;
    }

    public static String getBookName(ItemStack stack) {
        return GuideNbtCodec.getBookName(stack.getTag());
    }

    public static void setBookName(ItemStack stack, String book) {
        if (book == null || book.isBlank() || GuideContracts.DEFAULT_BOOK.equals(book)) {
            GuideNbtCodec.setBookName(stack.getTag(), book);
            return;
        }
        GuideNbtCodec.setBookName(stack.getOrCreateTag(), book);
    }
}
