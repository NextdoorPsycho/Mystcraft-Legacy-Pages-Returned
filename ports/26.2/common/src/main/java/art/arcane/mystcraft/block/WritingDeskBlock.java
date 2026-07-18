package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Writing Desk block. Used for writing symbols onto pages using ink. This
 * is a multi-block structure (2 blocks wide, 2 blocks tall).
 * <p>
 * Block positions: - Main block (has tile entity): !IS_TOP && !IS_FOOT - Foot
 * block (secondary horizontal): !IS_TOP && IS_FOOT - Top block above main:
 * IS_TOP && !IS_FOOT - Top block above foot: IS_TOP && IS_FOOT
 */
public class WritingDeskBlock extends BaseEntityBlock {

  public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty IS_TOP = BooleanProperty.create("is_top");
  public static final BooleanProperty IS_FOOT = BooleanProperty.create("is_foot");
  public static final MapCodec<WritingDeskBlock> CODEC = simpleCodec(WritingDeskBlock::new);
  private static final int[][] HEAD_FOOT_MAP = {
      {0, 1},
      {-1, 0},
      {0, -1},
      {1, 0}
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

  /**
   * Gets the position of the main block (with block entity) from any part of
   * the structure.
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
   * Gets the block entity for this writing desk from any part of the
   * structure.
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
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, IS_TOP, IS_FOOT);
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {

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

    return RenderShape.INVISIBLE;
  }

  @Override
  public boolean useShapeForLightOcclusion(BlockState state) {
    return state.getValue(IS_TOP);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

    if (!state.getValue(IS_TOP) && !state.getValue(IS_FOOT)) {
      return new WritingDeskBlockEntity(pos, state);
    }
    return null;
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide() || state.getValue(IS_TOP) || state.getValue(IS_FOOT)) {
      return null;
    }
    return createTickerHelper(type, ModBlockEntities.WRITING_DESK.get(),
        (lvl, pos, st, be) -> be.tick());
  }

  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    if (level.isClientSide()) return;

    Direction facing = state.getValue(FACING);
    int dirIndex = facing.get2DDataValue();

    int xOffset = HEAD_FOOT_MAP[dirIndex][0];
    int zOffset = HEAD_FOOT_MAP[dirIndex][1];

    BlockPos footPos = pos.offset(xOffset, 0, zOffset);
    BlockPos topMainPos = pos.above();
    BlockPos topFootPos = footPos.above();

    if (canPlaceAt(level, footPos) && canPlaceAt(level, topMainPos) && canPlaceAt(level, topFootPos)) {

      level.setBlock(footPos, state.setValue(IS_FOOT, true), 3);

      level.setBlock(topMainPos, state.setValue(IS_TOP, true).setValue(IS_FOOT, false), 3);
      level.setBlock(topFootPos, state.setValue(IS_TOP, true).setValue(IS_FOOT, true), 3);
    } else {

      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

      if (!level.isClientSide()) {
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
  protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                 Orientation orientation, boolean isMoving) {
    if (level.isClientSide()) return;

    Direction facing = state.getValue(FACING);
    int dirIndex = facing.get2DDataValue();
    boolean isTop = state.getValue(IS_TOP);
    boolean isFoot = state.getValue(IS_FOOT);

    if (isTop && !isFoot) {

      if (!isWritingDesk(level.getBlockState(pos.below()))) {
        destroyStructure(level, pos, state);
      }
    } else if (isTop && isFoot) {

      if (!isWritingDesk(level.getBlockState(pos.below()))) {
        destroyStructure(level, pos, state);
      }
    } else if (isFoot) {

      BlockPos mainPos = pos.offset(-HEAD_FOOT_MAP[dirIndex][0], 0, -HEAD_FOOT_MAP[dirIndex][1]);
      if (!isWritingDesk(level.getBlockState(mainPos))) {
        destroyStructure(level, pos, state);
      }
    } else {

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

  private void clearStructureOnRemove(Level level, BlockPos pos, BlockState state) {
    Direction facing = state.getValue(FACING);
    int dirIndex = facing.get2DDataValue();

    BlockPos mainPos = getMainBlockPos(pos, state);
    BlockPos footPos = mainPos.offset(HEAD_FOOT_MAP[dirIndex][0], 0, HEAD_FOOT_MAP[dirIndex][1]);
    BlockPos topMainPos = mainPos.above();
    BlockPos topFootPos = footPos.above();

    if (!pos.equals(mainPos) && isWritingDesk(level.getBlockState(mainPos))) {
      level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), 3);
    }
    if (!pos.equals(footPos) && isWritingDesk(level.getBlockState(footPos))) {
      level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 35);
    }
    if (!pos.equals(topMainPos) && isWritingDesk(level.getBlockState(topMainPos))) {
      level.setBlock(topMainPos, Blocks.AIR.defaultBlockState(), 35);
    }
    if (!pos.equals(topFootPos) && isWritingDesk(level.getBlockState(topFootPos))) {
      level.setBlock(topFootPos, Blocks.AIR.defaultBlockState(), 35);
    }
  }

  @Override
  @NotNull
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    WritingDeskBlockEntity desk = getBlockEntity(level, pos);
    if (desk != null) {
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        BlockPos mainPos = getMainBlockPos(pos, state);
        Services.PLATFORM.openMenu(serverPlayer, desk, buf -> buf.writeBlockPos(mainPos));
      }
    }

    return InteractionResult.CONSUME;
  }

  @Override
  protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                             BlockPos pos, boolean movedByPiston) {
    clearStructureOnRemove(level, pos, state);
    if (!state.getValue(IS_TOP) && !state.getValue(IS_FOOT)) {
      level.updateNeighbourForOutputSignal(pos, this);
    }
    super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return !state.getValue(IS_TOP) && !state.getValue(IS_FOOT);
  }

  @Override
  protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
                                      Direction direction) {
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
