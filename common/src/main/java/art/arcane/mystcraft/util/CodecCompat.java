package art.arcane.mystcraft.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.Method;
import java.util.function.Function;

public final class CodecCompat {

  private static final Method SIMPLE_CODEC = findMethod(Block.class, "simpleCodec", Function.class);

  private CodecCompat() {
  }

  @SuppressWarnings("unchecked")
  public static <T extends Block> MapCodec<T> simpleCodec(Function<BlockBehaviour.Properties, T> factory) {
    if (SIMPLE_CODEC == null) {
      return null;
    }
    try {
      return (MapCodec<T>) SIMPLE_CODEC.invoke(null, factory);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
    try {
      return owner.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
