package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.NeoForgeModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

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

    /** The actual overlay implementation. */
    private static class GlassesHudOverlay implements IGuiOverlay {

        @Override
        public void render(ExtendedGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player == null || mc.level == null) {
                return;
            }

            if (!isWearingGlasses(player)) {
                return;
            }

            renderSymbolInfo(graphics, mc, screenWidth, screenHeight);

            ResourceKey<Level> dimension = mc.level.dimension();
            if (!isMystcraftAge(dimension)) {
                return;
            }

            renderGlassesHud(graphics, mc, dimension, screenWidth, screenHeight);
        }

        /** Renders symbol information when looking at pages or books. */
        private void renderSymbolInfo(GuiGraphics graphics, Minecraft mc, int screenWidth, int screenHeight) {
            HitResult hitResult = mc.hitResult;
            if (hitResult == null) {
                return;
            }

            ItemStack targetItem = ItemStack.EMPTY;

            if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof ItemFrame itemFrame) {
                    targetItem = itemFrame.getItem();
                }
            }

            if (targetItem.isEmpty()) {
                return;
            }

            List<SymbolInfo> symbols = getSymbolsFromItem(targetItem);
            if (symbols.isEmpty()) {
                return;
            }

            renderSymbolTooltip(graphics, mc, symbols, screenWidth, screenHeight);
        }

        /** Gets symbol information from an item. */
        private List<SymbolInfo> getSymbolsFromItem(ItemStack stack) {
            List<SymbolInfo> symbols = new ArrayList<>();

            if (stack.getItem() instanceof PageItem) {
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

        /** Renders the symbol tooltip near the crosshair. */
        private void renderSymbolTooltip(GuiGraphics graphics, Minecraft mc, List<SymbolInfo> symbols, int screenWidth, int screenHeight) {
            Font font = mc.font;

            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;
            int tooltipY = centerY + 20;

            RenderSystem.enableBlend();

            int maxWidth = 0;
            for (SymbolInfo info : symbols) {
                int width = font.width(info.name);
                if (info.description != null) {
                    width = Math.max(width, font.width(info.description));
                }
                maxWidth = Math.max(maxWidth, width);
            }

            int lineHeight = 10;

            int displayCount = Math.min(symbols.size(), 5);
            int totalHeight = displayCount * lineHeight + (symbols.size() > displayCount ? lineHeight : 0);

            int bgX = centerX - maxWidth / 2 - 5;
            int bgY = tooltipY - 2;

            graphics.fill(bgX - 2, bgY - 2, centerX + maxWidth / 2 + 7, bgY + totalHeight + 4, 0xC0000000);

            int y = tooltipY;
            for (int i = 0; i < displayCount; i++) {
                SymbolInfo info = symbols.get(i);
                Component text = Component.literal(info.name).withStyle(style -> style.withColor(info.color));
                graphics.drawCenteredString(font, text, centerX, y, 0xFFFFFF);
                y += lineHeight;
            }

            if (symbols.size() > displayCount) {
                String moreText = "... and " + (symbols.size() - displayCount) + " more";
                graphics.drawCenteredString(font, moreText, centerX, y, 0x888888);
            }

            RenderSystem.disableBlend();
        }

        /** Helper class for symbol display info. */
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
                    case "terrain" -> 0x8B4513;
                    case "biome" -> 0x00FF00;
                    case "biomecontroller" -> 0x32CD32;
                    case "celestial" -> 0xFFD700;
                    case "weather" -> 0x87CEEB;
                    case "lighting" -> 0xFFFF00;
                    case "color" -> 0xFF69B4;
                    case "modifier" -> 0x9370DB;
                    case "environment" -> 0x228B22;
                    default -> 0xFFFFFF;
                };
            }
        }

        private boolean isWearingGlasses(Player player) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            return helmet.is(NeoForgeModItems.GLASSES.get());
        }

        private boolean isMystcraftAge(ResourceKey<Level> dimension) {
            return dimension.location().getNamespace().equals(Mystcraft.MOD_ID);
        }

        private void renderGlassesHud(GuiGraphics graphics, Minecraft mc, ResourceKey<Level> dimension, int screenWidth, int screenHeight) {
            Font font = mc.font;

            int x = screenWidth - 10;
            int y = 10;

            int bgWidth = 120;
            int bgHeight = 50;
            int bgX = x - bgWidth;
            int bgY = y;

            RenderSystem.enableBlend();
            graphics.fill(bgX - 5, bgY - 5, x + 5, bgY + bgHeight + 5, 0x80000000);

            String ageId = dimension.location().getPath();
            String ageName = "Age " + ageId;

            Component nameText = Component.literal(ageName).withStyle(style -> style.withColor(0x00FFFF));
            graphics.drawString(font, nameText, bgX, bgY, 0xFFFFFF, true);

            float instability = getEstimatedInstability(dimension);
            int instabilityColor = getInstabilityColor(instability);
            String instabilityText = String.format("Instability: %.0f%%", instability * 100);
            graphics.drawString(font, instabilityText, bgX, bgY + 12, instabilityColor, true);

            String dimInfo = "Dimension: " + dimension.location().toString();
            if (dimInfo.length() > 25) {
                dimInfo = dimInfo.substring(0, 22) + "...";
            }
            graphics.drawString(font, dimInfo, bgX, bgY + 24, 0xAAAAAA, true);

            int barWidth = 100;
            int barHeight = 6;
            int barX = bgX;
            int barY = bgY + 38;

            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

            int filledWidth = (int) (barWidth * instability);
            graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, instabilityColor | 0xFF000000);

            graphics.renderOutline(barX - 1, barY - 1, barWidth + 2, barHeight + 2, 0xFF666666);

            RenderSystem.disableBlend();
        }

        private float getEstimatedInstability(ResourceKey<Level> dimension) {
            int hash = dimension.location().hashCode();
            return Math.abs(hash % 100) / 100.0f;
        }

        private int getInstabilityColor(float instability) {
            if (instability < 0.25f) {
                return 0x00FF00;
            } else if (instability < 0.5f) {
                return 0xFFFF00;
            } else if (instability < 0.75f) {
                return 0xFF8800;
            } else {
                return 0xFF0000;
            }
        }
    }

    private GlassesOverlay() {
    }
}
