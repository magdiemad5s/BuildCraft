/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import java.util.List;


import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public abstract class BlockBCTile_Neptune extends BlockBCBase_Neptune implements EntityBlock {

	
    public BlockBCTile_Neptune(Properties material) {
        super(material);
    }
    
    public BlockBCTile_Neptune() {}


    @Override
	public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune tileBC) {
            tileBC.onExplode(explosion);
            // Forge generates explosion loot before invoking this callback, so that loot transaction owns contents.
            tileBC.handleRemoval(false);
        }
		super.onBlockExploded(state, level, pos, explosion);
	}
    

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
			FluidState fluid) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune tileBC) {
            // Internal contents are world-owned even when vanilla suppresses block loot (creative or a wrong tool).
            // The guarded handler prevents the subsequent onRemove/getDrops sequence from duplicating them.
            tileBC.handleRemoval(true);
        }
		return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
	}

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof TileBC_Neptune tileBC) {
                // A direct replacement has no vanilla loot phase. Player/explosion paths arrive here too, but their
                // earlier handleRemoval call makes this idempotent.
                tileBC.handleRemoval(true);
            }
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    @Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer,
			ItemStack stack) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onPlacedBy(placer, stack);
            tileBC.onNeighbourBlockChanged(Blocks.AIR.defaultBlockState(), pos);
            tileBC.neighbourBlockChanged(Blocks.AIR.defaultBlockState(), pos, false);
        }
		super.setPlacedBy(world, pos, state, placer, stack);
	}
    
	@Override
	public List<ItemStack> getDrops(BlockState state, Builder builder) {
		BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		NonNullList<ItemStack> drops = NonNullList.create();
		if(blockEntity instanceof TileBC_Neptune tile) {
            boolean claimContents = tile.isRemovalHandled()
                || builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null
                || builder.getOptionalParameter(LootContextParams.THIS_ENTITY) != null;
            tile.collectRemovalDrops(drops, UPDATE_ALL, claimContents);
		}
		drops.add(state.getCloneItemStack(null, null, null, null));//TODOs
		return drops;
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            return tileBC.onActivated(player, hand, hit);
        }
		return InteractionResult.PASS;
	}


	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			BlockPos fromPos, boolean harvest) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.neighbourBlockChanged(state, fromPos, harvest);
        }
		super.neighborChanged(state, level, pos, neighbor, fromPos, harvest);

	}
	
	//Only Update for tileEntity changed
	@Override
	public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onNeighbourBlockChanged(state, neighbor);
        }
		super.onNeighborChange(state, level, pos, neighbor);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> bet) {
		return (a,b,c,blockEntity) -> {
			if(blockEntity instanceof TileBC_Neptune tile) {
				tile.update();
			}
		};
	}
	
	
    
    
}
