/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.item;

import java.util.List;

import buildcraft.lib.guide.GuideClientBridge;
import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideNbtCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A portable link to a guide page.
 *
 * <p>Old stacks store the page identifier under {@code note_id}; populated
 * notes open that document directly, while blank notes open the searchable
 * guide contents instead of presenting an inert placeholder.</p>
 */
public final class ItemGuideNote extends Item {
    public ItemGuideNote(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            GuideClientBridge.openNote(getNoteId(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Level level,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        String noteId = getNoteId(stack);
        if (!noteId.isBlank()) {
            tooltip.add(
                Component.translatable(
                    "item.buildcraftlib.guide_note.target",
                    Component.literal(noteId)
                ).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    public static String getNoteId(ItemStack stack) {
        return GuideNbtCodec.getNoteId(stack.getTag());
    }

    public static void setNoteId(ItemStack stack, String noteId) {
        if (noteId == null || noteId.isBlank()) {
            GuideNbtCodec.setNoteId(stack.getTag(), noteId);
        } else {
            GuideNbtCodec.setNoteId(stack.getOrCreateTag(), noteId);
        }
    }

    public ItemStack storeNoteId(String noteId) {
        ItemStack stack = new ItemStack(this);
        setNoteId(stack, noteId);
        return stack;
    }
}
