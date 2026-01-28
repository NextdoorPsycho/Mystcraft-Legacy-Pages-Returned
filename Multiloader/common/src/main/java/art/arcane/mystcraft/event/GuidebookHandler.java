package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.PlayerDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles giving the Mystcraft Guidebook to new players on their first spawn.
 * Tracks which players have received the book using server-level saved data.
 */
public class GuidebookHandler {

    private static final Set<UUID> playersGiven = new HashSet<>();

    /**
     * Gives the guidebook to a player on first login if configured.
     */
    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (!MystcraftConfig.giveGuidebookOnFirstSpawn.get()) return;

        if (playersGiven.contains(player.getUUID())) return;
        playersGiven.add(player.getUUID());

        // Create and give the guidebook
        ItemStack guidebook = new ItemStack(ModItems.GUIDEBOOK.get());
        if (!player.getInventory().add(guidebook)) {
            player.drop(guidebook, false);
        }

        Mystcraft.LOGGER.debug("Gave Mystcraft guidebook to new player: {}", player.getName().getString());
    }

    /**
     * Clears tracked data on server stop.
     */
    public static void onServerStopped() {
        playersGiven.clear();
    }
}
