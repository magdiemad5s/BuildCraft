/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.Collections;
import java.util.List;

import buildcraft.api.template.TemplateApi;
import buildcraft.api.core.IStackFilter;
import buildcraft.api.core.BuildCraftAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.FakePlayer;

public class TemplateBuilder extends SnapshotBuilder<ITileForTemplateBuilder> {
    private static final IStackFilter PLACEABLE_BLOCK_FILTER = stack -> stack.getItem() instanceof BlockItem;

    public TemplateBuilder(ITileForTemplateBuilder tile) {
        super(tile);
    }

    @Override
    protected Template.BuildingInfo getBuildingInfo() {
        return tile.getTemplateBuildingInfo();
    }

    @Override
    protected boolean isAir(BlockPos blockPos) {
        return !getBuildingInfo().box.contains(blockPos) ||
            !getBuildingInfo().getSnapshot().data.get(
                getBuildingInfo().getSnapshot().posToIndex(
                    getBuildingInfo().fromWorld(blockPos)
                )
            );
    }

    @Override
    protected boolean canPlace(BlockPos blockPos) {
        return TemplateTargetPolicy.canPlaceInto(tile.getWorldBC().getBlockState(blockPos));
    }

    @Override
    protected boolean isReadyToPlace(BlockPos blockPos) {
        return true;
    }

    @Override
    protected boolean hasEnoughToPlaceItems(BlockPos blockPos) {
        // Templates need one sample block to define the material; creative mode reuses it without consuming it.
        return !tile.getInvResources().extract(PLACEABLE_BLOCK_FILTER, 1, 1, true).isEmpty();
    }

    @Override
    protected List<ItemStack> getToPlaceItems(BlockPos blockPos) {
        return Collections.singletonList(
            tile.getInvResources().extract(PLACEABLE_BLOCK_FILTER, 1, 1, !tile.needMeterial())
        );
    }

    @Override
    protected boolean doPlaceTask(PlaceTask placeTask) {
        FakePlayer fakePlayer = BuildCraftAPI.fakePlayerProvider.getFakePlayer(
            (ServerLevel) tile.getWorldBC(),
            tile.getOwner(),
            tile.getBuilderPos()
        );
        // Item-use handlers may mutate their argument. Preserve the reserved stack for exact cancellation refunds.
        ItemStack placementStack = placeTask.items.get(0).copy();
        return TemplateApi.templateRegistry.handle(
            tile.getWorldBC(),
            placeTask.pos,
            fakePlayer,
            placementStack
        );
    }

    @Override
    protected void cancelPlaceTask(PlaceTask placeTask) {
        super.cancelPlaceTask(placeTask);
        if (!placeTask.shouldRefundResources()) {
            return;
        }
        ItemStack remainder = tile.getInvResources().insert(placeTask.items.get(0), false, false);
        if (!remainder.isEmpty() && tile.getWorldBC() instanceof ServerLevel serverLevel) {
            Block.popResource(serverLevel, tile.getBuilderPos(), remainder);
        }
    }

    @Override
    protected boolean isBlockCorrect(BlockPos blockPos) {
        return !isAir(blockPos) && TemplateTargetPolicy.countsAsFilled(tile.getWorldBC().getBlockState(blockPos));
    }
}
