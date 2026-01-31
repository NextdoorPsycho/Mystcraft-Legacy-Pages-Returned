package art.arcane.mystcraft.instability.bonus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge-specific event wiring for instability bonuses.
 * Consolidates PlayerKilledBonus and PlayerSurvivalBonus event handling.
 */
public class ForgeInstabilityBonuses {

  private ForgeInstabilityBonuses() {
  }

  public static class ForgePlayerKilledBonus {
    private final PlayerKilledBonus delegate;

    public ForgePlayerKilledBonus(PlayerKilledBonus delegate) {
      this.delegate = delegate;
      MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
      if (!(event.getEntity() instanceof Player player)) return;
      Player killer = event.getSource().getEntity() instanceof Player k ? k : null;
      delegate.onPlayerDeath(player, killer);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
        delegate.onPlayerLogin(player);
      }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
        delegate.onPlayerChangedDimension(player, event.getTo());
      }
    }

    public void cleanup() {
      MinecraftForge.EVENT_BUS.unregister(this);
    }
  }

  public static class ForgePlayerSurvivalBonus {
    private final PlayerSurvivalBonus delegate;

    public ForgePlayerSurvivalBonus(PlayerSurvivalBonus delegate) {
      this.delegate = delegate;
      MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof Player player) {
        delegate.onPlayerDeath(player);
      }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
        delegate.onPlayerLogin(player);
      }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
        delegate.onPlayerLogout(player);
      }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
        delegate.onPlayerChangedDimension(player, event.getFrom(), event.getTo());
      }
    }

    public void cleanup() {
      MinecraftForge.EVENT_BUS.unregister(this);
    }
  }
}
