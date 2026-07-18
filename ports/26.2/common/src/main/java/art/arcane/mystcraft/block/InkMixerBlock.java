package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Ink Mixer block. Used to mix inks and dyes to create link panels with
 * properties.
 */
public class InkMixerBlock extends BaseEntityBlock {

  public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final MapCodec<InkMixerBlock> CODEC = simpleCodec(InkMixerBlock::new);

  public InkMixerBlock(Properties properties) {
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
    return new InkMixerBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return createTickerHelper(type, ModBlockEntities.INK_MIXER.get(),
        (lvl, pos, blockState, blockEntity) -> blockEntity.tick());
  }

  @Override
  @NotNull
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity instanceof InkMixerBlockEntity mixer) {
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        Services.PLATFORM.openMenu(serverPlayer, mixer, buf -> buf.writeBlockPos(pos));
      }
    }

    return InteractionResult.CONSUME;
  }

  @Override
  protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                             BlockPos pos, boolean movedByPiston) {
    level.updateNeighbourForOutputSignal(pos, this);
    super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Override
  protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
                                      Direction direction) {
    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity instanceof InkMixerBlockEntity mixer) {
      return mixer.hasInk() ? 15 : 0;
    }
    return 0;
  }
}
