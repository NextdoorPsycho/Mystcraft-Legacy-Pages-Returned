package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Block entity for the Mystcraft Lectern.
 * Extends BookstandBlockEntity with 90-degree yaw snapping.
 * Also accepts pages and filled maps, and updates map data to nearby players.
 */
public class LecternBlockEntity extends BookstandBlockEntity {

    /**
     * The yaw snap increment for lecterns (90 degrees).
     */
    private static final int LECTERN_YAW_SNAP = 90;

    public LecternBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LECTERN.get(), pos, blockState);
    }

    @Override
    protected int getYawSnap() {
        return LECTERN_YAW_SNAP;
    }

    /**
     * Checks if a stack is valid for the lectern.
     * Lecterns accept linkbooks, agebooks, pages, and filled maps.
     */
    public static boolean isValidLecternItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof LinkbookItem
                || stack.getItem() instanceof AgebookItem
                || stack.getItem() instanceof PageItem
                || stack.getItem() == Items.FILLED_MAP;
    }

    /**
     * Gets the display item (alias for getBook for API consistency).
     */
    @NotNull
    public ItemStack getDisplayItem() {
        return getBook();
    }

    /**
     * Called every tick to update map data for nearby players.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, LecternBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        ItemStack display = blockEntity.getDisplayItem();
        if (!display.isEmpty() && display.getItem() instanceof MapItem) {
            MapItemSavedData mapData = MapItem.getSavedData(display, level);
            if (mapData != null) {
                // Send map updates to all players in the level
                for (var player : level.players()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        mapData.tickCarriedBy(serverPlayer, display);

                        var updatePacket = mapData.getUpdatePacket(MapItem.getMapId(display), serverPlayer);
                        if (updatePacket != null) {
                            serverPlayer.connection.send(updatePacket);
                        }
                    }
                }
            }
        }
    }
}
