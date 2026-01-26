package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    // TODO: Add interaction to use the book for linking
    // TODO: Add interaction to pick up the book
    // TODO: Add custom renderer
}
