package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LecternBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * BlockEntityType registrations for Mystcraft (Fabric).
 */
public final class FabricModBlockEntities {

    public static final Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER =
            registerBlockEntity("ink_mixer",
                    BlockEntityType.Builder.of(InkMixerBlockEntity::new, FabricModBlocks.INK_MIXER.get()).build(null));

    public static final Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER =
            registerBlockEntity("book_binder",
                    BlockEntityType.Builder.of(BookBinderBlockEntity::new, FabricModBlocks.BOOK_BINDER.get()).build(null));

    public static final Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE =
            registerBlockEntity("book_receptacle",
                    BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, FabricModBlocks.BOOK_RECEPTACLE.get()).build(null));

    public static final Supplier<BlockEntityType<BookstandBlockEntity>> BOOKSTAND =
            registerBlockEntity("bookstand",
                    BlockEntityType.Builder.of(BookstandBlockEntity::new, FabricModBlocks.BOOKSTAND.get()).build(null));

    public static final Supplier<BlockEntityType<LecternBlockEntity>> LECTERN =
            registerBlockEntity("lectern",
                    BlockEntityType.Builder.of(LecternBlockEntity::new, FabricModBlocks.LECTERN.get()).build(null));

    public static final Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK =
            registerBlockEntity("writing_desk",
                    BlockEntityType.Builder.of(WritingDeskBlockEntity::new, FabricModBlocks.WRITING_DESK.get()).build(null));

    public static final Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE =
            registerBlockEntity("star_fissure",
                    BlockEntityType.Builder.of(StarFissureBlockEntity::new, FabricModBlocks.STAR_FISSURE.get()).build(null));

    public static final Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER =
            registerBlockEntity("link_modifier",
                    BlockEntityType.Builder.of(LinkModifierBlockEntity::new, FabricModBlocks.LINK_MODIFIER.get()).build(null));

    private FabricModBlockEntities() {
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
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
