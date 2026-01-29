package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

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

    /** Vignette overlay that shows instability level. */
    private static class InstabilityVignetteOverlay implements IGuiOverlay {

        private float currentIntensity = 0.0f;
        private float targetIntensity = 0.0f;
        private long lastUpdateTime = 0;

        @Override
        public void render(ExtendedGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level == null || mc.player == null) {
                currentIntensity = 0;
                return;
            }

            ResourceKey<Level> dimension = mc.level.dimension();

            if (!dimension.location().getNamespace().equals(Mystcraft.MOD_ID)) {
                targetIntensity = 0;
                updateIntensity();
                if (currentIntensity < 0.01f) {
                    return;
                }
            } else {
                float instability = getInstability(dimension);
                targetIntensity = Math.min(instability / 200.0f, 1.0f);
                updateIntensity();
            }

            if (currentIntensity < 0.01f) {
                return;
            }

            renderInstabilityOverlay(graphics, screenWidth, screenHeight);
        }

        private void updateIntensity() {
            float delta = (targetIntensity - currentIntensity) * 0.05f;
            currentIntensity += delta;
            currentIntensity = Mth.clamp(currentIntensity, 0, 1);
        }

        private float getInstability(ResourceKey<Level> dimension) {
            int ageUID = AgeDimensionFactory.getAgeUID(dimension);
            if (ageUID > 0) {
                return ClientAgeDataCache.getInstability(ageUID);
            }
            return 0.0f;
        }

        private void renderInstabilityOverlay(GuiGraphics graphics, int screenWidth, int screenHeight) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            float alpha = currentIntensity * 0.3f;

            int r, g, b;
            if (currentIntensity < 0.25f) {
                r = 0;
                g = 50;
                b = 0;
            } else if (currentIntensity < 0.5f) {
                r = 50;
                g = 50;
                b = 0;
            } else if (currentIntensity < 0.75f) {
                r = 80;
                g = 40;
                b = 0;
            } else {
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5);
                r = (int) (100 + pulse * 50);
                g = 0;
                b = 0;
                alpha = currentIntensity * (0.3f + pulse * 0.2f);
            }

            int color = ((int) (alpha * 255) << 24) | (r << 16) | (g << 8) | b;

            renderVignette(graphics, screenWidth, screenHeight, color);

            if (currentIntensity > 0.75f) {
                long time = System.currentTimeMillis();
                if ((time / 50) % 20 == 0) {
                    int flashAlpha = (int) ((currentIntensity - 0.75f) * 100);
                    graphics.fill(0, 0, screenWidth, screenHeight, (flashAlpha << 24) | 0xFFFFFF);
                }
            }

            RenderSystem.disableBlend();
        }

        private void renderVignette(GuiGraphics graphics, int screenWidth, int screenHeight, int color) {
            int edgeWidth = screenWidth / 8;
            int edgeHeight = screenHeight / 8;

            for (int i = 0; i < edgeWidth; i++) {
                float gradientAlpha = 1.0f - (float) i / edgeWidth;
                int gradientColor = applyAlpha(color, gradientAlpha);
                graphics.fill(i, 0, i + 1, screenHeight, gradientColor);
            }

            for (int i = 0; i < edgeWidth; i++) {
                float gradientAlpha = (float) i / edgeWidth;
                int gradientColor = applyAlpha(color, 1.0f - gradientAlpha);
                graphics.fill(screenWidth - edgeWidth + i, 0, screenWidth - edgeWidth + i + 1, screenHeight, gradientColor);
            }

            for (int i = 0; i < edgeHeight; i++) {
                float gradientAlpha = 1.0f - (float) i / edgeHeight;
                int gradientColor = applyAlpha(color, gradientAlpha * 0.5f);
                graphics.fill(0, i, screenWidth, i + 1, gradientColor);
            }

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
