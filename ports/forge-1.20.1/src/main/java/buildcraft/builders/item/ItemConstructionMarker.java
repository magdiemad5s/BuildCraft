/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.item;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileConstructionMarker;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The construction marker doubles as the classic composite-blueprint linking tool.
 *
 * <p>The released item stored the selected Architect Table directly under the item root as {@code x}, {@code y}, and
 * {@code z}. Those keys remain authoritative here. The short-lived {@code recording} boolean from the early 1.20.1
 * port is accepted only as display/migration evidence; it is never sufficient to resolve an endpoint.</p>
 */
public class ItemConstructionMarker extends BlockItem {
    private static final String TAG_RECORDING = "recording";

    public ItemConstructionMarker(Properties properties) {
        super(BCBuildersBlocks.CONSTRUCTION_MARKER.get(), properties);
    }

    public static boolean isRecording(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && ConstructionMarkerLinkState.isRecording(tag);
    }

    /** Compatibility helper retained for callers from the initial port. */
    public static void setRecording(ItemStack stack, boolean recording) {
        if (recording) {
            stack.getOrCreateTag().putBoolean(TAG_RECORDING, true);
        } else if (stack.hasTag()) {
            stack.getTag().remove(TAG_RECORDING);
            if (stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getTag();
        boolean hasOrigin = tag != null && ConstructionMarkerLinkState.hasOriginCoordinates(tag);
        BlockEntity clicked = context.getLevel().getBlockEntity(context.getClickedPos());

        // Item interaction is evaluated on both logical sides. Only the server may change the link or item NBT.
        if (context.getLevel().isClientSide) {
            if (hasOrigin || clicked instanceof TileArchitectTable) {
                return InteractionResult.SUCCESS;
            }
            return super.useOn(context);
        }

        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.FAIL;
        }

        // A stale "recording" flag from the incomplete port has no endpoint coordinates. Remove it before deciding
        // whether this click starts a real link or performs normal BlockItem placement.
        if (!hasOrigin && tag != null && tag.contains(TAG_RECORDING)) {
            tag.remove(TAG_RECORDING);
            removeEmptyTag(stack);
        }

        if (!hasOrigin) {
            if (!(clicked instanceof TileArchitectTable architect)) {
                return super.useOn(context);
            }
            if (!architect.canPlayerEdit(player) || !architect.isBlueprintCopyMode()) {
                notify(player, "item.buildcraftbuilders.marker_construction.link.invalid_architect");
                return InteractionResult.FAIL;
            }

            ConstructionMarkerLinkState.start(
                stack.getOrCreateTag(),
                context.getClickedPos().getX(),
                context.getClickedPos().getY(),
                context.getClickedPos().getZ(),
                level.dimension().location().toString()
            );
            notify(player, "item.buildcraftbuilders.marker_construction.link.started");
            return InteractionResult.CONSUME;
        }

        CompoundTag currentTag = stack.getOrCreateTag();
        String currentDimension = level.dimension().location().toString();
        ConstructionMarkerLinkState.Origin origin = ConstructionMarkerLinkState.readOrigin(currentTag, currentDimension).orElseThrow();

        if (!origin.dimension().equals(currentDimension)) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.wrong_dimension");
            return InteractionResult.FAIL;
        }

        if (!ConstructionMarkerLinkState.withinReach(
            origin.x(), origin.y(), origin.z(),
            context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()
        )) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.too_far");
            return InteractionResult.FAIL;
        }

        net.minecraft.core.BlockPos architectPos =
            new net.minecraft.core.BlockPos(origin.x(), origin.y(), origin.z());
        if (!level.hasChunkAt(architectPos) || !level.hasChunkAt(context.getClickedPos())) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.unloaded");
            return InteractionResult.FAIL;
        }

        BlockEntity rootBlockEntity = level.getBlockEntity(architectPos);
        if (!(rootBlockEntity instanceof TileArchitectTable architect)) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.INVALID_BLOCK);
            notify(player, "item.buildcraftbuilders.marker_construction.link.invalid_architect");
            return InteractionResult.FAIL;
        }
        if (!architect.isBlueprintCopyMode() || !architect.canPlayerEdit(player)) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.invalid_architect");
            return InteractionResult.FAIL;
        }

        if (!isLinkTarget(clicked) || clicked == architect) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.INVALID_BLOCK);
            notify(player, "item.buildcraftbuilders.marker_construction.link.invalid_target");
            return InteractionResult.FAIL;
        }
        if (!canPlayerEdit(clicked, player)) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.invalid_target");
            return InteractionResult.FAIL;
        }

        if (!architect.addSubBlueprint(context.getClickedPos())) {
            finishLinkState(stack, ConstructionMarkerLinkState.Completion.RECOVERABLE_FAILURE);
            notify(player, "item.buildcraftbuilders.marker_construction.link.duplicate");
            return InteractionResult.FAIL;
        }

        finishLinkState(stack, ConstructionMarkerLinkState.Completion.SUCCESS);
        notify(player, "item.buildcraftbuilders.marker_construction.link.complete");
        return InteractionResult.CONSUME;
    }

    private static boolean isLinkTarget(BlockEntity blockEntity) {
        return blockEntity instanceof TileArchitectTable
            || blockEntity instanceof TileBuilder
            || blockEntity instanceof TileConstructionMarker;
    }

    private static boolean canPlayerEdit(BlockEntity blockEntity, Player player) {
        return blockEntity instanceof TileBC_Neptune tile && tile.canPlayerEdit(player);
    }

    private static void finishLinkState(ItemStack stack, ConstructionMarkerLinkState.Completion completion) {
        if (stack.hasTag()) {
            ConstructionMarkerLinkState.complete(stack.getTag(), completion);
            removeEmptyTag(stack);
        }
    }

    private static void removeEmptyTag(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
    }

    private static void notify(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
