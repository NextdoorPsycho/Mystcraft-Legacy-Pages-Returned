package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IVersionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric 1.20.1 implementation of IVersionHelper.
 * Uses the legacy SavedData API without DataFixTypes.
 */
public class FabricVersionHelper_1_20_1 implements IVersionHelper {

  @Override
  public String getMinecraftVersion() {
    return "1.20.1";
  }

  @Override
  public <T extends SavedData> T computeSavedData(
      ServerLevel level,
      Supplier<T> constructor,
      Function<CompoundTag, T> loader,
      String name
  ) {
    return level.getDataStorage().computeIfAbsent(loader, constructor, name);
  }

  @Override
  public boolean usesNewScrollAPI() {
    return false;
  }

  @Override
  public boolean usesNewRenderBackgroundAPI() {
    return false;
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
