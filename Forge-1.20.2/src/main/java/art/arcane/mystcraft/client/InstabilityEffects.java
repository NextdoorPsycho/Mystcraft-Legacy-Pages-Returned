package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders visual effects on the screen when in an unstable Mystcraft Age.
 * Effects include screen distortion, color tinting, and vignette.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InstabilityEffects {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Instability vignette overlay removed - instability is communicated
        // through gameplay effects rather than screen overlays
    }

    /**
     * Vignette overlay that shows instability level.
     */
    private static class InstabilityVignetteOverlay implements IGuiOverlay {

        private float currentIntensity = 0.0f;
        private float targetIntensity = 0.0f;
        private long lastUpdateTime = 0;

        @Override
        public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level == null || mc.player == null) {
                currentIntensity = 0;
                return;
            }

            ResourceKey<Level> dimension = mc.level.dimension();

            // Only apply in Mystcraft ages
            if (!dimension.location().getNamespace().equals(Mystcraft.MOD_ID)) {
                // Smoothly fade out when leaving
                targetIntensity = 0;
                updateIntensity();
                if (currentIntensity < 0.01f) {
                    return;
                }
            } else {
                // Get instability level for this age
                float instability = getInstability(dimension);
                // Normalize instability to 0-1 range for visual effects
                // Instability values can range from 0 to 200+, so we normalize against a max of 200
                // This gives a smooth progression: 0 = none, 50 = low, 100 = medium, 150 = high, 200+ = max
                targetIntensity = Math.min(instability / 200.0f, 1.0f);
                updateIntensity();
            }

            if (currentIntensity < 0.01f) {
                return;
            }

            renderInstabilityOverlay(graphics, screenWidth, screenHeight);
        }

        private void updateIntensity() {
            // Smooth transition
            float delta = (targetIntensity - currentIntensity) * 0.05f;
            currentIntensity += delta;
            currentIntensity = Mth.clamp(currentIntensity, 0, 1);
        }

        private float getInstability(ResourceKey<Level> dimension) {
            int ageUID = getAgeUID(dimension);
            if (ageUID > 0) {
                return ClientAgeDataCache.getInstability(ageUID);
            }
            return 0.0f;
        }

        private int getAgeUID(ResourceKey<Level> dimension) {
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

        private void renderInstabilityOverlay(GuiGraphics graphics, int screenWidth, int screenHeight) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Intensity controls how strong the effect is
            float alpha = currentIntensity * 0.3f;

            // Choose color based on instability level
            int r, g, b;
            if (currentIntensity < 0.25f) {
                // Low instability - subtle green tint
                r = 0;
                g = 50;
                b = 0;
            } else if (currentIntensity < 0.5f) {
                // Medium - yellow tint
                r = 50;
                g = 50;
                b = 0;
            } else if (currentIntensity < 0.75f) {
                // High - orange tint
                r = 80;
                g = 40;
                b = 0;
            } else {
                // Very high - red tint with pulsing
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5);
                r = (int) (100 + pulse * 50);
                g = 0;
                b = 0;
                alpha = currentIntensity * (0.3f + pulse * 0.2f);
            }

            int color = ((int) (alpha * 255) << 24) | (r << 16) | (g << 8) | b;

            // Render vignette effect (darker at edges)
            renderVignette(graphics, screenWidth, screenHeight, color);

            // Add flickering effect for very high instability
            if (currentIntensity > 0.75f) {
                long time = System.currentTimeMillis();
                if ((time / 50) % 20 == 0) {
                    // Brief white flash
                    int flashAlpha = (int) ((currentIntensity - 0.75f) * 100);
                    graphics.fill(0, 0, screenWidth, screenHeight, (flashAlpha << 24) | 0xFFFFFF);
                }
            }

            RenderSystem.disableBlend();
        }

        private void renderVignette(GuiGraphics graphics, int screenWidth, int screenHeight, int color) {
            // Simple vignette by drawing semi-transparent rectangles at edges
            int edgeWidth = screenWidth / 8;
            int edgeHeight = screenHeight / 8;

            // Left edge
            for (int i = 0; i < edgeWidth; i++) {
                float gradientAlpha = 1.0f - (float) i / edgeWidth;
                int gradientColor = applyAlpha(color, gradientAlpha);
                graphics.fill(i, 0, i + 1, screenHeight, gradientColor);
            }

            // Right edge
            for (int i = 0; i < edgeWidth; i++) {
                float gradientAlpha = (float) i / edgeWidth;
                int gradientColor = applyAlpha(color, 1.0f - gradientAlpha);
                graphics.fill(screenWidth - edgeWidth + i, 0, screenWidth - edgeWidth + i + 1, screenHeight, gradientColor);
            }

            // Top edge
            for (int i = 0; i < edgeHeight; i++) {
                float gradientAlpha = 1.0f - (float) i / edgeHeight;
                int gradientColor = applyAlpha(color, gradientAlpha * 0.5f);
                graphics.fill(0, i, screenWidth, i + 1, gradientColor);
            }

            // Bottom edge
            for (int i = 0; i < edgeHeight; i++) {
                float gradientAlpha = (float) i / edgeHeight;
                int gradientColor = applyAlpha(color, (1.0f - gradientAlpha) * 0.5f);
                graphics.fill(0, screenHeight - edgeHeight + i, screenWidth, screenHeight - edgeHeight + i + 1, gradientColor);
            }
        }

        private int applyAlpha(int color, float multiplier) {
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            a = (int) (a * multiplier);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private InstabilityEffects() {
    }
}
