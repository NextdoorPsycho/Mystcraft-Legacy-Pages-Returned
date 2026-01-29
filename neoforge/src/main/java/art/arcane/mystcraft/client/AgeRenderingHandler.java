package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import net.minecraft.client.renderer.FogRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side rendering handler for Age visual effects.
 * Handles fog color and fog density based on Age symbols.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public class AgeRenderingHandler {

    /** Tracks which age UIDs have already logged fog info to avoid per-frame spam. */
    private static final Set<Integer> LOGGED_FOG_AGES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Modifies fog color based on Age configuration. */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID < 0) return;

        int fogColor = ClientAgeDataCache.getFogColor(ageUID);
        if (fogColor != -1) {
            float r = ((fogColor >> 16) & 0xFF) / 255.0f;
            float g = ((fogColor >> 8) & 0xFF) / 255.0f;
            float b = (fogColor & 0xFF) / 255.0f;

            event.setRed(r);
            event.setGreen(g);
            event.setBlue(b);
        }

        // Apply lighting type modifications
        String lightingType = ClientAgeDataCache.getLightingType(ageUID);
        switch (lightingType) {
            case "bright" -> {
                event.setRed(Math.min(1.0f, event.getRed() * 1.2f));
                event.setGreen(Math.min(1.0f, event.getGreen() * 1.2f));
                event.setBlue(Math.min(1.0f, event.getBlue() * 1.2f));
            }
            case "dark" -> {
                event.setRed(event.getRed() * 0.5f);
                event.setGreen(event.getGreen() * 0.5f);
                event.setBlue(event.getBlue() * 0.5f);
            }
        }

        // Log once per age for fog pipeline tracing
        if (LOGGED_FOG_AGES.add(ageUID)) {
            Mystcraft.LOGGER.info("[FogRender] Age {}: fogColor=0x{}, lighting={}, applied R={} G={} B={}",
                    ageUID,
                    fogColor != -1 ? Integer.toHexString(fogColor) : "none",
                    lightingType,
                    String.format("%.3f", event.getRed()),
                    String.format("%.3f", event.getGreen()),
                    String.format("%.3f", event.getBlue()));
        }
    }

    /** Modifies fog density based on Age lighting type. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID < 0) return;

        String lightingType = ClientAgeDataCache.getLightingType(ageUID);
        if ("dark".equals(lightingType)) {
            if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
                event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.5f);
                event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.7f);
                event.setCanceled(true);
            }
        }
    }
}
