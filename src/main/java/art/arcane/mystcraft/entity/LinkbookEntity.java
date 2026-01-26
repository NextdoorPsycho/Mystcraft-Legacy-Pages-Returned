package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Linkbook entity.
 * Represents a dropped/placed linkbook in the world that can be used for linking.
 * Features damage/decay system for survival mechanics and hopper/minecart support.
 */
public class LinkbookEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> BOOK_ITEM =
            SynchedEntityData.defineId(LinkbookEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> BOOK_HEALTH =
            SynchedEntityData.defineId(LinkbookEntity.class, EntityDataSerializers.FLOAT);

    // Damage constants (matching legacy)
    private static final float MAX_HEALTH = 100.0f;
    private static final float FIRE_DAMAGE = 5.0f;
    private static final float COLLISION_DAMAGE = 10.0f; // 2x fire damage
    private static final float DROWNING_DAMAGE = 20.0f; // Quick damage
    private static final float STARVATION_DAMAGE = 1.0f;
    private static final int STARVATION_INTERVAL = 10000; // Every 10k ticks

    private int ticksExisted = 0;
    private int lastStarvationTick = 0;

    /** Visual hurt time for rendering red tint when damaged (legacy behavior) */
    public int hurtTime = 0;

    // Item handler for hopper/minecart interaction
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!getStackInSlot(0).isEmpty()) {
                setBookItem(getStackInSlot(0));
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem;
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!getBookItem().isEmpty()) {
                return stack; // Already has a book
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                setBookItem(ItemStack.EMPTY);
            }
            return extracted;
        }
    };
    private final LazyOptional<IItemHandler> itemHandlerLazy = LazyOptional.of(() -> itemHandler);

    public LinkbookEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public LinkbookEntity(Level level, double x, double y, double z) {
        this(ModEntities.LINKBOOK.get(), level);
        setPos(x, y, z);
        setHealth(MAX_HEALTH);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BOOK_ITEM, ItemStack.EMPTY);
        entityData.define(BOOK_HEALTH, MAX_HEALTH);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Book")) {
            setBookItem(ItemStack.of(tag.getCompound("Book")));
        }
        if (tag.contains("Health")) {
            setHealth(tag.getFloat("Health"));
        }
        ticksExisted = tag.getInt("TicksExisted");
        lastStarvationTick = tag.getInt("LastStarvationTick");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack book = getBookItem();
        if (!book.isEmpty()) {
            tag.put("Book", book.save(new CompoundTag()));
        }
        tag.putFloat("Health", getHealth());
        tag.putInt("TicksExisted", ticksExisted);
        tag.putInt("LastStarvationTick", lastStarvationTick);
    }

    public ItemStack getBookItem() {
        return entityData.get(BOOK_ITEM);
    }

    public void setBookItem(ItemStack stack) {
        entityData.set(BOOK_ITEM, stack.copy());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack book = getBookItem();
        if (book.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            // Shift-click: Pick up the book (server only)
            if (!level().isClientSide) {
                if (!player.getInventory().add(book.copy())) {
                    player.drop(book.copy(), false);
                }
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        } else {
            // Normal click: Open book screen (client only)
            // Server waits for EntityBookActivatePacket from GUI
            if (isValidLinkBook(book)) {
                if (level().isClientSide) {
                    // On client, open the book viewing screen with entity ID
                    openBookScreen(book);
                }
                // Server does nothing here - waits for packet from GUI
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
    }

    /**
     * Activates the book for the given entity (called from EntityBookActivatePacket).
     * Performs the actual linking based on book type.
     */
    public void activateBook(Entity entity) {
        ItemStack book = getBookItem();
        if (book.isEmpty()) {
            return;
        }

        if (book.getItem() instanceof LinkbookItem linkbook) {
            linkbook.activate(book, level(), entity);
        } else if (book.getItem() instanceof AgebookItem agebook) {
            agebook.activate(book, level(), entity);
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
     * Opens the book screen on the client with entity ID for proper activation.
     */
    private void openBookScreen(ItemStack book) {
        int entityId = this.getId();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            art.arcane.mystcraft.client.screen.BookScreen.openForEntity(book, entityId));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        ticksExisted++;

        // Decrement hurt time for visual effect
        if (hurtTime > 0) {
            hurtTime--;
        }

        // Apply gravity if not on ground
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.98));
        }

        if (!level().isClientSide && !getBookItem().isEmpty()) {
            // Process environmental damage
            processEnvironmentalDamage();

            // Process starvation decay
            processStarvationDecay();

            // Check for death
            if (getHealth() <= 0) {
                destroyBook();
            }
        }

        // Sync item handler with book item
        ItemStack book = getBookItem();
        if (!book.isEmpty() && itemHandler.getStackInSlot(0).isEmpty()) {
            itemHandler.setStackInSlot(0, book.copy());
        }
    }

    // ========================= Health/Damage System =========================

    /**
     * Gets the current health of the book.
     */
    public float getHealth() {
        return entityData.get(BOOK_HEALTH);
    }

    /**
     * Sets the health of the book.
     */
    public void setHealth(float health) {
        entityData.set(BOOK_HEALTH, Math.max(0, Math.min(MAX_HEALTH, health)));
    }

    /**
     * Damages the book by the specified amount.
     */
    public void damageBook(float amount) {
        setHealth(getHealth() - amount);
    }

    /**
     * Processes environmental damage (fire, water, collision).
     */
    private void processEnvironmentalDamage() {
        // Fire damage
        if (isOnFire()) {
            damageBook(FIRE_DAMAGE);
        }

        // Drowning damage (in water or any fluid)
        FluidState fluidState = level().getFluidState(blockPosition());
        if (!fluidState.isEmpty()) {
            damageBook(DROWNING_DAMAGE);
        }

        // Collision damage (from entities hitting the book)
        if (ticksExisted % 20 == 0) { // Check every second
            AABB box = getBoundingBox().inflate(0.1);
            List<Entity> entities = level().getEntities(this, box, e -> e != this && !(e instanceof Player));
            if (!entities.isEmpty()) {
                for (Entity entity : entities) {
                    // Check if entity is moving fast enough to cause damage
                    double speed = entity.getDeltaMovement().length();
                    if (speed > 0.2) {
                        damageBook(COLLISION_DAMAGE * (float) speed);
                    }
                }
            }
        }
    }

    /**
     * Processes slow starvation decay over time.
     */
    private void processStarvationDecay() {
        if (ticksExisted - lastStarvationTick >= STARVATION_INTERVAL) {
            damageBook(STARVATION_DAMAGE);
            lastStarvationTick = ticksExisted;
        }
    }

    /**
     * Destroys the book when health reaches zero.
     */
    private void destroyBook() {
        // Drop damaged book or destroy it
        ItemStack book = getBookItem();
        if (!book.isEmpty()) {
            // Mark book as damaged if it has durability, otherwise just destroy
            if (book.isDamageableItem()) {
                book.setDamageValue(book.getMaxDamage());
            }
            // Optionally drop the damaged book
            // spawnAtLocation(book);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return false;
        }

        // Set hurt time for visual red tint (10 ticks like legacy)
        hurtTime = 10;

        // Check for fire damage using tags
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            damageBook(amount * 2);
            return true;
        }

        // Check for explosion damage using tags
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            damageBook(amount * 3);
            return true;
        }

        // General damage
        damageBook(amount);
        return true;
    }

    @Override
    public boolean fireImmune() {
        return false; // Books can burn
    }

    // ========================= Capability System (Hopper/Minecart Support) =========================

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerLazy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerLazy.invalidate();
    }

    /**
     * Checks if the book can be picked up by a hopper minecart.
     */
    public boolean canBePickedUpByMinecart(AbstractMinecart minecart) {
        return !getBookItem().isEmpty();
    }

    /**
     * Gets the book item for hopper extraction.
     */
    public ItemStack extractBook() {
        ItemStack book = getBookItem();
        if (!book.isEmpty()) {
            setBookItem(ItemStack.EMPTY);
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            return book;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Inserts a book from a hopper.
     */
    public boolean insertBook(ItemStack book) {
        if (getBookItem().isEmpty() && isValidLinkBook(book)) {
            setBookItem(book);
            itemHandler.setStackInSlot(0, book.copy());
            return true;
        }
        return false;
    }
}
