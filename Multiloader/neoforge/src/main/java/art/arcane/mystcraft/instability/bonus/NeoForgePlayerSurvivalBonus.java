package art.arcane.mystcraft.instability.bonus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * NeoForge-specific event wiring for PlayerSurvivalBonus.
 * Delegates all game logic to the common PlayerSurvivalBonus class.
 */
public class NeoForgePlayerSurvivalBonus {

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

    /** Unregisters NeoForge event handlers. */
    public void cleanup() {
        NeoForge.EVENT_BUS.unregister(this);
    }
}
