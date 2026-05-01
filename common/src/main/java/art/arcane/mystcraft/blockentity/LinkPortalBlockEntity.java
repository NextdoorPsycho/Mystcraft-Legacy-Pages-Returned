package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for individual {@code LinkPortalBlock} cells inside a lit
 * portal. Stores three pieces of stamped state at fire time:
 *
 * <ul>
 *   <li>{@code portalColor} — the receptacle's resolved tint at the moment
 *       this cell was placed. Used by both Forge and Fabric block-colour
 *       providers as the single source of truth, so every cell of a portal
 *       renders the same colour even when the cell is far from any
 *       receptacle.</li>
 *   <li>{@code receptaclePos} — the BlockPos of the source receptacle. Lets
 *       {@code entityInside} skip the legacy cross-block BFS hunt and go
 *       straight to the BE that owns the link target.</li>
 *   <li>{@code linkData} — a *snapshot* of the receptacle's book NBT at fire
 *       time. Used by {@code performLink} so teleportation never depends on
 *       cross-chunk receptacle resolution and survives the receptacle being
 *       broken mid-traversal (a common source of "portal goes nowhere"
 *       reports).</li>
 * </ul>
 *
 * <p>All three fields persist in NBT and sync to client through the standard
 * {@link MystcraftBlockEntity#getUpdatePacket()} pipeline. Default values are
 * benign (white, ZERO, empty) so a freshly-loaded portal cell pre-stamping
 * still renders sensibly while the receptacle re-fires.
 */
public class LinkPortalBlockEntity extends MystcraftBlockEntity {

  private static final String TAG_COLOR = "portalColor";
  private static final String TAG_RECEPTACLE = "receptaclePos";
  private static final String TAG_LINK = "linkData";

  /** The receptacle's resolved colour at fire time. {@code -1} means "not stamped yet". */
  private int portalColor = -1;

  /** {@link BlockPos#ZERO} when not stamped (treat as "unknown"). */
  private BlockPos receptaclePos = BlockPos.ZERO;

  /** Book NBT snapshot from the source receptacle. Empty when not stamped. */
  private CompoundTag linkData = new CompoundTag();

  public LinkPortalBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.LINK_PORTAL.get(), pos, blockState);
  }

  // ---------------------------------------------------------------------------
  // Colour
  // ---------------------------------------------------------------------------

  /**
   * @return the stamped colour, or a fallback Mystcraft blue when the cell
   *         hasn't been stamped yet (pre-v2 portals on disk + the brief window
   *         between block placement and fire-time stamping).
   */
  public int getPortalColor() {
    return portalColor != -1 ? portalColor : 0x4488FF;
  }

  /**
   * Stamps the cell's colour. Marks the BE dirty and pushes an update packet
   * so the client tint refreshes immediately.
   */
  public void setPortalColor(int color) {
    if (this.portalColor == color) {
      return;
    }
    this.portalColor = color;
    markForUpdate();
  }

  // ---------------------------------------------------------------------------
  // Receptacle pointer
  // ---------------------------------------------------------------------------

  /**
   * @return the source receptacle position, or {@link BlockPos#ZERO} when not
   *         stamped.
   */
  public BlockPos getReceptaclePos() {
    return receptaclePos;
  }

  public void setReceptaclePos(BlockPos pos) {
    this.receptaclePos = pos != null ? pos.immutable() : BlockPos.ZERO;
    setChanged();
  }

  // ---------------------------------------------------------------------------
  // Link snapshot
  // ---------------------------------------------------------------------------

  /**
   * @return a defensive copy of the stamped link NBT. Empty when not stamped.
   */
  public CompoundTag getLinkDataCopy() {
    return linkData.copy();
  }

  public boolean hasLinkData() {
    return !linkData.isEmpty();
  }

  public void setLinkData(CompoundTag tag) {
    this.linkData = tag != null ? tag.copy() : new CompoundTag();
    setChanged();
  }

  // ---------------------------------------------------------------------------
  // Convenience: stamp every field at once. Called by PortalUtils.firePortal
  // when laying down the portal cells, so callers don't have to remember the
  // three-field set order.
  // ---------------------------------------------------------------------------
  public void stamp(BlockPos receptaclePos, int color, CompoundTag linkData) {
    this.receptaclePos = receptaclePos != null ? receptaclePos.immutable() : BlockPos.ZERO;
    this.portalColor = color;
    this.linkData = linkData != null ? linkData.copy() : new CompoundTag();
    markForUpdate();
  }

  // ---------------------------------------------------------------------------
  // NBT
  // ---------------------------------------------------------------------------

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    tag.putInt(TAG_COLOR, portalColor);
    tag.putLong(TAG_RECEPTACLE, receptaclePos.asLong());
    if (!linkData.isEmpty()) {
      tag.put(TAG_LINK, linkData.copy());
    }
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    portalColor = tag.contains(TAG_COLOR) ? tag.getInt(TAG_COLOR) : -1;
    receptaclePos = tag.contains(TAG_RECEPTACLE) ? BlockPos.of(tag.getLong(TAG_RECEPTACLE)) : BlockPos.ZERO;
    linkData = tag.contains(TAG_LINK) ? tag.getCompound(TAG_LINK).copy() : new CompoundTag();
  }
}
