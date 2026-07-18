package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.OpenLecternBookPacket;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Book Receptacle block — a thin platform that mounts on any face of a
 * Crystal block and accepts a Linkbook or Agebook to fire the portal frame.
 * <p>
 * The {@link #FACING} property records which face of the supporting Crystal the
 * receptacle is attached to (i.e. the direction <em>away</em> from that
 * Crystal). Five orientations are valid:
 * <ul>
 *   <li>{@code FACING=UP} — sits on top of a floor-mounted Crystal.</li>
 *   <li>{@code FACING=NORTH/SOUTH/EAST/WEST} — clings to a wall-mounted Crystal.</li>
 * </ul>
 * Ceiling mounting ({@code FACING=DOWN}) is intentionally disallowed: the
 * placed book would render upside down with no clean visual treatment.
 * <p>
 * Right-click behaviour:
 * <ul>
 *   <li>Empty hand + book inside → opens the book screen for reading/editing the link.</li>
 *   <li>Sneak + empty hand + book inside → retrieves the book (and tears down the portal).</li>
 *   <li>Holding a Linkbook/Agebook on an empty receptacle → places it (firing the portal).</li>
 * </ul>
 */
public class BookReceptacleBlock extends BaseEntityBlock {

  public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
  public static final MapCodec<BookReceptacleBlock> CODEC = simpleCodec(BookReceptacleBlock::new);
  private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 6, 16);
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
      case NORTH -> SHAPE_NORTH;
      case SOUTH -> SHAPE_SOUTH;
      case WEST -> SHAPE_WEST;
      case EAST -> SHAPE_EAST;
      default -> SHAPE_UP;
    };
  }

  /**
   * Validate placement up front: the clicked block must be a Crystal and the
   * resulting orientation cannot be ceiling-mount. Returning {@code null}
   * cancels placement entirely so we never temporarily place a doomed block.
   */
  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction facing = context.getClickedFace();
    if (facing == Direction.DOWN) {
      return null;
    }
    BlockPos placedAt = context.getClickedPos();
    BlockPos supportPos = placedAt.relative(facing.getOpposite());
    if (!context.getLevel().getBlockState(supportPos).is(ModBlocks.CRYSTAL.get())) {
      return null;
    }
    return defaultBlockState().setValue(FACING, facing);
  }

  @Override
  public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    Direction facing = state.getValue(FACING);
    BlockPos crystalPos = pos.relative(facing.getOpposite());
    return level.getBlockState(crystalPos).is(ModBlocks.CRYSTAL.get());
  }

  /**
   * Pop off if the supporting Crystal is removed.
   */
  @Override
  protected BlockState updateShape(BlockState state, LevelReader level,
                                   ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                   Direction direction, BlockPos neighborPos,
                                   BlockState neighborState, RandomSource random) {
    Direction facing = state.getValue(FACING);
    if (direction == facing.getOpposite() && !neighborState.is(ModBlocks.CRYSTAL.get())) {
      return Blocks.AIR.defaultBlockState();
    }
    return super.updateShape(state, level, scheduledTickAccess, pos, direction,
        neighborPos, neighborState, random);
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

  @Override
  @NotNull
  protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                        BlockPos pos, Player player, InteractionHand hand,
                                        BlockHitResult hit) {
    return interact(state, level, pos, player, held);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
    return interact(state, level, pos, player, ItemStack.EMPTY);
  }

  private InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                     Player player, ItemStack held) {
    BlockEntity be = level.getBlockEntity(pos);
    if (!(be instanceof BookReceptacleBlockEntity receptacle)) {
      return InteractionResult.PASS;
    }

    if (!level.isClientSide()) {
      art.arcane.mystcraft.Mystcraft.LOGGER.info(
          "[Receptacle.use] pos={} sneak={} hasBook={} held={} validActivator={}",
          pos, player.isShiftKeyDown(), receptacle.hasBook(),
          held.isEmpty() ? "<empty>" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()),
          BookReceptacleBlockEntity.isValidPortalActivator(held));
    }

    if (receptacle.hasBook()) {

      if (player.isShiftKeyDown() && held.isEmpty()) {
        if (!level.isClientSide()) {
          ItemStack book = receptacle.takeBook();
          if (!player.getInventory().add(book)) {
            player.drop(book, false);
          }
        }
        return InteractionResult.SUCCESS;
      }

      if (held.isEmpty()) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
          MystcraftNetwork.sendToPlayer(new OpenLecternBookPacket(pos, receptacle.getBook()), serverPlayer);
        }
        return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
    }

    if (BookReceptacleBlockEntity.isValidPortalActivator(held)) {
      if (!level.isClientSide()) {
        ItemStack inserted = held.copy();
        inserted.setCount(1);
        receptacle.setBook(inserted);
        if (!player.getAbilities().instabuild) {
          held.shrink(1);
        }
      }
      return InteractionResult.SUCCESS;
    }

    if (held.getItem() instanceof AgebookItem) {
      if (!level.isClientSide()) {
        art.arcane.mystcraft.util.PlayerMessages.send(player,
            Component.translatable("mystcraft.portal.agebook_no_link_panel"), true);
      }
      return InteractionResult.SUCCESS;
    }

    return InteractionResult.PASS;
  }

  @Override
  protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                             BlockPos pos, boolean movedByPiston) {
    level.updateNeighbourForOutputSignal(pos, this);
    // Always tear down. A portal lit by this receptacle has no other owner;
    // lingering portal blocks confuse pathfinding and stale crystals stay
    // glowing. Safe to call when no portal is currently lit.
    PortalUtils.shutdownPortal(level, pos);
    super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Override
  protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
                                      Direction direction) {
    if (level.getBlockEntity(pos) instanceof BookReceptacleBlockEntity receptacle) {
      return receptacle.getAnalogOutputSignal();
    }
    return 0;
  }
}
