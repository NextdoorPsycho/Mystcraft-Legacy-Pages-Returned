package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Entity registrations for Mystcraft.
 */
public final class NeoForgeModEntities {

    public static final DeferredHolder<EntityType<?>, EntityType<LinkbookEntity>> LINKBOOK =
            MystcraftRegistries.ENTITIES.register("linkbook",
                    () -> EntityType.Builder.<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "linkbook").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK =
            MystcraftRegistries.ENTITIES.register("falling_block",
                    () -> EntityType.Builder.<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "falling_block").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
            MystcraftRegistries.ENTITIES.register("meteor",
                    () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(16)
                            .updateInterval(10)
                            .fireImmune()
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "meteor").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<ColoredLightningEntity>> COLORED_LIGHTNING =
            MystcraftRegistries.ENTITIES.register("colored_lightning",
                    () -> EntityType.Builder.<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(16)
                            .updateInterval(Integer.MAX_VALUE)
                            .noSave()
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "colored_lightning").toString()));

    private NeoForgeModEntities() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
