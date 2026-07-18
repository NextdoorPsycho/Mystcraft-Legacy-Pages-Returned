package art.arcane.mystcraft.mixin;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mapping-aware access to the server state needed while installing a dynamic
 * Age. Mixin accessors keep these names inside the normal loader remapping
 * pipeline instead of relying on fragile runtime reflection.
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

  @Accessor("levels")
  Map<ResourceKey<Level>, ServerLevel> mystcraft$getLevels();

  @Accessor("storageSource")
  LevelStorageSource.LevelStorageAccess mystcraft$getStorageSource();
}
