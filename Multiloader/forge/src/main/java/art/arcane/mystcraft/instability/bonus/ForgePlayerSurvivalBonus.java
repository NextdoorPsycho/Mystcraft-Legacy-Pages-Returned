package art.arcane.mystcraft.instability.bonus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Forge-specific event wiring for PlayerSurvivalBonus.
 * Delegates all game logic to the common PlayerSurvivalBonus class.
 */
public class ForgePlayerSurvivalBonus {

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

    /** Unregisters Forge event handlers. */
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
