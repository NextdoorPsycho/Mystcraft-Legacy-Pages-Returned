package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A lightning bolt with customizable color. Used for instability effects and
 * special age features.
 */
public class ColoredLightningEntity extends LightningBolt {

  private static final EntityDataAccessor<Integer> DATA_COLOR =
      SynchedEntityData.defineId(ColoredLightningEntity.class, EntityDataSerializers.INT);

  private static final int DEFAULT_COLOR = packColor(0.45f, 0.45f, 0.5f);

  public ColoredLightningEntity(EntityType<? extends LightningBolt> entityType, Level level) {
    super(entityType, level);
  }

  public ColoredLightningEntity(Level level, double x, double y, double z, boolean visualOnly) {
    this(ModEntities.COLORED_LIGHTNING.get(), level);
    this.setPos(x, y, z);
    this.setVisualOnly(visualOnly);
  }

  private static int packColor(float red, float green, float blue) {
    int r = (int) (red * 255) & 0xFF;
    int g = (int) (green * 255) & 0xFF;
    int b = (int) (blue * 255) & 0xFF;
    return (r << 16) | (g << 8) | b;
  }

  /**
   * Spawns a colored lightning bolt at the given position.
   */
  public static ColoredLightningEntity spawn(ServerLevel level, BlockPos pos, boolean visualOnly, int color) {
    ColoredLightningEntity lightning = new ColoredLightningEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, visualOnly);
    lightning.setColor(color);
    level.addFreshEntity(lightning);
    return lightning;
  }

  /**
   * Spawns a colored lightning bolt at the given position with RGB floats.
   */
  public static ColoredLightningEntity spawn(ServerLevel level, BlockPos pos, boolean visualOnly, float red, float green, float blue) {
    return spawn(level, pos, visualOnly, packColor(red, green, blue));
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DATA_COLOR, DEFAULT_COLOR);
  }

  /**
   * Sets the lightning color using RGB floats (0.0-1.0).
   */
  public void setColor(float red, float green, float blue) {
    this.entityData.set(DATA_COLOR, packColor(red, green, blue));
  }

  /**
   * Gets the lightning color as a packed RGB integer.
   */
  public int getColor() {
    return this.entityData.get(DATA_COLOR);
  }

  /**
   * Sets the lightning color using a packed RGB integer.
   */
  public void setColor(int color) {
    this.entityData.set(DATA_COLOR, color);
  }

  /**
   * Gets the red component (0.0-1.0).
   */
  public float getRed() {
    return ((getColor() >> 16) & 0xFF) / 255.0f;
  }

  /**
   * Gets the green component (0.0-1.0).
   */
  public float getGreen() {
    return ((getColor() >> 8) & 0xFF) / 255.0f;
  }

  /**
   * Gets the blue component (0.0-1.0).
   */
  public float getBlue() {
    return (getColor() & 0xFF) / 255.0f;
  }

  @Override
  protected void readAdditionalSaveData(ValueInput input) {
    super.readAdditionalSaveData(input);
    setColor(input.getIntOr("Color", DEFAULT_COLOR));
  }

  @Override
  protected void addAdditionalSaveData(ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putInt("Color", getColor());
  }
}
