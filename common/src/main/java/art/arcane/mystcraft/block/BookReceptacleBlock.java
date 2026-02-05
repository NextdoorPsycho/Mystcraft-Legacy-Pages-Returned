package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.util.BlockInteractionCompat;
import art.arcane.mystcraft.util.CodecCompat;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Book Receptacle block.
 * Holds a descriptive book and creates a portal when attached to a crystal.
 * Can be placed on any face of a crystal block.
 */
public class BookReceptacleBlock extends BaseEntityBlock implements BlockInteractionCompat {

  public static final MapCodec<BookReceptacleBlock> CODEC = CodecCompat.simpleCodec(BookReceptacleBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.FACING;

  // Shapes for each facing direction (thin block against the crystal)
  private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 6, 16);
  private static final VoxelShape SHAPE_DOWN = Block.box(0, 10, 0, 16, 16, 16);
  private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 10, 16, 16, 16);
  private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 6);
  private static final VoxelShape SHAPE_WEST = Block.box(10, 0, 0, 16, 16, 16);
  private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 6, 16, 16);

  public BookReceptacleBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return switch (state.getValue(FACING)) {
      case UP -> SHAPE_UP;
      case DOWN -> SHAPE_DOWN;
      case NORTH -> SHAPE_NORTH;
      case SOUTH -> SHAPE_SOUTH;
      case WEST -> SHAPE_WEST;
      case EAST -> SHAPE_EAST;
    };
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction facing = context.getClickedFace();
    // Cannot place facing down (book would be upside down on top of crystal)
    if (facing == Direction.DOWN) {
      return null;
    }
    return defaultBlockState().setValue(FACING, facing);
  }

  @Override
  public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    Direction facing = state.getValue(FACING);
    BlockPos crystalPos = pos.relative(facing.getOpposite());
    BlockState crystalState = level.getBlockState(crystalPos);
    return crystalState.is(ModBlocks.CRYSTAL.get());
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    Direction facing = state.getValue(FACING);
    if (direction == facing.getOpposite()) {
      // The crystal behind us changed
      if (!neighborState.is(ModBlocks.CRYSTAL.get())) {
        return Blocks.AIR.defaultBlockState();
      }
    }
    return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public boolean useShapeForLightOcclusion(BlockState state) {
    return true;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BookReceptacleBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return null;
  }

  @NotNull
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    }

    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (!(blockEntity instanceof BookReceptacleBlockEntity receptacle)) {
      return InteractionResult.PASS;
    }

    ItemStack held = player.getItemInHand(hand);

    if (receptacle.hasBook()) {
      // Book is in receptacle - retrieve it
      if (held.isEmpty()) {
        player.setItemInHand(hand, receptacle.getBook());
        receptacle.setBook(ItemStack.EMPTY);
        return InteractionResult.CONSUME;
      }
    } else {
      // No book in receptacle - try to place one
      if (!held.isEmpty() && BookReceptacleBlockEntity.isValidPortalActivator(held)) {
        receptacle.setBook(held.copy());
        player.setItemInHand(hand, ItemStack.EMPTY);
        return InteractionResult.CONSUME;
      }
    }

    return InteractionResult.PASS;
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof BookReceptacleBlockEntity receptacle) {
        receptacle.dropContents();
        level.updateNeighbourForOutputSignal(pos, this);
      }
      super.onRemove(state, level, pos, newState, isMoving);
    }
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Override
  public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity instanceof BookReceptacleBlockEntity receptacle) {
      return receptacle.getAnalogOutputSignal();
    }
    return 0;
  }
}
