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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NeoForge 1.20.4 implementation of IVersionHelper.
 */
public class NeoForgeVersionHelper implements IVersionHelper {

  @Override
  public String getMinecraftVersion() {
    return "1.20.4";
  }

  @Override
  public <T extends SavedData> T computeSavedData(
      ServerLevel level,
      Supplier<T> constructor,
      Function<CompoundTag, T> loader,
      String name
  ) {
    SavedData.Factory<T> factory = new SavedData.Factory<>(
        constructor,
        loader,
        DataFixTypes.LEVEL
    );
    return level.getDataStorage().computeIfAbsent(factory, name);
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
