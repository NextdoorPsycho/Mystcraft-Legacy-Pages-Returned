package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.world.GuidebookData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Handles giving the Mystcraft Guidebook to new players on their first spawn.
 * Tracks which players have received the book using server-level saved data.
 */
public class GuidebookHandler {

  /**
   * Gives the guidebook to a player on first login if configured.
   */
  public static void onPlayerLoggedIn(ServerPlayer player) {
    if (!MystcraftConfig.giveGuidebookOnFirstSpawn.get()) return;

    MinecraftServer server = player.getServer();
    if (server == null) return;

    GuidebookData data = GuidebookData.get(server);
    if (data.hasReceived(player.getUUID())) return;

    ItemStack guidebook = new ItemStack(ModItems.GUIDEBOOK.get());
    if (!player.getInventory().add(guidebook)) {
      player.drop(guidebook, false);
    }

    data.markReceived(player.getUUID());
    Mystcraft.LOGGER.debug("Gave Mystcraft guidebook to new player: {}", player.getName().getString());
  }
}
