package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD overlay that displays when wearing Mystcraft Glasses.
 * Shows Age information and instability levels when in a Mystcraft Age.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GlassesOverlay {

    private static final String OVERLAY_ID = "glasses_overlay";

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(OVERLAY_ID, new GlassesHudOverlay());
        Mystcraft.LOGGER.info("Registered glasses HUD overlay");
    }

    /**
     * The actual overlay implementation.
     */
    private static class GlassesHudOverlay implements IGuiOverlay {

        @Override
        public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player == null || mc.level == null) {
                return;
            }

            // Check if player is wearing glasses
            if (!isWearingGlasses(player)) {
                return;
            }

            // Check if in a Mystcraft Age
            ResourceKey<Level> dimension = mc.level.dimension();
            if (!isMystcraftAge(dimension)) {
                return;
            }

            // Render the overlay
            renderGlassesHud(graphics, mc, dimension, screenWidth, screenHeight);
        }

        private boolean isWearingGlasses(Player player) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            return helmet.is(ModItems.GLASSES.get());
        }

        private boolean isMystcraftAge(ResourceKey<Level> dimension) {
            // Check if this dimension belongs to Mystcraft
            return dimension.location().getNamespace().equals(Mystcraft.MOD_ID);
        }

        private void renderGlassesHud(GuiGraphics graphics, Minecraft mc, ResourceKey<Level> dimension, int screenWidth, int screenHeight) {
            Font font = mc.font;

            // Position in top-right corner
            int x = screenWidth - 10;
            int y = 10;

            // Semi-transparent background
            int bgWidth = 120;
            int bgHeight = 50;
            int bgX = x - bgWidth;
            int bgY = y;

            RenderSystem.enableBlend();
            graphics.fill(bgX - 5, bgY - 5, x + 5, bgY + bgHeight + 5, 0x80000000);

            // Get age data from server (simplified - in full implementation would sync from server)
            String ageId = dimension.location().getPath();
            String ageName = "Age " + ageId;

            // Render age name
            Component nameText = Component.literal(ageName).withStyle(style -> style.withColor(0x00FFFF));
            graphics.drawString(font, nameText, bgX, bgY, 0xFFFFFF, true);

            // Render instability indicator
            // In a full implementation, this would read actual instability from synced data
            float instability = getEstimatedInstability(dimension);
            int instabilityColor = getInstabilityColor(instability);
            String instabilityText = String.format("Instability: %.0f%%", instability * 100);
            graphics.drawString(font, instabilityText, bgX, bgY + 12, instabilityColor, true);

            // Render dimension info
            String dimInfo = "Dimension: " + dimension.location().toString();
            if (dimInfo.length() > 25) {
                dimInfo = dimInfo.substring(0, 22) + "...";
            }
            graphics.drawString(font, dimInfo, bgX, bgY + 24, 0xAAAAAA, true);

            // Visual indicator bar for instability
            int barWidth = 100;
            int barHeight = 6;
            int barX = bgX;
            int barY = bgY + 38;

            // Background bar
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

            // Filled portion
            int filledWidth = (int) (barWidth * instability);
            graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, instabilityColor | 0xFF000000);

            // Border
            graphics.renderOutline(barX - 1, barY - 1, barWidth + 2, barHeight + 2, 0xFF666666);

            RenderSystem.disableBlend();
        }

        private float getEstimatedInstability(ResourceKey<Level> dimension) {
            // In a full implementation, this would sync from server
            // For now, return a placeholder based on dimension hash
            int hash = dimension.location().hashCode();
            return Math.abs(hash % 100) / 100.0f;
        }

        private int getInstabilityColor(float instability) {
            if (instability < 0.25f) {
                return 0x00FF00; // Green - stable
            } else if (instability < 0.5f) {
                return 0xFFFF00; // Yellow - moderate
            } else if (instability < 0.75f) {
                return 0xFF8800; // Orange - unstable
            } else {
                return 0xFF0000; // Red - very unstable
            }
        }
    }

    private GlassesOverlay() {
    }
}
