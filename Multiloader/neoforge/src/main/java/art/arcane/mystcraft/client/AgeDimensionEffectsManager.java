package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Manages registration of custom DimensionSpecialEffects for Mystcraft Ages.
 *
 * All Mystcraft age dimension types use "effects": "mystcraft:age" in their JSON.
 * A single shared AgeDimensionSpecialEffects instance is registered under that key
 * using RegisterDimensionSpecialEffectsEvent.
 * The effects instance dynamically resolves the current age UID at render time.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AgeDimensionEffectsManager {

    private static final ResourceLocation AGE_EFFECTS_KEY = new ResourceLocation(Mystcraft.MOD_ID, "age");

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(AGE_EFFECTS_KEY, new AgeDimensionSpecialEffects());
        Mystcraft.LOGGER.info("Registered shared DimensionSpecialEffects under key '{}'", AGE_EFFECTS_KEY);
    }
}
