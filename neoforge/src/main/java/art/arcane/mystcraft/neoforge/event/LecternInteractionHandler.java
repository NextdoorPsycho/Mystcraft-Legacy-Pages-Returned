package art.arcane.mystcraft.neoforge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;

/**
 * Handles Mystcraft book interactions with vanilla lecterns using NeoForge events.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LecternInteractionHandler {

    private static Field bookField;
    private static Field pageCountField;

    static {
        try {
            bookField = LecternBlockEntity.class.getDeclaredField("book");
            bookField.setAccessible(true);
            pageCountField = LecternBlockEntity.class.getDeclaredField("pageCount");
            pageCountField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Mystcraft.LOGGER.error("[LecternHandler] Failed to find LecternBlockEntity fields", e);
        }
    }

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
                        if (bookField != null && pageCountField != null) {
                            bookField.set(lectern, book);
                            pageCountField.set(lectern, pageCount);
                        }
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
