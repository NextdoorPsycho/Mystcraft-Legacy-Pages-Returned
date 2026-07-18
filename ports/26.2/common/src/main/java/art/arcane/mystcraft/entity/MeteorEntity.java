package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Meteor entity. Falls from the sky in unstable Ages, causing destruction
 * on impact.
 */
public class MeteorEntity extends Entity {

  private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(
      MeteorEntity.class, EntityDataSerializers.INT);

  private boolean explodeOnImpact = true;

  public MeteorEntity(EntityType<?> type, Level level) {
    super(type, level);
    this.noPhysics = false;
  }

  public MeteorEntity(Level level, double x, double y, double z, int size) {
    this(ModEntities.METEOR.get(), level);
    setPos(x, y, z);
    setSize(size);
    setDeltaMovement(0, -0.5, 0);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(DATA_SIZE, 1);
  }

  @Override
  public void tick() {
    super.tick();

    if (!isNoGravity()) {
      setDeltaMovement(getDeltaMovement().add(0, -0.05, 0));
    }

    Vec3 motion = getDeltaMovement();
    setDeltaMovement(motion.x * 0.98, motion.y, motion.z * 0.98);

    move(MoverType.SELF, getDeltaMovement());

    if (onGround() && !level().isClientSide()) {
      impact();
    }

    if (getY() < level().getMinY() - 64) {
      discard();
    }
  }

  private void impact() {
    if (level().isClientSide()) {
      discard();
      return;
    }

    BlockPos impactPos = blockPosition();
    int size = getSize();
    float explosionRadius = 2.0f + (size * 1.5f);

    level().playSound(null, impactPos, ModSounds.METEOR_IMPACT.get(),
        SoundSource.BLOCKS, 2.0f, 0.8f + random.nextFloat() * 0.4f);

    if (explodeOnImpact) {
      level().explode(this, getX(), getY(), getZ(), explosionRadius,
          Level.ExplosionInteraction.TNT);
    }

    if (level() instanceof ServerLevel serverLevel) {
      int craterRadius = size + 1;
      createCrater(serverLevel, impactPos, craterRadius);
      spawnFire(serverLevel, impactPos, craterRadius);
      spawnImpactParticles(serverLevel, impactPos);
    }

    discard();
  }

  private void createCrater(ServerLevel level, BlockPos center, int radius) {
    int radiusSq = radius * radius;
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius / 2; dy++) {
        for (int dz = -radius; dz <= radius; dz++) {
          int distSq = dx * dx + dy * dy * 4 + dz * dz;
          if (distSq <= radiusSq) {
            BlockPos pos = center.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);

            if (state.getDestroySpeed(level, pos) >= 0 && !state.isAir()) {
              level.destroyBlock(pos, false);
            }
          }
        }
      }
    }
  }

  private void spawnFire(ServerLevel level, BlockPos center, int radius) {
    int fireCount = getSize() * 3 + random.nextInt(4);
    for (int i = 0; i < fireCount; i++) {
      int dx = random.nextInt(radius * 2 + 1) - radius;
      int dz = random.nextInt(radius * 2 + 1) - radius;
      BlockPos firePos = center.offset(dx, 0, dz);

      for (int dy = radius; dy >= -radius; dy--) {
        BlockPos checkPos = firePos.above(dy);
        BlockPos belowPos = checkPos.below();
        if (level.getBlockState(checkPos).isAir() &&
            level.getBlockState(belowPos).isSolidRender()) {
          level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
          break;
        }
      }
    }
  }

  private void spawnImpactParticles(ServerLevel level, BlockPos center) {
    int size = getSize();
    int particleCount = 20 + size * 10;
    for (int i = 0; i < particleCount; i++) {
      double ox = (random.nextDouble() - 0.5) * 2.0 * size;
      double oy = random.nextDouble() * size;
      double oz = (random.nextDouble() - 0.5) * 2.0 * size;
      level.sendParticles(ParticleTypes.LARGE_SMOKE,
          center.getX() + ox, center.getY() + oy, center.getZ() + oz,
          1, 0, 0.1, 0, 0.05);
    }
    for (int i = 0; i < particleCount / 2; i++) {
      double ox = (random.nextDouble() - 0.5) * size;
      double oy = random.nextDouble() * size * 0.5;
      double oz = (random.nextDouble() - 0.5) * size;
      level.sendParticles(ParticleTypes.FLAME,
          center.getX() + ox, center.getY() + oy, center.getZ() + oz,
          1, 0, 0.2, 0, 0.1);
    }
  }

  @Override
  protected void readAdditionalSaveData(ValueInput input) {
    setSize(input.getIntOr("Size", 0));
    explodeOnImpact = input.getBooleanOr("ExplodeOnImpact", false);
  }

  @Override
  protected void addAdditionalSaveData(ValueOutput output) {
    output.putInt("Size", getSize());
    output.putBoolean("ExplodeOnImpact", explodeOnImpact);
  }

  @Override
  public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    return false;
  }

  public int getSize() {
    return this.entityData.get(DATA_SIZE);
  }

  public void setSize(int size) {
    this.entityData.set(DATA_SIZE, size);
  }
}
