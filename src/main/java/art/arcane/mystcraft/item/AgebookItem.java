package art.arcane.mystcraft.item;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.event.AgeDataSyncHandler;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Descriptive Book (Agebook) item.
 * Contains the complete description of an Age and allows travel to it.
 * Ages are created when the book is first used with a link panel.
 */
public class AgebookItem extends Item {

    private static final String TAG_PAGES = "Pages";
    private static final String TAG_AUTHORS = "Authors";

    public AgebookItem(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public Rarity getRarity(@NotNull ItemStack stack) {
        return stack.isEnchanted() ? Rarity.RARE : Rarity.EPIC;
    }

    @Override
    @NotNull
    public Component getName(@NotNull ItemStack stack) {
        if (stack.getTag() != null) {
            String displayName = LinkOptions.getDisplayName(stack.getTag());
            if (!"???".equals(displayName)) {
                return Component.literal(displayName);
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.getTag() != null) {
            Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
            if (dimId != null) {
                tooltip.add(Component.translatable("item.mystcraft.agebook.age", dimId));
            } else {
                tooltip.add(Component.translatable("item.mystcraft.agebook.unwritten"));
            }

            Collection<String> authors = getAuthors(stack);
            if (!authors.isEmpty()) {
                tooltip.add(Component.translatable("item.mystcraft.agebook.authors",
                        String.join(", ", authors)));
            }

            List<ItemStack> pages = getPageList(stack);
            if (!pages.isEmpty()) {
                tooltip.add(Component.translatable("item.mystcraft.agebook.pages", pages.size()));
            }
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Open book viewing screen on client
            if (level.isClientSide) {
                art.arcane.mystcraft.client.screen.BookScreen.open(stack);
            }
        } else {
            // Perform linking on server
            if (!level.isClientSide) {
                activate(stack, level, player);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Activates the book, potentially creating a new Age.
     */
    private void activate(ItemStack stack, Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
        }

        Integer dimId = LinkOptions.getDimensionUID(stack.getTag());

        if (dimId == null) {
            // This is a new book - check if it has a link panel
            List<ItemStack> pages = getPageList(stack);
            if (!pages.isEmpty() && Page.isLinkPanel(pages.get(0))) {
                // Create a new Age dimension
                createAge(stack, serverLevel, serverPlayer);
            } else {
                player.displayClientMessage(Component.translatable("item.mystcraft.agebook.no_panel"), true);
            }
        } else {
            // Existing Age - perform linking
            linkToAge(stack, serverLevel, serverPlayer);
        }
    }

    /**
     * Creates a new Age dimension for this Agebook.
     */
    private void createAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
        player.displayClientMessage(Component.translatable("item.mystcraft.agebook.creating"), true);

        // Extract symbols from pages
        List<ItemStack> pages = getPageList(stack);
        List<IAgeSymbol> symbols = extractSymbols(pages);

        // Generate a seed from the player's position and time
        long seed = System.currentTimeMillis() ^ player.blockPosition().asLong();

        // Build the Age using the grammar system
        AgeBuilder builder = new AgeBuilder(symbols, seed);
        AgeDirectorImpl director = builder.build();

        // Allocate a new age UID
        AgeManager ageManager = AgeManager.get(level);
        int ageUID = ageManager.allocateUID();

        // Generate a UUID for this age
        java.util.UUID ageUUID = java.util.UUID.randomUUID();

        // Create the dimension with director configuration
        ServerLevel ageLevel = AgeDimensionFactory.createAgeDimension(
                level.getServer(), ageUID, ageUUID, director);

        if (ageLevel == null) {
            player.displayClientMessage(Component.translatable("item.mystcraft.agebook.creation_failed"), true);
            return;
        }

        // Register the age with AgeManager so it can be found later
        ResourceLocation dimLoc = new ResourceLocation("mystcraft", "mystcraft_age_" + ageUID);
        ageManager.registerAge(ageUID, dimLoc, ageUUID);

        // Initialize the AgeData
        AgeData ageData = AgeData.get(ageLevel);
        ageData.setAgeUID(ageUID);
        ageData.setAgeUUID(ageUUID);
        ageData.setAgeName(getDisplayName(stack));
        for (String author : getAuthors(stack)) {
            ageData.addAuthor(author);
        }
        ageData.setPages(pages);

        // Copy ALL configuration from director (including instability)
        ageData.copyFromDirector(director);

        // Force sync Age data to the player BEFORE they teleport
        // This ensures colors, instability, and other visual data is available on the client
        AgeDataSyncHandler.syncAgeDataToPlayer(player, ageLevel);

        // Set spawn point at the center of spawn chunk
        net.minecraft.core.BlockPos spawn = ageLevel.getSharedSpawnPos();
        ageData.setSpawn(spawn.getX(), spawn.getY() + 1, spawn.getZ());

        // Update the book with the Age's dimension ID and spawn
        LinkOptions.setDimensionUID(stack.getTag(), ageUID);
        LinkOptions.setSpawn(stack.getTag(), spawn.above());
        LinkOptions.setUUID(stack.getTag(), ageUUID);

        // Report creation with instability info
        if (!builder.isComplete()) {
            player.displayClientMessage(Component.translatable("item.mystcraft.agebook.created_incomplete",
                    ageUID, String.format("%.1f", builder.getInstability())), true);
        } else {
            player.displayClientMessage(Component.translatable("item.mystcraft.agebook.created", ageUID), true);
        }

        // Link to the newly created Age
        linkToAge(stack, level, player);
    }

    /**
     * Extracts IAgeSymbol objects from a list of pages.
     */
    private List<IAgeSymbol> extractSymbols(List<ItemStack> pages) {
        List<IAgeSymbol> symbols = new ArrayList<>();
        for (ItemStack page : pages) {
            if (Page.isLinkPanel(page)) {
                continue; // Link panels are not symbols
            }
            ResourceLocation symbolId = Page.getSymbol(page);
            if (symbolId != null) {
                IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }
        return symbols;
    }

    /**
     * Links the player to the Age described in this book.
     */
    private void linkToAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
        CompoundTag linkData = stack.getTag();
        if (linkData == null) {
            return;
        }

        Integer ageUID = LinkOptions.getDimensionUID(linkData);
        if (ageUID == null) {
            player.displayClientMessage(Component.translatable("item.mystcraft.agebook.no_age"), true);
            return;
        }

        player.displayClientMessage(Component.translatable("item.mystcraft.agebook.linking"), true);

        // Perform the link
        LinkingManager.LinkResult result = LinkingManager.performLink(player, linkData);

        if (result != LinkingManager.LinkResult.SUCCESS) {
            player.displayClientMessage(Component.translatable("item.mystcraft.agebook.link_failed", result.name()), true);
        }
    }

    /**
     * Creates an Agebook from pages.
     */
    public static void create(ItemStack agebook, Player player, List<ItemStack> pages, String title) {
        agebook.setTag(new CompoundTag());

        AgebookItem item = (AgebookItem) agebook.getItem();
        item.addPages(agebook, pages);
        item.addAuthor(agebook, player);
        item.setDisplayName(agebook, title);

        if (!pages.isEmpty()) {
            ItemStack linkpanel = pages.get(0);
            if (Page.isLinkPanel(linkpanel)) {
                Page.applyLinkPanel(linkpanel, agebook);
            }
        }
    }

    /**
     * Checks if this is a new (unlinked) Agebook.
     */
    public static boolean isNewAgebook(ItemStack stack) {
        if (!(stack.getItem() instanceof AgebookItem)) {
            return false;
        }
        if (stack.getTag() == null) {
            return false;
        }
        Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
        if (dimId != null) {
            return false;
        }
        List<ItemStack> pages = ((AgebookItem) stack.getItem()).getPageList(stack);
        return !pages.isEmpty() && Page.isLinkPanel(pages.get(0));
    }

    /**
     * Gets the list of pages in this book.
     */
    public List<ItemStack> getPageList(ItemStack stack) {
        if (stack.getTag() == null) {
            return Collections.emptyList();
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
        List<ItemStack> pages = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            pages.add(ItemStack.of(listTag.getCompound(i)));
        }
        return pages;
    }

    /**
     * Adds pages to this book.
     */
    public void addPages(ItemStack stack, Collection<ItemStack> pages) {
        if (stack.getTag() == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
        for (ItemStack page : pages) {
            listTag.add(page.save(new CompoundTag()));
        }
        tag.put(TAG_PAGES, listTag);
    }

    /**
     * Sets the page list.
     */
    public void setPageList(ItemStack stack, List<ItemStack> pages) {
        if (stack.getTag() == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = new ListTag();
        for (ItemStack page : pages) {
            listTag.add(page.save(new CompoundTag()));
        }
        tag.put(TAG_PAGES, listTag);
    }

    /**
     * Adds an author to this book.
     */
    public void addAuthor(ItemStack stack, Player player) {
        if (stack.getTag() == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
        String playerName = player.getGameProfile().getName();
        boolean found = false;
        for (int i = 0; i < listTag.size(); i++) {
            if (listTag.getString(i).equals(playerName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            listTag.add(net.minecraft.nbt.StringTag.valueOf(playerName));
            tag.put(TAG_AUTHORS, listTag);
        }
    }

    /**
     * Gets the authors of this book.
     */
    public Collection<String> getAuthors(ItemStack stack) {
        if (stack.getTag() == null) {
            return Collections.emptyList();
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
        List<String> authors = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            authors.add(listTag.getString(i));
        }
        return authors;
    }

    /**
     * Sets the display name of the book.
     */
    public void setDisplayName(ItemStack stack, String name) {
        LinkOptions.setDisplayName(stack.getOrCreateTag(), name);
    }

    /**
     * Gets the display name of the book.
     */
    public String getDisplayName(ItemStack stack) {
        return LinkOptions.getDisplayName(stack.getTag());
    }

    /**
     * Agebooks with a linked Age have a foil effect to show they're active.
     */
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        // Show foil if the book has an Age (dimension) linked
        return stack.getTag() != null && LinkOptions.getDimensionUID(stack.getTag()) != null;
    }
}
