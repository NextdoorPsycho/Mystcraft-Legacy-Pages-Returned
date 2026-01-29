package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.FallingBlockHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge hook to prevent gravity blocks from falling in Mystcraft Ages when disabled.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class ForgeFallingBlockHandler {

  private ForgeFallingBlockHandler() {
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }
    if (!(event.getEntity() instanceof FallingBlockEntity fallingBlock)) {
      return;
    }
    if (FallingBlockHandler.handle(serverLevel, fallingBlock)) {
      event.setCanceled(true);
    }
  }
}
