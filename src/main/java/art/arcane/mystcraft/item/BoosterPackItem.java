package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The Booster Pack (Sealed Notebook) item.
 * Contains random symbol pages when opened.
 * Used to give players new symbols to discover.
 */
public class BoosterPackItem extends Item {

    private static final int PAGES_PER_PACK = 3;

    public BoosterPackItem(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Generate random pages
            List<ItemStack> pages = generatePages(level, player);

            // Give pages to player
            for (ItemStack page : pages) {
                if (!player.getInventory().add(page)) {
                    player.drop(page, false);
                }
            }

            // Consume the booster pack
            stack.shrink(1);

            player.displayClientMessage(
                    Component.translatable("item.mystcraft.booster.opened", pages.size()),
                    true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Generates random pages for this booster pack.
     */
    private List<ItemStack> generatePages(Level level, Player player) {
        List<ItemStack> pages = new ArrayList<>();

        // TODO: Implement proper random symbol generation from SymbolManager
        // For now, just generate blank pages and link panels

        for (int i = 0; i < PAGES_PER_PACK; i++) {
            if (level.random.nextFloat() < 0.3f) {
                // 30% chance of link panel
                pages.add(Page.createLinkPage());
            } else {
                // 70% chance of blank page (will be symbol page when SymbolManager implemented)
                pages.add(Page.createPage());
            }
        }

        return pages;
    }
}
