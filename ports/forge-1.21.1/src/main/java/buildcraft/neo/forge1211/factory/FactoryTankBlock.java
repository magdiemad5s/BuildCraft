package buildcraft.neo.forge1211.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidUtil;

/** A vertically connecting 16-bucket BuildCraft Factory Tank. */
public final class FactoryTankBlock extends BaseEntityBlock {
    public static final MapCodec<FactoryTankBlock> CODEC = simpleCodec(FactoryTankBlock::new);
    public static final BooleanProperty JOINED_BELOW = BooleanProperty.create("joined_below");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public FactoryTankBlock() {
        this(defaultProperties());
    }

    private FactoryTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(JOINED_BELOW, false));
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .sound(SoundType.GLASS)
            .strength(6.0F, 10.0F)
            .noOcclusion()
            .requiresCorrectToolForDrops();
    }

    @Override
    protected MapCodec<? extends FactoryTankBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINED_BELOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(JOINED_BELOW, isTank(context.getLevel(), context.getClickedPos().below()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryTankBlockEntity(pos, state);
    }

    /**
     * 1.21 separates item and empty-hand use. A fluid container is handled
     * first; returning PASS here deliberately routes every non-container item
     * to {@link #useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)}.
     */
    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof FactoryTankBlockEntity tank) {
            // Forge 1.21's ServerPlayer extension encodes the position for the
            // client menu factory; no custom C2S packet is introduced.
            serverPlayer.openMenu(tank, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FactoryTankBlockEntity tank) {
            return tank.getComparatorLevel();
        }
        return 0;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previousState, boolean movedByPiston) {
        if (!previousState.is(state.getBlock())) {
            updateJoinedBelow(level, pos);
            updateJoinedBelow(level, pos.above());
        }
        super.onPlace(state, level, pos, previousState, movedByPiston);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            updateJoinedBelow(level, pos.above());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static boolean isTank(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FactoryTankBlock;
    }

    private static void updateJoinedBelow(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FactoryTankBlock)) {
            return;
        }
        boolean joinedBelow = isTank(level, pos.below());
        if (state.getValue(JOINED_BELOW) != joinedBelow) {
            level.setBlock(pos, state.setValue(JOINED_BELOW, joinedBelow), Block.UPDATE_CLIENTS);
        }
    }
}
