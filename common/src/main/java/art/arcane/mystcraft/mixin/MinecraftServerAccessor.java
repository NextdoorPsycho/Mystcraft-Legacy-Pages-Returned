package art.arcane.mystcraft.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Mapping-aware access to the two server fields required for dynamic Age
 * registration. Keeping this in a Mixin accessor avoids runtime field-name
 * reflection and lets each loader remap the targets normally.
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

  @Accessor("levels")
  Map<ResourceKey<Level>, ServerLevel> mystcraft$getLevels();

  @Accessor("storageSource")
  LevelStorageSource.LevelStorageAccess mystcraft$getStorageSource();
}
