package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Star Fissure.
 * The gateway out of an Age back to the overworld.
 */
public class StarFissureBlockEntity extends MystcraftBlockEntity {

  private static final String TAG_STATE = "FissureState";
  private static final String TAG_PROGRESS = "FormProgress";
  private FissureState state = FissureState.OPEN;
  private float formProgress = 1.0f; // 0 = closed, 1 = open
  private float animationTick = 0f;
  public StarFissureBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.STAR_FISSURE.get(), pos, blockState);
  }

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    tag.putInt(TAG_STATE, state.ordinal());
    tag.putFloat(TAG_PROGRESS, formProgress);
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    int stateOrd = tag.getInt(TAG_STATE);
    if (stateOrd >= 0 && stateOrd < FissureState.values().length) {
      state = FissureState.values()[stateOrd];
    }
    formProgress = tag.getFloat(TAG_PROGRESS);
  }

  /**
   * Gets the current fissure state.
   */
  public FissureState getFissureState() {
    return state;
  }

  /**
   * Sets the fissure state.
   */
  public void setFissureState(FissureState newState) {
    if (this.state != newState) {
      this.state = newState;
      markForUpdate();
    }
  }

  /**
   * Gets the formation progress (0-1).
   */
  public float getFormProgress() {
    return formProgress;
  }

  /**
   * Gets the animation tick for rendering.
   */
  public float getAnimationTick() {
    return animationTick;
  }

  /**
   * Checks if the fissure can be used for travel.
   */
  public boolean isUsable() {
    return state == FissureState.OPEN;
  }

  /**
   * Opens the fissure.
   */
  public void open() {
    if (state == FissureState.CLOSED || state == FissureState.CLOSING) {
      setFissureState(FissureState.FORMING);
    }
  }

  /**
   * Closes the fissure.
   */
  public void close() {
    if (state == FissureState.OPEN || state == FissureState.FORMING) {
      setFissureState(FissureState.CLOSING);
    }
  }

  /**
   * Updates the fissure animation state.
   * Should be called every tick on the client.
   */
  public void tickAnimation(float partialTick) {
    animationTick += partialTick;

    // Update formation progress based on state
    float progressDelta = 0.02f; // 50 ticks to fully open/close
    switch (state) {
      case FORMING:
        formProgress = Math.min(1.0f, formProgress + progressDelta);
        if (formProgress >= 1.0f) {
          state = FissureState.OPEN;
          markForUpdate();
        }
        break;
      case CLOSING:
        formProgress = Math.max(0.0f, formProgress - progressDelta);
        if (formProgress <= 0.0f) {
          state = FissureState.CLOSED;
          markForUpdate();
        }
        break;
      default:
        break;
    }
  }

  /**
   * Gets the scale for rendering based on progress.
   */
  public float getRenderScale() {
    return 0.5f + (formProgress * 0.5f);
  }

  /**
   * Gets the alpha for rendering based on progress.
   */
  public float getRenderAlpha() {
    return formProgress;
  }

  /**
   * The current state of the fissure.
   */
  public enum FissureState {
    CLOSED,    // Not active, not visible
    FORMING,   // Animating to open state
    OPEN,      // Fully open, can be used
    CLOSING    // Animating to closed state
  }
}
