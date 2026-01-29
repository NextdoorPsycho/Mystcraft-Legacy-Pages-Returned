package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

/**
 * Forge 1.20.1-specific platform helper.
 * Uses NetworkHooks.openScreen() instead of IForgeServerPlayer.openMenu().
 */
public class ForgePlatformHelper_1_20_1 implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
        if (extraDataWriter == null) {
            NetworkHooks.openScreen(player, provider);
        } else {
            NetworkHooks.openScreen(player, provider, extraDataWriter);
        }
    }
}
