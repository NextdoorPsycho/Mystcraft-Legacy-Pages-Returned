package art.arcane.mystcraft.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public abstract class FabricBlackInkFluid extends FlowingFluid {

  @Override
  public Fluid getFlowing() {
    return FabricRegistries.BLACK_INK_FLOWING.get();
  }

  @Override
  public Fluid getSource() {
    return FabricRegistries.BLACK_INK_SOURCE.get();
  }

  @Override
  public Item getBucket() {
    return FabricRegistries.INK_BUCKET.get();
  }

  @Override
  protected boolean canConvertToSource(Level level) {
    return false;
  }

  @Override
  protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
    Block.dropResources(state, level, pos, blockEntity);
  }

  @Override
  protected int getSlopeFindDistance(LevelReader level) {
    return 2;
  }

  @Override
  protected int getDropOff(LevelReader level) {
    return 2;
  }

  @Override
  protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
    return direction == Direction.DOWN && !isSame(fluid);
  }

  @Override
  public int getTickDelay(LevelReader level) {
    return 10;
  }

  @Override
  protected float getExplosionResistance() {
    return 100.0F;
  }

  @Override
  protected BlockState createLegacyBlock(FluidState state) {
    return FabricRegistries.FLUID_INK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
  }

  @Override
  public boolean isSame(Fluid fluid) {
    return fluid == FabricRegistries.BLACK_INK_SOURCE.get() || fluid == FabricRegistries.BLACK_INK_FLOWING.get();
  }

  public static class Source extends FabricBlackInkFluid {
    @Override
    public int getAmount(FluidState state) {
      return 8;
    }

    @Override
    public boolean isSource(FluidState state) {
      return true;
    }
  }

  public static class Flowing extends FabricBlackInkFluid {
    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
      super.createFluidStateDefinition(builder);
      builder.add(LEVEL);
    }

    @Override
    public int getAmount(FluidState state) {
      return state.getValue(LEVEL);
    }

    @Override
    public boolean isSource(FluidState state) {
      return false;
    }
  }
}
