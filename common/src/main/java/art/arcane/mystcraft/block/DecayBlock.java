package art.arcane.mystcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

/**
 * The Decay block. Spreads through unstable Ages, destroying blocks in its
 * path. Different decay types have different behaviors and colors.
 */
public class DecayBlock extends Block {

  public static final EnumProperty<DecayType> DECAY_TYPE = EnumProperty.create("decay", DecayType.class);

  public DecayBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(DECAY_TYPE, DecayType.BLACK));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(DECAY_TYPE);
  }

  @Override
  public boolean isRandomlyTicking(BlockState state) {
    return true;
  }

  @Override
  public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    DecayType type = state.getValue(DECAY_TYPE);

    if (type == DecayType.BLACK) {
      tickBlackDecay(state, level, pos, random);
    } else if (type.canSpread()) {
      trySpread(state, level, pos, random);
    }
  }

  private void tickBlackDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

    if (random.nextInt(4) == 0) {
      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      if (canDecay(belowState) && !belowState.isAir()) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        FallingBlockEntity.fall(level, below, state);
      }
    }

    if (random.nextInt(3) == 0) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (canDecay(neighborState)) {
          level.setBlock(neighborPos, state, 3);
        }
      }
    } else {

      trySpread(state, level, pos, random);
    }

    for (Direction direction : Direction.values()) {
      BlockPos neighborPos = pos.relative(direction);
      BlockState neighborState = level.getBlockState(neighborPos);
      if (!neighborState.getFluidState().isEmpty() && !(neighborState.getBlock() instanceof DecayBlock)) {
        level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), 3);
      }
    }
  }

  private void trySpread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

    Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
    BlockPos targetPos = pos.relative(direction);
    BlockState targetState = level.getBlockState(targetPos);

    if (canDecay(targetState)) {
      level.setBlock(targetPos, state, 3);
    }
  }

  private boolean canDecay(BlockState state) {

    if (state.isAir()) {
      return false;
    }

    if (state.getDestroySpeed(null, BlockPos.ZERO) < 0) {
      return false;
    }

    if (state.getBlock() instanceof DecayBlock) {
      return false;
    }

    return !state.is(Blocks.END_PORTAL) && !state.is(Blocks.END_PORTAL_FRAME) &&
        !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_GATEWAY);
  }

  @Override
  public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
    if (entity instanceof LivingEntity living) {
      DecayType type = state.getValue(DECAY_TYPE);

      if (type.damageOnContact()) {
        living.hurt(level.damageSources().magic(), type.getDamage());
      }
    }
    super.stepOn(level, pos, state, entity);
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (entity instanceof LivingEntity living) {
      DecayType type = state.getValue(DECAY_TYPE);
      if (type.damageOnContact()) {
        living.hurt(level.damageSources().magic(), type.getDamage() * 0.5f);
      }
    }
  }

  /**
   * Decay types with different behaviors.
   */
  public enum DecayType implements StringRepresentable {
    BLACK("black", true, true, 2.0f),
    WHITE("white", true, false, 0.0f),
    RED("red", true, true, 4.0f),
    BLUE("blue", false, false, 0.0f),
    PURPLE("purple", true, true, 3.0f);

    private final String name;
    private final boolean spreads;
    private final boolean damages;
    private final float damage;

    DecayType(String name, boolean spreads, boolean damages, float damage) {
      this.name = name;
      this.spreads = spreads;
      this.damages = damages;
      this.damage = damage;
    }

    public boolean canSpread() {
      return spreads;
    }

    public boolean damageOnContact() {
      return damages;
    }

    public float getDamage() {
      return damage;
    }

    @Override
    @NotNull
    public String getSerializedName() {
      return name;
    }
  }
}
