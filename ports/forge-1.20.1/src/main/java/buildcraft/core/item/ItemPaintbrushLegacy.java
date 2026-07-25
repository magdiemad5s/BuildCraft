/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.core.item;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import buildcraft.api.blocks.CustomPaintHelper;
import buildcraft.lib.item.ICreativeTabItemProvider;
import buildcraft.lib.item.LegacyItemMetadata;
import buildcraft.lib.misc.SoundUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * One metadata-compatible paintbrush item with clean and sixteen colour
 * variants. Colour remains the legacy item metadata ({@code 0 = clean},
 * {@code 1..16 = DyeColor id + 1}); uses remain in the original lowercase
 * {@code damage} NBT field.
 */
public final class ItemPaintbrushLegacy extends Item implements ICreativeTabItemProvider {
    public static final String TAG_USES_DAMAGE = "damage";
    public static final int MAX_USES = 64;

    public ItemPaintbrushLegacy(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
        output.accept(getStack(null));
        for (DyeColor colour : DyeColor.values()) {
            output.accept(getStack(colour));
        }
    }

    public ItemStack getStack(@Nullable DyeColor colour) {
        return LegacyItemMetadata.create(this, 1, getMetadata(colour));
    }

    @Nullable
    public DyeColor getColour(ItemStack stack) {
        int metadata = LegacyItemMetadata.get(stack);
        return metadata >= 1 && metadata <= DyeColor.values().length
            ? DyeColor.byId(metadata - 1)
            : null;
    }

    public int getUsesLeft(ItemStack stack) {
        if (getColour(stack) == null) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        int used = tag != null && tag.contains(TAG_USES_DAMAGE, Tag.TAG_ANY_NUMERIC)
            ? tag.getInt(TAG_USES_DAMAGE)
            : 0;
        return MAX_USES - Mth.clamp(used, 0, MAX_USES);
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor colour = getColour(stack);
        String suffix = colour == null ? "clean" : colour.getSerializedName();
        return Component.translatable(getDescriptionId() + "." + suffix);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        DyeColor colour = getColour(stack);
        return colour != null && getUsesLeft(stack) < MAX_USES;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getUsesLeft(stack) / MAX_USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fraction = getUsesLeft(stack) / (float) MAX_USES;
        return Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        DyeColor colour = getColour(stack);

        if (colour != null && getUsesLeft(stack) <= 0) {
            return InteractionResult.FAIL;
        }

        InteractionResult result = CustomPaintHelper.INSTANCE.attemptPaintBlock(
            level,
            context.getClickedPos(),
            level.getBlockState(context.getClickedPos()),
            context.getClickLocation(),
            context.getClickedFace(),
            colour
        );
        if (!result.consumesAction()) {
            return result;
        }

        if (!level.isClientSide) {
            SoundUtil.playChangeColour(level, context.getClickedPos(), colour);
            if (player != null && !player.getAbilities().instabuild && colour != null) {
                consumeUse(stack);
            }
            if (player != null) {
                player.inventoryMenu.broadcastChanges();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean tryBrush(
        ItemStack stack,
        Level level,
        BlockPos pos,
        BlockState state,
        Vec3 hitPos,
        net.minecraft.core.Direction side,
        Player player
    ) {
        DyeColor colour = getColour(stack);
        if (colour != null && getUsesLeft(stack) <= 0) {
            return false;
        }
        InteractionResult result = CustomPaintHelper.INSTANCE.attemptPaintBlock(
            level, pos, state, hitPos, side, colour
        );
        if (!result.consumesAction()) {
            return false;
        }
        if (!level.isClientSide) {
            SoundUtil.playChangeColour(level, pos, colour);
            if (player != null && !player.getAbilities().instabuild && colour != null) {
                consumeUse(stack);
            }
        }
        return true;
    }

    private static int getMetadata(@Nullable DyeColor colour) {
        return colour == null ? 0 : colour.getId() + 1;
    }

    private static void consumeUse(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int used = Mth.clamp(tag.getInt(TAG_USES_DAMAGE), 0, MAX_USES) + 1;
        if (used >= MAX_USES) {
            tag.remove(TAG_USES_DAMAGE);
            LegacyItemMetadata.set(stack, 0);
        } else {
            tag.putInt(TAG_USES_DAMAGE, used);
        }
    }
}
