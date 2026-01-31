package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IVersionHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric 1.19.2 implementation of IVersionHelper.
 * Uses the older SavedData API without DataFixTypes.
 */
public class FabricVersionHelper_1_19_2 implements IVersionHelper {

  @Override
  public String getMinecraftVersion() {
    return "1.19.2";
  }

  @Override
  @SuppressWarnings("deprecation")
  public <T extends SavedData> T computeSavedData(
      ServerLevel level,
      Supplier<T> constructor,
      Function<CompoundTag, T> loader,
      String name
  ) {
    // 1.19.2 uses the older API: computeIfAbsent(Function<CompoundTag, T>, Supplier<T>, String)
    return level.getDataStorage().computeIfAbsent(loader, constructor, name);
  }

  @Override
  public boolean usesNewScrollAPI() {
    // 1.19.2 uses 3-parameter mouseScrolled(mouseX, mouseY, scrollDelta)
    return false;
  }

  @Override
  public boolean usesNewRenderBackgroundAPI() {
    // 1.19.2 uses 1-parameter renderBackground(graphics)
    return false;
  }
}
