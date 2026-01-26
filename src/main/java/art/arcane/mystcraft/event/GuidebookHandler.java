package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles giving the Mystcraft Guidebook to new players on their first spawn.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class GuidebookHandler {

    private static final String GUIDEBOOK_GIVEN_TAG = "mystcraft:guidebook_given";

    /**
     * Give the guidebook to players on first login.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check if config allows giving guidebook on first spawn
        if (!MystcraftConfig.giveGuidebookOnFirstSpawn.get()) return;

        // Check if we've already given this player a guidebook
        if (player.getPersistentData().getBoolean(GUIDEBOOK_GIVEN_TAG)) return;

        // Mark that we've given the guidebook
        player.getPersistentData().putBoolean(GUIDEBOOK_GIVEN_TAG, true);

        // Create and give the guidebook
        ItemStack guidebook = new ItemStack(ModItems.GUIDEBOOK.get());
        if (!player.getInventory().add(guidebook)) {
            // If inventory is full, drop it at the player's feet
            player.drop(guidebook, false);
        }

        Mystcraft.LOGGER.debug("Gave Mystcraft guidebook to new player: {}", player.getName().getString());
    }
}
