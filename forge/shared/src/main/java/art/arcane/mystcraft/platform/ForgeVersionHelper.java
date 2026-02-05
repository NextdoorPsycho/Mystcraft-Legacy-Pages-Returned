package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IVersionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge 1.20.2 implementation of IVersionHelper.
 * Uses the newer SavedData.Factory API with DataFixTypes.
 */
public class ForgeVersionHelper implements IVersionHelper {

  @Override
  public String getMinecraftVersion() {
    return "1.20.2";
  }

  @Override
  public <T extends SavedData> T computeSavedData(
      ServerLevel level,
      Supplier<T> constructor,
      Function<CompoundTag, T> loader,
      String name
  ) {
    SavedData.Factory<T> factory = createFactory(constructor, loader);
    return level.getDataStorage().computeIfAbsent(factory, name);
  }

  @SuppressWarnings("unchecked")
  private static <T extends SavedData> SavedData.Factory<T> createFactory(
      Supplier<T> constructor,
      Function<CompoundTag, T> loader
  ) {
    try {
      Constructor<?> ctor = SavedData.Factory.class.getConstructor(
          Supplier.class,
          BiFunction.class,
          DataFixTypes.class
      );
      BiFunction<CompoundTag, Object, T> biLoader = (tag, provider) -> loader.apply(tag);
      return (SavedData.Factory<T>) ctor.newInstance(constructor, biLoader, DataFixTypes.LEVEL);
    } catch (ReflectiveOperationException ignored) {
    }

    try {
      Constructor<?> ctor = SavedData.Factory.class.getConstructor(
          Supplier.class,
          Function.class,
          DataFixTypes.class
      );
      return (SavedData.Factory<T>) ctor.newInstance(constructor, loader, DataFixTypes.LEVEL);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to create SavedData.Factory", e);
    }
  }

  @Override
  public boolean usesNewScrollAPI() {
    return true;
  }

  @Override
  public boolean usesNewRenderBackgroundAPI() {
    return true;
  }

  @Override
  public Level getEntityLevel(Entity entity) {
    return entity.level();
  }

  @Override
  public BlockPos blockPosContaining(Vec3 vec) {
    return BlockPos.containing(vec);
  }

  @Override
  public BlockPos blockPosContaining(double x, double y, double z) {
    return BlockPos.containing(x, y, z);
  }
}
