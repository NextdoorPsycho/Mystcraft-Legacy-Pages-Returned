package art.arcane.mystcraft.instability.bonus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Forge-specific event wiring for PlayerKilledBonus.
 * Delegates all game logic to the common PlayerKilledBonus class.
 */
public class ForgePlayerKilledBonus {

    private final PlayerKilledBonus delegate;

    public ForgePlayerKilledBonus(PlayerKilledBonus delegate) {
        this.delegate = delegate;
        MinecraftForge.EVENT_BUS.register(this);
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

    /** Unregisters Forge event handlers. */
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
