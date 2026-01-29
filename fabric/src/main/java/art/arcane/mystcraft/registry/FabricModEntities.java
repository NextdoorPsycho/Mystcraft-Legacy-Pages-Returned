package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/**
 * Entity registrations for Mystcraft (Fabric).
 */
public final class FabricModEntities {

    public static final Supplier<EntityType<LinkbookEntity>> LINKBOOK =
            registerEntity("linkbook",
                    EntityType.Builder.<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "linkbook").toString()));

    public static final Supplier<EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK =
            registerEntity("falling_block",
                    EntityType.Builder.<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "falling_block").toString()));

    public static final Supplier<EntityType<MeteorEntity>> METEOR =
            registerEntity("meteor",
                    EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(16)
                            .updateInterval(10)
                            .fireImmune()
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "meteor").toString()));

    public static final Supplier<EntityType<ColoredLightningEntity>> COLORED_LIGHTNING =
            registerEntity("colored_lightning",
                    EntityType.Builder.<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(16)
                            .updateInterval(Integer.MAX_VALUE)
                            .noSave()
                            .build(new ResourceLocation(Mystcraft.MOD_ID, "colored_lightning").toString()));

    private FabricModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> registerEntity(
            String name, EntityType<T> type) {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(Mystcraft.MOD_ID, name), type);
        return () -> type;
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
