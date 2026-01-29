package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * BlockEntityType registrations for Mystcraft (Fabric 1.20.1).
 */
public final class FabricModBlockEntities {

    public static final Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER =
            registerBlockEntity("ink_mixer",
                    FabricBlockEntityTypeBuilder.create(InkMixerBlockEntity::new, FabricModBlocks.INK_MIXER.get()).build());

    public static final Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER =
            registerBlockEntity("book_binder",
                    FabricBlockEntityTypeBuilder.create(BookBinderBlockEntity::new, FabricModBlocks.BOOK_BINDER.get()).build());

    public static final Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE =
            registerBlockEntity("book_receptacle",
                    FabricBlockEntityTypeBuilder.create(BookReceptacleBlockEntity::new, FabricModBlocks.BOOK_RECEPTACLE.get()).build());

    public static final Supplier<BlockEntityType<BookstandBlockEntity>> BOOKSTAND =
            registerBlockEntity("bookstand",
                    FabricBlockEntityTypeBuilder.create(BookstandBlockEntity::new, FabricModBlocks.BOOKSTAND.get()).build());

    public static final Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK =
            registerBlockEntity("writing_desk",
                    FabricBlockEntityTypeBuilder.create(WritingDeskBlockEntity::new, FabricModBlocks.WRITING_DESK.get()).build());

    public static final Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE =
            registerBlockEntity("star_fissure",
                    FabricBlockEntityTypeBuilder.create(StarFissureBlockEntity::new, FabricModBlocks.STAR_FISSURE.get()).build());

    public static final Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER =
            registerBlockEntity("link_modifier",
                    FabricBlockEntityTypeBuilder.create(LinkModifierBlockEntity::new, FabricModBlocks.LINK_MODIFIER.get()).build());

    private FabricModBlockEntities() {
    }

    private static <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
            String name, BlockEntityType<T> type) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(Mystcraft.MOD_ID, name), type);
        return () -> type;
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
