package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Handles Mystcraft book interactions with vanilla lecterns using Forge events.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LecternInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Only handle vanilla lecterns
        if (!(state.getBlock() instanceof LecternBlock)) {
            return;
        }

        var result = MystcraftLecternHelper.handleLecternInteraction(
                level, pos, state, event.getEntity(), event.getHand(),
                (lectern, book, pageCount) -> {
                    try {
                        ObfuscationReflectionHelper.setPrivateValue(
                                LecternBlockEntity.class, lectern, book, "f_59527_");
                        ObfuscationReflectionHelper.setPrivateValue(
                                LecternBlockEntity.class, lectern, pageCount, "f_59529_");
                    } catch (Exception e) {
                        Mystcraft.LOGGER.error("[LecternHandler] Failed to set book fields", e);
                    }
                });

        if (result.handled) {
            event.setCanceled(true);
            event.setCancellationResult(result.result);
        }
    }
}
