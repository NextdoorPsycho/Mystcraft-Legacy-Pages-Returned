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
 * Centralizes Minecraft API calls that are sensitive to mappings or loader setup.
 */
public interface IVersionHelper {

  /**
   * Gets the Minecraft version this helper targets.
   *
   * @return Target Minecraft version string.
   */
  String getMinecraftVersion();

  /**
   * Computes or loads SavedData using the supported 1.20.1 API.
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
   * Checks if the screen API uses the newer 4-parameter mouseScrolled method.
   *
   * @return false for the supported 1.20.1 API.
   */
  default boolean usesNewScrollAPI() {
    return false;
  }

  /**
   * Checks if the screen API uses the newer 4-parameter renderBackground method.
   *
   * @return false for the supported 1.20.1 API.
   */
  default boolean usesNewRenderBackgroundAPI() {
    return false;
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
