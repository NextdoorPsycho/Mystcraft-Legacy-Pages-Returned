package art.arcane.mystcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Base class for all Mystcraft block entities.
 * Provides common NBT handling and sync utilities.
 */
public abstract class MystcraftBlockEntity extends BlockEntity {

  private static final Method SAVE_ADDITIONAL_PROVIDER = findMethod(BlockEntity.class, "saveAdditional", CompoundTag.class, HolderLookup.Provider.class);
  private static final Method SAVE_ADDITIONAL_OLD = findMethod(BlockEntity.class, "saveAdditional", CompoundTag.class);
  private static final Method LOAD_ADDITIONAL_PROVIDER = findMethod(BlockEntity.class, "loadAdditional", CompoundTag.class, HolderLookup.Provider.class);
  private static final Method LOAD_OLD = findMethod(BlockEntity.class, "load", CompoundTag.class);
  private static final Method GET_UPDATE_TAG_PROVIDER = findMethod(BlockEntity.class, "getUpdateTag", HolderLookup.Provider.class);
  private static final Method GET_UPDATE_TAG_OLD = findMethod(BlockEntity.class, "getUpdateTag");

  public MystcraftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
    super(type, pos, blockState);
  }

  protected void saveAdditional(CompoundTag tag) {
    invokeSuperSaveAdditional(tag, null);
    writeNbt(tag);
  }

  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    invokeSuperSaveAdditional(tag, provider);
    writeNbt(tag);
  }

  public void load(CompoundTag tag) {
    invokeSuperLoad(tag, null);
    readNbt(tag);
  }

  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    invokeSuperLoad(tag, provider);
    readNbt(tag);
  }

  /**
   * Override to save custom data to NBT.
   */
  protected void writeNbt(CompoundTag tag) {
    // Override in subclasses
  }

  /**
   * Override to load custom data from NBT.
   */
  protected void readNbt(CompoundTag tag) {
    // Override in subclasses
  }

  public CompoundTag getUpdateTag() {
    CompoundTag tag = invokeSuperGetUpdateTag(null);
    writeNbt(tag);
    return tag;
  }

  public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
    CompoundTag tag = invokeSuperGetUpdateTag(provider);
    writeNbt(tag);
    return tag;
  }

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  /**
   * Marks this block entity as dirty and sends an update to clients.
   */
  protected void sync() {
    setChanged();
    if (level != null && !level.isClientSide) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
  }

  /**
   * Alias for sync().
   */
  protected void markForUpdate() {
    sync();
  }

  private void invokeSuperSaveAdditional(CompoundTag tag, @Nullable HolderLookup.Provider provider) {
    try {
      if (provider != null && SAVE_ADDITIONAL_PROVIDER != null) {
        SAVE_ADDITIONAL_PROVIDER.invoke(this, tag, provider);
        return;
      }
      if (SAVE_ADDITIONAL_OLD != null) {
        SAVE_ADDITIONAL_OLD.invoke(this, tag);
      }
    } catch (ReflectiveOperationException ignored) {
    }
  }

  private void invokeSuperLoad(CompoundTag tag, @Nullable HolderLookup.Provider provider) {
    try {
      if (provider != null && LOAD_ADDITIONAL_PROVIDER != null) {
        LOAD_ADDITIONAL_PROVIDER.invoke(this, tag, provider);
        return;
      }
      if (LOAD_OLD != null) {
        LOAD_OLD.invoke(this, tag);
      }
    } catch (ReflectiveOperationException ignored) {
    }
  }

  private CompoundTag invokeSuperGetUpdateTag(@Nullable HolderLookup.Provider provider) {
    try {
      if (provider != null && GET_UPDATE_TAG_PROVIDER != null) {
        Object tag = GET_UPDATE_TAG_PROVIDER.invoke(this, provider);
        if (tag instanceof CompoundTag compoundTag) {
          return compoundTag;
        }
      }
      if (GET_UPDATE_TAG_OLD != null) {
        Object tag = GET_UPDATE_TAG_OLD.invoke(this);
        if (tag instanceof CompoundTag compoundTag) {
          return compoundTag;
        }
      }
    } catch (ReflectiveOperationException ignored) {
    }
    return new CompoundTag();
  }

  private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
    try {
      return owner.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
