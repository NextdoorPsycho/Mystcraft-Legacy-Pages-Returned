package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeServerPlayer;

import java.util.function.Consumer;

public class ForgePlatformHelper implements IPlatformHelper {

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
        IForgeServerPlayer forgePlayer = (IForgeServerPlayer) player;
        if (extraDataWriter == null) {
            forgePlayer.openMenu(provider, (Consumer<FriendlyByteBuf>) null);
            return;
        }
        forgePlayer.openMenu(provider, extraDataWriter);
    }
}
