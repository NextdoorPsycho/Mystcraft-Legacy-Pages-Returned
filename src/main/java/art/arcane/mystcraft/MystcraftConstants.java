package art.arcane.mystcraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Constants and identifiers used throughout Mystcraft.
 */
public final class MystcraftConstants {

    private MystcraftConstants() {
        // Utility class
    }

    /**
     * Creates a ResourceLocation with the Mystcraft mod ID namespace.
     */
    public static ResourceLocation loc(String path) {
        return new ResourceLocation(Mystcraft.MOD_ID, path);
    }

    /**
     * Block registry names
     */
    public static final class Blocks {
        public static final String PORTAL = "link_portal";
        public static final String CRYSTAL = "crystal";
        public static final String CRYSTAL_RECEPTACLE = "crystal_receptacle";
        public static final String DECAY = "decay";
        public static final String BOOKSTAND = "bookstand";
        public static final String LECTERN = "lectern";
        public static final String WRITING_DESK = "writing_desk";
        public static final String BOOK_BINDER = "book_binder";
        public static final String INK_MIXER = "ink_mixer";
        public static final String STAR_FISSURE = "star_fissure";
        public static final String LINK_MODIFIER = "link_modifier";

        private Blocks() {}
    }

    /**
     * Item registry names
     */
    public static final class Items {
        public static final String PAGE = "page";
        public static final String DESCRIPTIVE_BOOK = "descriptive_book";
        public static final String LINKBOOK_UNLINKED = "linkbook_unlinked";
        public static final String LINKBOOK = "linkbook";
        public static final String FOLDER = "folder";
        public static final String BOOSTER = "booster";
        public static final String INK_VIAL = "ink_vial";
        public static final String PORTFOLIO = "portfolio";

        private Items() {}
    }

    /**
     * Fluid registry names
     */
    public static final class Fluids {
        public static final String BLACK_INK = "black_ink";

        private Fluids() {}
    }
}
