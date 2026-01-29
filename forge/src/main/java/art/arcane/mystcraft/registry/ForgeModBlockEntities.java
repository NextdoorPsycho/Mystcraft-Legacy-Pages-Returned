package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * BlockEntityType registrations for Mystcraft.
 */
public final class ForgeModBlockEntities {

    public static final RegistryObject<BlockEntityType<InkMixerBlockEntity>> INK_MIXER =
            MystcraftRegistries.BLOCK_ENTITIES.register("ink_mixer",
                    () -> BlockEntityType.Builder.of(InkMixerBlockEntity::new, ForgeModBlocks.INK_MIXER.get()).build(null));

    public static final RegistryObject<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER =
            MystcraftRegistries.BLOCK_ENTITIES.register("book_binder",
                    () -> BlockEntityType.Builder.of(BookBinderBlockEntity::new, ForgeModBlocks.BOOK_BINDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE =
            MystcraftRegistries.BLOCK_ENTITIES.register("book_receptacle",
                    () -> BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, ForgeModBlocks.BOOK_RECEPTACLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BookstandBlockEntity>> BOOKSTAND =
            MystcraftRegistries.BLOCK_ENTITIES.register("bookstand",
                    () -> BlockEntityType.Builder.of(BookstandBlockEntity::new, ForgeModBlocks.BOOKSTAND.get()).build(null));

    public static final RegistryObject<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK =
            MystcraftRegistries.BLOCK_ENTITIES.register("writing_desk",
                    () -> BlockEntityType.Builder.of(WritingDeskBlockEntity::new, ForgeModBlocks.WRITING_DESK.get()).build(null));

    public static final RegistryObject<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE =
            MystcraftRegistries.BLOCK_ENTITIES.register("star_fissure",
                    () -> BlockEntityType.Builder.of(StarFissureBlockEntity::new, ForgeModBlocks.STAR_FISSURE.get()).build(null));

    public static final RegistryObject<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER =
            MystcraftRegistries.BLOCK_ENTITIES.register("link_modifier",
                    () -> BlockEntityType.Builder.of(LinkModifierBlockEntity::new, ForgeModBlocks.LINK_MODIFIER.get()).build(null));

    private ForgeModBlockEntities() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
