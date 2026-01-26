package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

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

            // Check if looking at a page/book and show symbol info
            renderSymbolInfo(graphics, mc, screenWidth, screenHeight);

            // Check if in a Mystcraft Age for the dimension HUD
            ResourceKey<Level> dimension = mc.level.dimension();
            if (!isMystcraftAge(dimension)) {
                return;
            }

            // Render the age overlay
            renderGlassesHud(graphics, mc, dimension, screenWidth, screenHeight);
        }

        /**
         * Renders symbol information when looking at pages or books.
         */
        private void renderSymbolInfo(GuiGraphics graphics, Minecraft mc, int screenWidth, int screenHeight) {
            // Check what the player is looking at
            HitResult hitResult = mc.hitResult;
            if (hitResult == null) {
                return;
            }

            ItemStack targetItem = ItemStack.EMPTY;

            // Check if looking at an item frame
            if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof ItemFrame itemFrame) {
                    targetItem = itemFrame.getItem();
                }
            }

            // If not looking at anything with a symbol, check held items (main hand for reference)
            if (targetItem.isEmpty()) {
                return;
            }

            // Check if it's a page or book with symbols
            List<SymbolInfo> symbols = getSymbolsFromItem(targetItem);
            if (symbols.isEmpty()) {
                return;
            }

            // Render symbol info at crosshair
            renderSymbolTooltip(graphics, mc, symbols, screenWidth, screenHeight);
        }

        /**
         * Gets symbol information from an item.
         */
        private List<SymbolInfo> getSymbolsFromItem(ItemStack stack) {
            List<SymbolInfo> symbols = new ArrayList<>();

            if (stack.getItem() instanceof PageItem) {
                // Single page
                ResourceLocation symbolId = Page.getSymbol(stack);
                if (symbolId != null) {
                    IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                    if (symbol != null) {
                        symbols.add(new SymbolInfo(symbol));
                    }
                } else if (Page.isLinkPanel(stack)) {
                    symbols.add(new SymbolInfo("Link Panel", 0x00FFFF, "Enables travel to an Age"));
                }
            } else if (stack.getItem() instanceof AgebookItem agebookItem) {
                // Agebook - show all symbols from pages
                List<ItemStack> pages = agebookItem.getPageList(stack);
                for (ItemStack page : pages) {
                    if (Page.isLinkPanel(page)) {
                        symbols.add(new SymbolInfo("Link Panel", 0x00FFFF, "Enables travel to an Age"));
                    } else {
                        ResourceLocation symbolId = Page.getSymbol(page);
                        if (symbolId != null) {
                            IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                            if (symbol != null) {
                                symbols.add(new SymbolInfo(symbol));
                            }
                        }
                    }
                }
            }

            return symbols;
        }

        /**
         * Renders the symbol tooltip near the crosshair.
         */
        private void renderSymbolTooltip(GuiGraphics graphics, Minecraft mc, List<SymbolInfo> symbols, int screenWidth, int screenHeight) {
            Font font = mc.font;

            // Position below crosshair
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;
            int tooltipY = centerY + 20;

            RenderSystem.enableBlend();

            // Calculate background size
            int maxWidth = 0;
            for (SymbolInfo info : symbols) {
                int width = font.width(info.name);
                if (info.description != null) {
                    width = Math.max(width, font.width(info.description));
                }
                maxWidth = Math.max(maxWidth, width);
            }

            int lineHeight = 10;
            int totalHeight = symbols.size() * lineHeight + (symbols.size() > 1 ? 5 : 0);

            // Limit to 5 symbols shown
            int displayCount = Math.min(symbols.size(), 5);
            totalHeight = displayCount * lineHeight + (symbols.size() > displayCount ? lineHeight : 0);

            int bgX = centerX - maxWidth / 2 - 5;
            int bgY = tooltipY - 2;

            // Semi-transparent background
            graphics.fill(bgX - 2, bgY - 2, centerX + maxWidth / 2 + 7, bgY + totalHeight + 4, 0xC0000000);

            // Render symbols
            int y = tooltipY;
            for (int i = 0; i < displayCount; i++) {
                SymbolInfo info = symbols.get(i);
                Component text = Component.literal(info.name).withStyle(style -> style.withColor(info.color));
                graphics.drawCenteredString(font, text, centerX, y, 0xFFFFFF);
                y += lineHeight;
            }

            // Show "and X more..." if there are more symbols
            if (symbols.size() > displayCount) {
                String moreText = "... and " + (symbols.size() - displayCount) + " more";
                graphics.drawCenteredString(font, moreText, centerX, y, 0x888888);
            }

            RenderSystem.disableBlend();
        }

        /**
         * Helper class for symbol display info.
         */
        private static class SymbolInfo {
            final String name;
            final int color;
            final String description;

            SymbolInfo(IAgeSymbol symbol) {
                this.name = symbol.getLocalizedName();
                this.color = getCategoryColor(symbol.getCategory().getName());
                this.description = null;
            }

            SymbolInfo(String name, int color, String description) {
                this.name = name;
                this.color = color;
                this.description = description;
            }

            private static int getCategoryColor(String category) {
                return switch (category.toLowerCase()) {
                    case "terrain" -> 0x8B4513; // Brown
                    case "biome" -> 0x00FF00;   // Green
                    case "biomecontroller" -> 0x32CD32; // Lime green
                    case "celestial" -> 0xFFD700; // Gold
                    case "weather" -> 0x87CEEB;  // Sky blue
                    case "lighting" -> 0xFFFF00; // Yellow
                    case "color" -> 0xFF69B4;    // Pink
                    case "modifier" -> 0x9370DB; // Purple
                    case "environment" -> 0x228B22; // Forest green
                    default -> 0xFFFFFF;         // White
                };
            }
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
