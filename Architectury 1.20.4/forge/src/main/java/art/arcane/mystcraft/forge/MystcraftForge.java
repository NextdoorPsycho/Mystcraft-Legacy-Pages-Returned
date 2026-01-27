package art.arcane.mystcraft.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import art.arcane.mystcraft.Mystcraft;

@Mod(Mystcraft.MOD_ID)
public final class MystcraftForge {
    public MystcraftForge() {
        EventBuses.registerModEventBus(Mystcraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Mystcraft.init();
    }
}
