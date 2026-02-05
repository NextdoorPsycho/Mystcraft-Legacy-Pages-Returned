package art.arcane.mystcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.lang.reflect.Method;

public final class NbtCompat {

  private static final Method READ_BLOCK_POS_KEY =
      findMethod(NbtUtils.class, "readBlockPos", CompoundTag.class, String.class);
  private static final Method READ_BLOCK_POS_TAG =
      findMethod(NbtUtils.class, "readBlockPos", CompoundTag.class);

  private NbtCompat() {
  }

  public static BlockPos readBlockPos(CompoundTag tag, String key) {
    if (READ_BLOCK_POS_KEY != null) {
      BlockPos pos = invoke(READ_BLOCK_POS_KEY, tag, key);
      return pos != null ? pos : BlockPos.ZERO;
    }
    if (READ_BLOCK_POS_TAG != null) {
      CompoundTag inner = tag.getCompound(key);
      BlockPos pos = invoke(READ_BLOCK_POS_TAG, inner);
      return pos != null ? pos : BlockPos.ZERO;
    }
    return BlockPos.ZERO;
  }

  private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
    try {
      return owner.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Method method, Object... args) {
    if (method == null) {
      return null;
    }
    try {
      return (T) method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
