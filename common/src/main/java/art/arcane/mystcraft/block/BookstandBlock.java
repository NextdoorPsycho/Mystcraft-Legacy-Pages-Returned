package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
 * The Bookstand block.
 * Holds a linkbook or agebook for display and use.
 * Players can place books and use them to link.
 */
public class BookstandBlock extends BaseEntityBlock implements BlockInteractionCompat {

  public static final MapCodec<BookstandBlock> CODEC = CodecCompat.simpleCodec(BookstandBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

  private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

  public BookstandBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    // Use ENTITYBLOCK_ANIMATED - the BlockEntityRenderer handles all rendering
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  public boolean useShapeForLightOcclusion(BlockState state) {
    return true;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BookstandBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return null;
  }

  @NotNull
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (!(blockEntity instanceof BookstandBlockEntity stand)) {
      return InteractionResult.PASS;
    }

    ItemStack held = player.getItemInHand(hand);

    if (stand.hasBook()) {
      // Book is on stand
      if (player.isShiftKeyDown() && held.isEmpty()) {
        // Shift + empty hand = pick up book (server only)
        if (!level.isClientSide) {
          player.setItemInHand(hand, stand.getBook());
          stand.setBook(ItemStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
        // Open book GUI on client side
        if (level.isClientSide) {
          art.arcane.mystcraft.client.screen.BookScreen.openForBlock(stand.getBook(), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
      }
    } else {
      // No book on stand
      if (!held.isEmpty() && BookstandBlockEntity.isValidBook(held)) {
        // Place book on stand (server only)
        if (!level.isClientSide) {
          ItemStack bookCopy = held.copy();
          bookCopy.setCount(1);
          held.shrink(1);
          stand.setBook(bookCopy);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
    }
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof BookstandBlockEntity stand) {
        stand.dropContents();
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
    if (blockEntity instanceof BookstandBlockEntity stand) {
      return stand.getAnalogOutputSignal();
    }
    return 0;
  }
}
