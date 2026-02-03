package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.instability.bonus.PlayerKilledBonus;
import art.arcane.mystcraft.instability.bonus.PlayerSurvivalBonus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * NeoForge-specific event wiring for instability bonuses.
 * Consolidates PlayerKilledBonus and PlayerSurvivalBonus event handling.
 */
public class NeoForgeInstabilityBonuses {

  private NeoForgeInstabilityBonuses() {
  }

  public static class NeoForgePlayerKilledBonus {
    private final PlayerKilledBonus delegate;

    public NeoForgePlayerKilledBonus(PlayerKilledBonus delegate) {
      this.delegate = delegate;
      NeoForge.EVENT_BUS.register(this);
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
      NeoForge.EVENT_BUS.unregister(this);
    }
  }

  public static class NeoForgePlayerSurvivalBonus {
    private final PlayerSurvivalBonus delegate;

    public NeoForgePlayerSurvivalBonus(PlayerSurvivalBonus delegate) {
      this.delegate = delegate;
      NeoForge.EVENT_BUS.register(this);
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
      NeoForge.EVENT_BUS.unregister(this);
    }
  }
}
