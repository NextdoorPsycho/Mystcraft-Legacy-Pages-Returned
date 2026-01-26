package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * The Linkbook entity.
 * Represents a dropped/placed linkbook in the world that can be used for linking.
 */
public class LinkbookEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> BOOK_ITEM =
            SynchedEntityData.defineId(LinkbookEntity.class, EntityDataSerializers.ITEM_STACK);

    public LinkbookEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public LinkbookEntity(Level level, double x, double y, double z) {
        this(ModEntities.LINKBOOK.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BOOK_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Book")) {
            setBookItem(ItemStack.of(tag.getCompound("Book")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack book = getBookItem();
        if (!book.isEmpty()) {
            tag.put("Book", book.save(new CompoundTag()));
        }
    }

    public ItemStack getBookItem() {
        return entityData.get(BOOK_ITEM);
    }

    public void setBookItem(ItemStack stack) {
        entityData.set(BOOK_ITEM, stack.copy());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack book = getBookItem();
        if (book.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            // Shift-click: Pick up the book
            if (!player.getInventory().add(book.copy())) {
                player.drop(book.copy(), false);
            }
            discard();
            return InteractionResult.CONSUME;
        } else {
            // Normal click: Open book screen or perform linking
            if (isValidLinkBook(book)) {
                if (level().isClientSide) {
                    openBookScreen(book);
                } else {
                    // On server, perform the link
                    CompoundTag linkData = book.getTag();
                    if (linkData != null) {
                        LinkingManager.performLink(player, linkData);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Checks if the book is a valid linking book.
     */
    private boolean isValidLinkBook(ItemStack book) {
        return book.getItem() instanceof LinkbookItem ||
               book.getItem() instanceof AgebookItem;
    }

    /**
     * Opens the book screen on the client.
     */
    private void openBookScreen(ItemStack book) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            art.arcane.mystcraft.client.screen.BookScreen.open(book));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        // Apply gravity if not on ground
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.98));
        }
    }
}
