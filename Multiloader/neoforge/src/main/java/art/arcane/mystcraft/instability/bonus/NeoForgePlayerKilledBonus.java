package art.arcane.mystcraft.instability.bonus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * NeoForge-specific event wiring for PlayerKilledBonus.
 * Delegates all game logic to the common PlayerKilledBonus class.
 */
public class NeoForgePlayerKilledBonus {

    private final PlayerKilledBonus delegate;

    public NeoForgePlayerKilledBonus(PlayerKilledBonus delegate) {
        this.delegate = delegate;
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Player killer = null;
        if (event.getSource().getEntity() instanceof Player killerPlayer) {
            killer = killerPlayer;
        }
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

    /** Unregisters NeoForge event handlers. */
    public void cleanup() {
        NeoForge.EVENT_BUS.unregister(this);
    }
}
