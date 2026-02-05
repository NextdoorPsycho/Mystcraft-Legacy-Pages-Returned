package art.arcane.mystcraft.block;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Link Portal block.
 * Created by book receptacles when a book is placed.
 * Allows travel to the destination specified in the book.
 */
public class LinkPortalBlock extends Block {

  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final DirectionProperty SOURCE_DIRECTION = BlockStateProperties.FACING;

  private static final VoxelShape SHAPE_SMALL = Block.box(4, 4, 4, 12, 12, 12);

  // Cooldown to prevent spam teleporting (entity UUID -> last teleport time)
  private static final Map<UUID, Long> TELEPORT_COOLDOWNS = new HashMap<>();
  private static final long COOLDOWN_TICKS = 100; // 5 seconds

  public LinkPortalBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any()
        .setValue(ACTIVE, false)
        .setValue(SOURCE_DIRECTION, Direction.DOWN));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(ACTIVE, SOURCE_DIRECTION);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    // Expand shape based on adjacent portal/crystal blocks
    return calculateShape(level, pos);
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  /**
   * Calculates the shape based on adjacent portal blocks.
   */
  private VoxelShape calculateShape(BlockGetter level, BlockPos pos) {
    double minX = 0.25, maxX = 0.75;
    double minY = 0.25, maxY = 0.75;
    double minZ = 0.25, maxZ = 0.75;

    if (isValidPortalNeighbor(level.getBlockState(pos.west()))) minX = 0;
    if (isValidPortalNeighbor(level.getBlockState(pos.east()))) maxX = 1;
    if (isValidPortalNeighbor(level.getBlockState(pos.below()))) minY = 0;
    if (isValidPortalNeighbor(level.getBlockState(pos.above()))) maxY = 1;
    if (isValidPortalNeighbor(level.getBlockState(pos.north()))) minZ = 0;
    if (isValidPortalNeighbor(level.getBlockState(pos.south()))) maxZ = 1;

    return Block.box(minX * 16, minY * 16, minZ * 16, maxX * 16, maxY * 16, maxZ * 16);
  }

  /**
   * Checks if a state is a valid portal neighbor.
   */
  private boolean isValidPortalNeighbor(BlockState state) {
    return state.getBlock() instanceof LinkPortalBlock ||
        state.getBlock() instanceof CrystalBlock;
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (level.isClientSide) {
      return;
    }

    // Check cooldown
    UUID entityId = entity.getUUID();
    long currentTime = level.getGameTime();
    Long lastTeleport = TELEPORT_COOLDOWNS.get(entityId);
    if (lastTeleport != null && currentTime - lastTeleport < COOLDOWN_TICKS) {
      return; // Still in cooldown
    }

    // Find the book receptacle to get the link destination
    BookReceptacleBlockEntity receptacle = findReceptacle(level, pos, state);
    if (receptacle == null || !receptacle.hasBook()) {
      // Don't destroy the portal block here. Portal validation is handled by
      // neighborChanged / PortalUtils.validatePortal. Destroying on contact
      // cascades and tears the entire portal apart.
      return;
    }

    // Get link data from the book
    ItemStack book = receptacle.getBook();

    // If this is a new Agebook (no dimension assigned yet), activate it to create the Age.
    // This matches the original behavior where placing a book in a receptacle and walking
    // through the portal creates the Age on first contact.
    if (book.getItem() instanceof AgebookItem agebookItem
        && LinkOptions.getDimensionUID(ItemStackNbt.getTag(book)) == null
        && entity instanceof net.minecraft.server.level.ServerPlayer player) {
      agebookItem.activate(book, level, entity);
      // After activation, the book tag is updated in place with Dimension/Spawn data.
      // If activation failed (no link panel, etc.), the tag still won't have a dimension.
      if (LinkOptions.getDimensionUID(ItemStackNbt.getTag(book)) == null) {
        return;
      }
      // activate() already performed the link, so we're done.
      TELEPORT_COOLDOWNS.put(entityId, currentTime);
      return;
    }

    CompoundTag linkData = getLinkData(book);
    if (linkData == null) {
      return;
    }

    // Set cooldown before teleporting
    TELEPORT_COOLDOWNS.put(entityId, currentTime);

    // Play portal sound
    level.playSound(null, pos, ModSounds.LINKING_PORTAL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

    // Clone link data and apply portal-specific overrides
    CompoundTag portalLinkData = linkData.copy();
    LinkOptions.setFlag(portalLinkData, LinkFlags.MAINTAIN_MOMENTUM, true);
    LinkOptions.setFlag(portalLinkData, LinkFlags.GENERATE_PLATFORM, false);

    // Perform the link
    LinkingManager.LinkResult result = LinkingManager.performLink(entity, portalLinkData);

    if (result != LinkingManager.LinkResult.SUCCESS) {
      Mystcraft.LOGGER.warn("[Portal] Link failed at {}: {}", pos, result);
      if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("mystcraft.portal.link_failed", result.name()),
            true);
      }
    }

    // Clean up old cooldowns occasionally
    if (currentTime % 1200 == 0) {
      TELEPORT_COOLDOWNS.entrySet().removeIf(entry -> currentTime - entry.getValue() > COOLDOWN_TICKS * 2);
    }
  }

  /**
   * Extracts link data from a book item.
   */
  private CompoundTag getLinkData(ItemStack book) {
    if (book.isEmpty() || ItemStackNbt.getTag(book) == null) {
      return null;
    }

    // Both Linkbooks and Agebooks store their link data in the item tag
    if (book.getItem() instanceof LinkbookItem || book.getItem() instanceof AgebookItem) {
      return ItemStackNbt.getTag(book);
    }

    return null;
  }

  /**
   * Finds the book receptacle controlling this portal.
   * Uses PortalUtils.findReceptacle() which correctly follows the direction chain.
   */
  private BookReceptacleBlockEntity findReceptacle(Level level, BlockPos pos, BlockState state) {
    BlockEntity be = PortalUtils.findReceptacle(level, pos);
    if (be instanceof BookReceptacleBlockEntity receptacle) {
      return receptacle;
    }
    return null;
  }

  @Override
  public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
    if (level.isClientSide) return;
    // Validate when a neighbor is explicitly changed (e.g. frame block broken).
    // This does NOT fire during portal creation (which uses UPDATE_NONE),
    // avoiding a cascading validation loop.
    PortalUtils.validatePortal(level, pos);
  }

}
