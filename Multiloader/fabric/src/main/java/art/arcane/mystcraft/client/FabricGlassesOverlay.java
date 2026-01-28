package art.arcane.mystcraft.client;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.FabricModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** HUD overlay for Mystcraft Glasses showing Age information. */
public final class FabricGlassesOverlay {

    public static void register() {
        HudRenderCallback.EVENT.register(FabricGlassesOverlay::renderOverlay);
    }

    private static void renderOverlay(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != FabricModItems.GLASSES.get()) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        List<Component> lines = new ArrayList<>();

        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof ItemFrame frame) {
                ItemStack framedItem = frame.getItem();
                if (!framedItem.isEmpty() && framedItem.getItem() instanceof PageItem) {
                    var symbolId = Page.getSymbol(framedItem);
                    if (symbolId != null) {
                        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                        if (symbol != null) {
                            lines.add(Component.literal("Symbol: " + symbol.getLocalizedName()));
                            lines.add(Component.literal("Category: " + symbol.getCategory().getName()));
                            if (symbol.getCardRank() != null) {
                                lines.add(Component.literal("Rank: " + symbol.getCardRank()));
                            }
                        }
                    }
                }
            }
        }

        ResourceKey<Level> dimension = mc.level.dimension();
        if (AgeDimensionFactory.isMystcraftAge(dimension)) {
            int ageUID = getAgeUID(dimension);
            if (ageUID >= 0) {
                lines.add(Component.literal("Age UID: " + ageUID));
                float instability = ClientAgeDataCache.getInstability(ageUID);
                lines.add(Component.literal(String.format("Instability: %.1f", instability)));
            }
        }

        if (!lines.isEmpty()) {
            Font font = mc.font;
            int y = 5;
            int padding = 4;
            int bgColor = 0x80000000;

            for (Component line : lines) {
                int textWidth = font.width(line);
                graphics.fill(screenWidth - textWidth - padding * 2 - 2, y - 1,
                        screenWidth - 2, y + font.lineHeight + 1, bgColor);
                graphics.drawString(font, line, screenWidth - textWidth - padding - 2, y, 0x55FF55, false);
                y += font.lineHeight + 2;
            }
        }
    }

    private static int getAgeUID(ResourceKey<Level> dimension) {
        String path = dimension.location().getPath();
        if (path.startsWith("mystcraft_age_")) {
            try {
                return Integer.parseInt(path.substring("mystcraft_age_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private FabricGlassesOverlay() {}
}
