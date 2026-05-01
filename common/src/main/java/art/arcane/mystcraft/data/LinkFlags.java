package art.arcane.mystcraft.data;

/**
 * Constants for link property flags. These flags modify how linking behaves.
 */
public final class LinkFlags {

  /**
   * Allows linking within the same dimension (intra-age linking).
   */
  public static final String INTRA_LINKING = "intralinking";
  /**
   * Only allows linking within the same dimension.
   */
  public static final String INTRA_LINKING_ONLY = "intralinkingonly";
  /**
   * Generates a stone platform at the destination if needed.
   */
  public static final String GENERATE_PLATFORM = "generateplatform";
  /**
   * Maintains the player's momentum through the link.
   */
  public static final String MAINTAIN_MOMENTUM = "maintainmomentum";
  /**
   * Removes held items when linking (disarms the player).
   */
  public static final String DISARM = "disarm";
  /**
   * Uses relative positioning - offset from book position applied to
   * destination.
   */
  public static final String RELATIVE = "relative";
  /**
   * The link follows the entity that activated it.
   */
  public static final String FOLLOWING = "following";
  /**
   * All known link flags for iteration.
   */
  public static final String[] ALL_FLAGS = {
      INTRA_LINKING,
      INTRA_LINKING_ONLY,
      GENERATE_PLATFORM,
      MAINTAIN_MOMENTUM,
      DISARM,
      RELATIVE,
      FOLLOWING
  };

  private LinkFlags() {
  }
}
