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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The Linkbook entity.
 * Represents a dropped/placed linkbook in the world that can be used for linking.
 * Features damage/decay system for survival mechanics.
 */
public class LinkbookEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> BOOK_ITEM =
            SynchedEntityData.defineId(LinkbookEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> BOOK_HEALTH =
            SynchedEntityData.defineId(LinkbookEntity.class, EntityDataSerializers.FLOAT);

    // Damage constants
    private static final float MAX_HEALTH = 5.0f;
    private static final int DECAY_DURATION_TICKS = 20 * 30;
    private static final float DECAY_DAMAGE_PER_TICK = MAX_HEALTH / DECAY_DURATION_TICKS;
    private static float decayMultiplier = 1.0f;
    private static final float FIRE_DAMAGE = 5.0f;
    private static final float COLLISION_DAMAGE = 10.0f; // 2x fire damage
    private static final float DROWNING_DAMAGE = 20.0f; // Quick damage

    private int ticksExisted = 0;
    private int ticksOnGround = 0;

    /** Visual hurt time for rendering red tint when damaged */
    public int hurtTime = 0;

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
        ticksOnGround = tag.getInt("TicksOnGround");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack book = getBookItem();
        if (!book.isEmpty()) {
            tag.put("Book", book.save(new CompoundTag()));
        }
        tag.putFloat("Health", getHealth());
        tag.putInt("TicksExisted", ticksExisted);
        tag.putInt("TicksOnGround", ticksOnGround);
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
                    art.arcane.mystcraft.client.screen.BookScreen.openForEntity(book, this.getId());
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

            // Process ground decay
            processGroundDecay();

            // Check for death
            if (getHealth() <= 0) {
                destroyBook();
            }
        }

    }

    // --- Health/Damage System ---

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
    private void processGroundDecay() {
        ItemStack book = getBookItem();
        if (!book.isEmpty() && book.getTag() != null && book.getTag().getBoolean("NoDecay")) {
            return;
        }
        if (onGround() && level().getFluidState(blockPosition()).isEmpty()) {
            ticksOnGround++;
            damageBook(DECAY_DAMAGE_PER_TICK * decayMultiplier);
        } else {
            ticksOnGround = 0;
        }
    }

    public static void setDecayMultiplierForTests(float multiplier) {
        decayMultiplier = Math.max(0.01f, multiplier);
    }

    /**
     * Destroys the book when health reaches zero.
     */
    private void destroyBook() {
        if (!level().isClientSide) {
            ItemStack page = art.arcane.mystcraft.registry.ModItems.PAGE != null
                    ? new ItemStack(art.arcane.mystcraft.registry.ModItems.PAGE.get())
                    : ItemStack.EMPTY;
            if (!page.isEmpty()) {
                level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level(), getX(), getY(), getZ(), page));
            }
            level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(net.minecraft.world.item.Items.LEATHER)));
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return false;
        }

        // Set hurt time for visual red tint (10 ticks)
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

}
