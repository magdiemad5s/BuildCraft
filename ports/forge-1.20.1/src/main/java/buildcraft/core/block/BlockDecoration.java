/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.core.block;

import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.core.BCCoreItems;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.item.MultiBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;

/**
 * The six legacy BuildCraft decoration blocks stored under the single
 * {@code buildcraftcore:decorated} registry identity.
 */
public class BlockDecoration extends BlockBCBase_Neptune {
    public BlockDecoration() {
        super(
            Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(BuildCraftProperties.DECORATED_BLOCK).lightValue)
        );
        registerDefaultState(
            stateDefinition.any().setValue(BuildCraftProperties.DECORATED_BLOCK, EnumDecoratedBlock.DESTROY)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BuildCraftProperties.DECORATED_BLOCK);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        if (context.getItemInHand().getItem() instanceof MultiBlockItem<?> multi
            && multi.getType(context.getItemInHand()) instanceof EnumDecoratedBlock type) {
            return state.setValue(BuildCraftProperties.DECORATED_BLOCK, type);
        }
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        BlockGetter level,
        BlockPos pos,
        Player player
    ) {
        return BCCoreItems.getDecoratedStack(state.getValue(BuildCraftProperties.DECORATED_BLOCK));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return BCCoreItems.getDecoratedStack(state.getValue(BuildCraftProperties.DECORATED_BLOCK));
    }
}