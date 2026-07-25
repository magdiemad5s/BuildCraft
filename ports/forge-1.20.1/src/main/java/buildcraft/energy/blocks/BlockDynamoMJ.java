/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.blocks;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.lib.block.BlockBCTile_Neptune;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Legacy MJ Dynamo shell. Its output direction remains tile data so old-world NBT keeps the
 * authoritative {@code currentDirection} key.
 */
public class BlockDynamoMJ extends BlockBCTile_Neptune implements ICustomRotationHandler {
    private static final VoxelShape BASE_UP = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape TRUNK_UP = Block.box(4, 4, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_UP = Shapes.or(BASE_UP, TRUNK_UP);

    private static final VoxelShape BASE_DOWN = Block.box(0, 12, 0, 16, 16, 16);
    private static final VoxelShape TRUNK_DOWN = Block.box(4, 0, 4, 12, 12, 12);
    private static final VoxelShape SHAPE_DOWN = Shapes.or(BASE_DOWN, TRUNK_DOWN);

    private static final VoxelShape BASE_EAST = Block.box(0, 0, 0, 4, 16, 16);
    private static final VoxelShape TRUNK_EAST = Block.box(4, 4, 4, 16, 12, 12);
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE_EAST, TRUNK_EAST);

    private static final VoxelShape BASE_WEST = Block.box(12, 0, 0, 16, 16, 16);
    private static final VoxelShape TRUNK_WEST = Block.box(0, 4, 4, 12, 12, 12);
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE_WEST, TRUNK_WEST);

    private static final VoxelShape BASE_SOUTH = Block.box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape TRUNK_SOUTH = Block.box(4, 4, 4, 12, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE_SOUTH, TRUNK_SOUTH);

    private static final VoxelShape BASE_NORTH = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape TRUNK_NORTH = Block.box(4, 4, 0, 12, 12, 12);
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE_NORTH, TRUNK_NORTH);

    public BlockDynamoMJ(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDynamoMJ(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getStaticShape(level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getStaticShape(level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private static VoxelShape getStaticShape(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Direction direction = blockEntity instanceof TileDynamoMJ dynamo
            ? dynamo.getCurrentDirection()
            : Direction.UP;
        return switch (direction) {
            case DOWN -> SHAPE_DOWN;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
            case NORTH -> SHAPE_NORTH;
            case UP -> SHAPE_UP;
        };
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighbor,
        BlockPos fromPos,
        boolean moving
    ) {
        super.neighborChanged(state, level, pos, neighbor, fromPos, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileDynamoMJ dynamo) {
            dynamo.rotateIfInvalid();
        }
    }

    @Override
    public InteractionResult attemptRotation(Level level, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (level.getBlockEntity(pos) instanceof TileDynamoMJ dynamo) {
            return dynamo.attemptRotation();
        }
        return InteractionResult.FAIL;
    }
}
