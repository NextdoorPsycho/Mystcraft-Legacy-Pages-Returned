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
    public static final String INK_MIXER = "blockinkmixer";
    public static final String BOOK_BINDER = "blockbookbinder";
    public static final String BOOK_RECEPTACLE = "blockbookreceptacle";
    public static final String DECAY = "blockdecay";
    public static final String LINK_MODIFIER = "blocklinkmodifier";
    public static final String CRYSTAL = "blockcrystal";
    public static final String PORTAL = "linkportal";
    public static final String WRITING_DESK = "writingdesk";
    public static final String STAR_FISSURE = "blockstarfissure";
    public static final String FLUID_INK = "fluidblockblackink";

    private Blocks() {
    }
  }

  /**
   * Item registry names
   */
  public static final class Items {
    public static final String PAGE = "page";
    public static final String AGEBOOK = "agebook";
    public static final String LINKBOOK = "linkbook";
    public static final String LINKBOOK_UNLINKED = "linkbook_unlinked";
    public static final String PERSONAL_LINK_BOOK = "personal_link_book";
    public static final String BOOSTER = "booster";
    public static final String FOLDER = "folder";
    public static final String PORTFOLIO = "portfolio";
    public static final String INK_VIAL = "inkvial";
    public static final String INK_BUCKET = "ink_bucket";

    private Items() {
    }
  }

  /**
   * Block Entity registry names
   */
  public static final class BlockEntities {
    public static final String INK_MIXER = "ink_mixer";
    public static final String BOOK_BINDER = "book_binder";
    public static final String BOOK_RECEPTACLE = "book_receptacle";
    public static final String WRITING_DESK = "writing_desk";
    public static final String STAR_FISSURE = "star_fissure";
    public static final String LINK_MODIFIER = "link_modifier";

    private BlockEntities() {
    }
  }

  /**
   * Entity registry names
   */
  public static final class Entities {
    public static final String LINKBOOK = "linkbook";
    public static final String FALLING_BLOCK = "falling_block";
    public static final String METEOR = "meteor";

    private Entities() {
    }
  }

  /**
   * Fluid registry names
   */
  public static final class Fluids {
    public static final String BLACK_INK = "black_ink";
    public static final String BLACK_INK_FLOWING = "black_ink_flowing";

    private Fluids() {
    }
  }

  /**
   * Sound event registry names
   */
  public static final class Sounds {
    public static final String LINKING_POP = "linking.pop";
    public static final String LINKING_LINK = "linking.link";
    public static final String LINKING_DISARM = "linking.link-disarm";
    public static final String LINKING_FOLLOWING = "linking.link-following";
    public static final String LINKING_INTRA = "linking.link-intra";
    public static final String LINKING_FISSURE = "linking.link-fissure";
    public static final String LINKING_PORTAL = "linking.link-portal";
    public static final String METEOR_ROAR = "entity.meteor.roar";

    private Sounds() {
    }
  }
}
