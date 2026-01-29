package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * BlockEntityType registrations for Mystcraft.
 */
public final class NeoForgeModBlockEntities {

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InkMixerBlockEntity>> INK_MIXER =
            MystcraftRegistries.BLOCK_ENTITIES.register("ink_mixer",
                    () -> BlockEntityType.Builder.of(InkMixerBlockEntity::new, NeoForgeModBlocks.INK_MIXER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER =
            MystcraftRegistries.BLOCK_ENTITIES.register("book_binder",
                    () -> BlockEntityType.Builder.of(BookBinderBlockEntity::new, NeoForgeModBlocks.BOOK_BINDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE =
            MystcraftRegistries.BLOCK_ENTITIES.register("book_receptacle",
                    () -> BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, NeoForgeModBlocks.BOOK_RECEPTACLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookstandBlockEntity>> BOOKSTAND =
            MystcraftRegistries.BLOCK_ENTITIES.register("bookstand",
                    () -> BlockEntityType.Builder.of(BookstandBlockEntity::new, NeoForgeModBlocks.BOOKSTAND.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK =
            MystcraftRegistries.BLOCK_ENTITIES.register("writing_desk",
                    () -> BlockEntityType.Builder.of(WritingDeskBlockEntity::new, NeoForgeModBlocks.WRITING_DESK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE =
            MystcraftRegistries.BLOCK_ENTITIES.register("star_fissure",
                    () -> BlockEntityType.Builder.of(StarFissureBlockEntity::new, NeoForgeModBlocks.STAR_FISSURE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER =
            MystcraftRegistries.BLOCK_ENTITIES.register("link_modifier",
                    () -> BlockEntityType.Builder.of(LinkModifierBlockEntity::new, NeoForgeModBlocks.LINK_MODIFIER.get()).build(null));

    private NeoForgeModBlockEntities() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
