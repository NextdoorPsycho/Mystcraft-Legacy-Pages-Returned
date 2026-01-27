package art.arcane.mystcraft.quilt;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import art.arcane.mystcraft.Mystcraft;

public final class MystcraftQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        Mystcraft.init();
    }
}
