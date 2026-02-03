package art.arcane.mystcraft.platform.services;

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
 * Abstracts version-specific Minecraft APIs that changed between 1.20.1 and 1.20.2.
 */
public interface IVersionHelper {

  /**
   * Gets the Minecraft version this helper targets.
   *
   * @return Version string like "1.20.1" or "1.20.2"
   */
  String getMinecraftVersion();

  /**
   * Computes or loads SavedData using the version-appropriate API.
   * <p>
   * In 1.20.1: Uses computeIfAbsent(Function, Supplier, String)
   * In 1.20.2: Uses computeIfAbsent(SavedData.Factory, String)
   *
   * @param level       The server level to get data storage from
   * @param constructor Supplier for creating new instances
   * @param loader      Function to load from NBT
   * @param name        The data name
   * @param <T>         SavedData type
   * @return The SavedData instance
   */
  <T extends SavedData> T computeSavedData(
      ServerLevel level,
      Supplier<T> constructor,
      Function<CompoundTag, T> loader,
      String name
  );

  /**
   * Checks if the screen API uses the 4-parameter mouseScrolled method.
   *
   * @return true for 1.20.2+ (4 params: mouseX, mouseY, scrollX, scrollY),
   * false for 1.20.1 (3 params: mouseX, mouseY, scrollDelta)
   */
  default boolean usesNewScrollAPI() {
    return true;
  }

  /**
   * Checks if the screen API uses the 4-parameter renderBackground method.
   *
   * @return true for 1.20.2+ (4 params: graphics, mouseX, mouseY, partialTick),
   * false for 1.20.1 (1 param: graphics)
   */
  default boolean usesNewRenderBackgroundAPI() {
    return true;
  }

  /**
   * Gets the level from an entity.
   * <p>
   * 1.20.x: {@code entity.level()}
   * 1.19.x: {@code entity.getLevel()}
   *
   * @param entity The entity
   * @return The entity's level
   */
  Level getEntityLevel(Entity entity);

  /**
   * Gets the server level from an entity, if it exists on the server.
   * <p>
   * 1.20.x: {@code entity.level() instanceof ServerLevel sl ? sl : null}
   * 1.19.x: {@code entity.getLevel() instanceof ServerLevel sl ? sl : null}
   *
   * @param entity The entity
   * @return The server level, or null if client-side
   */
  default ServerLevel getServerLevel(Entity entity) {
    Level level = getEntityLevel(entity);
    return level instanceof ServerLevel sl ? sl : null;
  }

  /**
   * Creates a BlockPos containing the given Vec3 coordinates.
   * <p>
   * 1.20.x: {@code BlockPos.containing(vec)}
   * 1.19.x: {@code new BlockPos(vec)}
   *
   * @param vec The position vector
   * @return A BlockPos at those coordinates
   */
  BlockPos blockPosContaining(Vec3 vec);

  /**
   * Creates a BlockPos containing the given coordinates.
   * <p>
   * 1.20.x: {@code BlockPos.containing(x, y, z)}
   * 1.19.x: {@code new BlockPos(x, y, z)}
   *
   * @param x The x coordinate
   * @param y The y coordinate
   * @param z The z coordinate
   * @return A BlockPos at those coordinates
   */
  BlockPos blockPosContaining(double x, double y, double z);
}
