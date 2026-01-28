package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Writing Desk block.
 * Used for writing symbols onto pages using ink.
 * This is a multi-block structure (2 blocks wide, 2 blocks tall).
 *
 * Block positions:
 * - Main block (has tile entity): !IS_TOP && !IS_FOOT
 * - Foot block (secondary horizontal): !IS_TOP && IS_FOOT
 * - Top block above main: IS_TOP && !IS_FOOT
 * - Top block above foot: IS_TOP && IS_FOOT
 */
public class WritingDeskBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty IS_TOP = BooleanProperty.create("is_top");
    public static final BooleanProperty IS_FOOT = BooleanProperty.create("is_foot");

    /** Offset mapping for each horizontal direction index - foot extends in the facing direction */
    private static final int[][] HEAD_FOOT_MAP = {
            {0, 1},   // SOUTH (index 0): foot to Z+1 (south)
            {-1, 0},  // WEST (index 1): foot to X-1 (west)
            {0, -1},  // NORTH (index 2): foot to Z-1 (north)
            {1, 0}    // EAST (index 3): foot to X+1 (east)
    };

    private static final VoxelShape SHAPE_FULL = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_TOP_SOUTH = Block.box(8, 0, 0, 16, 12, 16);
    private static final VoxelShape SHAPE_TOP_WEST = Block.box(0, 0, 8, 16, 12, 16);
    private static final VoxelShape SHAPE_TOP_NORTH = Block.box(0, 0, 0, 8, 12, 16);
    private static final VoxelShape SHAPE_TOP_EAST = Block.box(0, 0, 0, 16, 12, 8);

    public WritingDeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(IS_TOP, false)
                .setValue(IS_FOOT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_TOP, IS_FOOT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Desk FACING is set to player's horizontal facing direction
        // The foot extends in the facing direction (away from where player is standing)
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(IS_TOP)) {
            Direction facing = state.getValue(FACING);
            return switch (facing) {
                case SOUTH -> SHAPE_TOP_SOUTH;
                case WEST -> SHAPE_TOP_WEST;
                case NORTH -> SHAPE_TOP_NORTH;
                case EAST -> SHAPE_TOP_EAST;
                default -> SHAPE_FULL;
            };
        }
        return SHAPE_FULL;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use ENTITYBLOCK_ANIMATED for BER rendering
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(IS_TOP);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only the main block (not top, not foot) has the block entity
        if (!state.getValue(IS_TOP) && !state.getValue(IS_FOOT)) {
            return new WritingDeskBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(IS_TOP) || state.getValue(IS_FOOT)) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WRITING_DESK.get(),
                (lvl, pos, st, be) -> be.tick());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        int dirIndex = facing.get2DDataValue();

        // Calculate foot position offset
        int xOffset = HEAD_FOOT_MAP[dirIndex][0];
        int zOffset = HEAD_FOOT_MAP[dirIndex][1];

        BlockPos footPos = pos.offset(xOffset, 0, zOffset);
        BlockPos topMainPos = pos.above();
        BlockPos topFootPos = footPos.above();

        // Check if we have room for the full structure
        if (canPlaceAt(level, footPos) && canPlaceAt(level, topMainPos) && canPlaceAt(level, topFootPos)) {
            // Place foot block
            level.setBlock(footPos, state.setValue(IS_FOOT, true), 3);

            // Place top blocks
            level.setBlock(topMainPos, state.setValue(IS_TOP, true).setValue(IS_FOOT, false), 3);
            level.setBlock(topFootPos, state.setValue(IS_TOP, true).setValue(IS_FOOT, true), 3);
        } else {
            // Not enough room - remove the placed block
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            // Drop the item back
            if (!level.isClientSide) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(this));
            }
        }
    }

    private boolean canPlaceAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        int dirIndex = facing.get2DDataValue();
        boolean isTop = state.getValue(IS_TOP);
        boolean isFoot = state.getValue(IS_FOOT);

        // Check structural integrity
        if (isTop && !isFoot) {
            // Top main block - check for main block below
            if (!isWritingDesk(level.getBlockState(pos.below()))) {
                destroyStructure(level, pos, state);
            }
        } else if (isTop && isFoot) {
            // Top foot block - check for foot block below
            if (!isWritingDesk(level.getBlockState(pos.below()))) {
                destroyStructure(level, pos, state);
            }
        } else if (isFoot) {
            // Foot block - check for main block
            BlockPos mainPos = pos.offset(-HEAD_FOOT_MAP[dirIndex][0], 0, -HEAD_FOOT_MAP[dirIndex][1]);
            if (!isWritingDesk(level.getBlockState(mainPos))) {
                destroyStructure(level, pos, state);
            }
        } else {
            // Main block - check for foot block
            BlockPos footPos = pos.offset(HEAD_FOOT_MAP[dirIndex][0], 0, HEAD_FOOT_MAP[dirIndex][1]);
            if (!isWritingDesk(level.getBlockState(footPos))) {
                destroyStructure(level, pos, state);
            }
        }
    }

    private boolean isWritingDesk(BlockState state) {
        return state.getBlock() instanceof WritingDeskBlock;
    }

    private void destroyStructure(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            int dirIndex = facing.get2DDataValue();
            boolean isTop = state.getValue(IS_TOP);
            boolean isFoot = state.getValue(IS_FOOT);

            // Find and clear all parts of the structure
            BlockPos mainPos = getMainBlockPos(pos, state);
            BlockPos footPos = mainPos.offset(HEAD_FOOT_MAP[dirIndex][0], 0, HEAD_FOOT_MAP[dirIndex][1]);
            BlockPos topMainPos = mainPos.above();
            BlockPos topFootPos = footPos.above();

            // Only drop items from main block
            if (!isTop && !isFoot) {
                // This is the main block - items will be dropped in onRemove
            }

            // Clear all blocks (without dropping items from non-main blocks)
            if (!pos.equals(mainPos) && isWritingDesk(level.getBlockState(mainPos))) {
                level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), 3);
            }
            if (!pos.equals(footPos) && isWritingDesk(level.getBlockState(footPos))) {
                level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 35); // 35 = no drops
            }
            if (!pos.equals(topMainPos) && isWritingDesk(level.getBlockState(topMainPos))) {
                level.setBlock(topMainPos, Blocks.AIR.defaultBlockState(), 35);
            }
            if (!pos.equals(topFootPos) && isWritingDesk(level.getBlockState(topFootPos))) {
                level.setBlock(topFootPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Gets the position of the main block (with block entity) from any part of the structure.
     */
    public static BlockPos getMainBlockPos(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof WritingDeskBlock)) {
            return pos;
        }

        Direction facing = state.getValue(FACING);
        int dirIndex = facing.get2DDataValue();
        boolean isTop = state.getValue(IS_TOP);
        boolean isFoot = state.getValue(IS_FOOT);

        BlockPos result = pos;
        if (isTop) {
            result = result.below();
        }
        if (isFoot) {
            result = result.offset(-HEAD_FOOT_MAP[dirIndex][0], 0, -HEAD_FOOT_MAP[dirIndex][1]);
        }
        return result;
    }

    /**
     * Gets the block entity for this writing desk from any part of the structure.
     */
    @Nullable
    public static WritingDeskBlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WritingDeskBlock)) {
            return null;
        }
        BlockPos mainPos = getMainBlockPos(pos, state);
        BlockEntity be = level.getBlockEntity(mainPos);
        return be instanceof WritingDeskBlockEntity desk ? desk : null;
    }

    @Override
    @NotNull
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        WritingDeskBlockEntity desk = getBlockEntity(level, pos);
        if (desk != null) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                BlockPos mainPos = getMainBlockPos(pos, state);
                serverPlayer.openMenu(desk);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Only drop items from the main block
            if (!state.getValue(IS_TOP) && !state.getValue(IS_FOOT)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof WritingDeskBlockEntity desk) {
                    List<ItemStack> drops = desk.getDrops();
                    for (ItemStack drop : drops) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                    }
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return !state.getValue(IS_TOP) && !state.getValue(IS_FOOT);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(IS_TOP) || state.getValue(IS_FOOT)) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WritingDeskBlockEntity desk) {
            return desk.getInkAmount() > 0 ? 15 : 0;
        }
        return 0;
    }
}
