package art.arcane.mystcraft.fabric;

import net.fabricmc.api.ModInitializer;

import art.arcane.mystcraft.Mystcraft;

public final class MystcraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Mystcraft.init();
    }
}
