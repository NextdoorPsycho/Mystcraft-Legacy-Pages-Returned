package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.registry.ModEntities;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * A server-owned body proxy left behind while a player is inside their personal
 * pocket.
 */
public class PersonalPocketProxyEntity extends LivingEntity {

  private static final EntityDataAccessor<String> OWNER_ID =
      SynchedEntityData.defineId(PersonalPocketProxyEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> OWNER_NAME =
      SynchedEntityData.defineId(PersonalPocketProxyEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> OWNER_TEXTURE =
      SynchedEntityData.defineId(PersonalPocketProxyEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> OWNER_TEXTURE_SIGNATURE =
      SynchedEntityData.defineId(PersonalPocketProxyEntity.class, EntityDataSerializers.STRING);

  private static final String TAG_OWNER_ID = "OwnerId";
  private static final String TAG_OWNER_NAME = "OwnerName";
  private static final String TAG_OWNER_TEXTURE = "OwnerTexture";
  private static final String TAG_OWNER_TEXTURE_SIGNATURE = "OwnerTextureSignature";
  private static final String TAG_RETURN_LINK = "ReturnLink";
  private static final String TEXTURES_PROPERTY = "textures";

  private CompoundTag returnLink;

  public PersonalPocketProxyEntity(EntityType<? extends PersonalPocketProxyEntity> type, Level level) {
    super(type, level);
    setNoGravity(true);
  }

  public PersonalPocketProxyEntity(Level level) {
    this(ModEntities.PERSONAL_POCKET_PROXY.get(), level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return LivingEntity.createLivingAttributes()
        .add(Attributes.MAX_HEALTH, 20.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.0D)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
  }

  @Nullable
  private static Property firstTextureProperty(GameProfile profile) {
    if (profile == null) {
      return null;
    }
    Collection<Property> textures = profile.properties().get(TEXTURES_PROPERTY);
    if (textures == null || textures.isEmpty()) {
      return null;
    }
    return textures.iterator().next();
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(OWNER_ID, "");
    builder.define(OWNER_NAME, "");
    builder.define(OWNER_TEXTURE, "");
    builder.define(OWNER_TEXTURE_SIGNATURE, "");
  }

  public void setOwner(GameProfile ownerProfile, @Nullable CompoundTag returnLink) {
    Property texture = firstTextureProperty(ownerProfile);
    setOwner(
        ownerProfile.id(),
        ownerProfile.name(),
        texture == null ? "" : texture.value(),
        texture != null && texture.hasSignature() ? texture.signature() : "",
        returnLink
    );
  }

  public void setOwner(UUID ownerId, String ownerName, @Nullable CompoundTag returnLink) {
    setOwner(ownerId, ownerName, "", "", returnLink);
  }

  public void setOwner(UUID ownerId, String ownerName, String ownerTexture, String ownerTextureSignature,
                       @Nullable CompoundTag returnLink) {
    entityData.set(OWNER_ID, ownerId.toString());
    entityData.set(OWNER_NAME, ownerName == null ? "" : ownerName);
    entityData.set(OWNER_TEXTURE, ownerTexture == null ? "" : ownerTexture);
    entityData.set(OWNER_TEXTURE_SIGNATURE, ownerTextureSignature == null ? "" : ownerTextureSignature);
    this.returnLink = returnLink == null ? null : returnLink.copy();
    if (ownerName != null && !ownerName.isBlank()) {
      setCustomName(Component.literal(ownerName));
      setCustomNameVisible(true);
    }
  }

  @Nullable
  public UUID getOwnerId() {
    String value = entityData.get(OWNER_ID);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  public String getOwnerName() {
    return entityData.get(OWNER_NAME);
  }

  @Nullable
  public GameProfile getOwnerProfile() {
    UUID ownerId = getOwnerId();
    if (ownerId == null) {
      return null;
    }
    String ownerName = getOwnerName();
    String profileName = ownerName == null || ownerName.isBlank() ? ownerId.toString() : ownerName;
    String texture = entityData.get(OWNER_TEXTURE);
    if (texture == null || texture.isBlank()) {
      return new GameProfile(ownerId, profileName);
    }
    String signature = entityData.get(OWNER_TEXTURE_SIGNATURE);
    Property property = signature == null || signature.isBlank()
        ? new Property(TEXTURES_PROPERTY, texture)
        : new Property(TEXTURES_PROPERTY, texture, signature);
    PropertyMap properties = new PropertyMap(
        ImmutableMultimap.of(TEXTURES_PROPERTY, property));
    return new GameProfile(ownerId, profileName, properties);
  }

  @Nullable
  public CompoundTag getReturnLink() {
    return returnLink == null ? null : returnLink.copy();
  }

  @Override
  public void tick() {
    super.tick();
    setNoGravity(true);
    setDeltaMovement(Vec3.ZERO);
    fallDistance = 0.0F;
  }

  @Override
  public boolean isPickable() {
    return true;
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public boolean isAttackable() {
    return true;
  }

  @Override
  public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    return PersonalPocketEscapeHandler.ripOwnerOutFromProxy(this, source);
  }

  @Override
  public ItemStack getItemBySlot(EquipmentSlot slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
  }

  @Override
  public HumanoidArm getMainArm() {
    return HumanoidArm.RIGHT;
  }

  @Override
  protected void readAdditionalSaveData(ValueInput input) {
    super.readAdditionalSaveData(input);
    entityData.set(OWNER_ID, input.getStringOr(TAG_OWNER_ID, ""));
    String ownerName = input.getStringOr(TAG_OWNER_NAME, "");
    entityData.set(OWNER_NAME, ownerName);
    if (!ownerName.isBlank()) {
      setCustomName(Component.literal(ownerName));
      setCustomNameVisible(true);
    }
    entityData.set(OWNER_TEXTURE, input.getStringOr(TAG_OWNER_TEXTURE, ""));
    entityData.set(OWNER_TEXTURE_SIGNATURE, input.getStringOr(TAG_OWNER_TEXTURE_SIGNATURE, ""));
    returnLink = input.read(TAG_RETURN_LINK, CompoundTag.CODEC)
        .map(CompoundTag::copy)
        .orElse(null);
  }

  @Override
  protected void addAdditionalSaveData(ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putString(TAG_OWNER_ID, entityData.get(OWNER_ID));
    output.putString(TAG_OWNER_NAME, entityData.get(OWNER_NAME));
    if (!entityData.get(OWNER_TEXTURE).isBlank()) {
      output.putString(TAG_OWNER_TEXTURE, entityData.get(OWNER_TEXTURE));
    }
    if (!entityData.get(OWNER_TEXTURE_SIGNATURE).isBlank()) {
      output.putString(TAG_OWNER_TEXTURE_SIGNATURE, entityData.get(OWNER_TEXTURE_SIGNATURE));
    }
    if (returnLink != null) {
      output.store(TAG_RETURN_LINK, CompoundTag.CODEC, returnLink);
    }
  }
}
