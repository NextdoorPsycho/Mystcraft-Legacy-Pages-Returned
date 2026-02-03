package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The link portal block that fills the space inside crystal frames.
 * Points toward the receptacle that controls it.
 * <p>
 * 1.18.2 version - uses Material instead of BlockBehaviour.Properties.
 */
public class LinkPortalBlock extends Block {

  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final EnumProperty<Direction> SOURCE_DIRECTION = EnumProperty.create("source", Direction.class);

  public LinkPortalBlock() {
    this(Properties.of(Material.PORTAL)
        .noCollission()
        .strength(-1.0F)
        .lightLevel(state -> state.getValue(ACTIVE) ? 11 : 0));
  }

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
    return Shapes.block();
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
  }

  @Override
  public PushReaction getPistonPushReaction(BlockState state) {
    return PushReaction.BLOCK;
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (level.isClientSide || !state.getValue(ACTIVE)) {
      return;
    }

    // Only trigger for players and living entities, not items or projectiles
    if (!(entity instanceof Player) && !entity.isAlive()) {
      return;
    }

    // Find the receptacle controlling this portal
    BlockEntity be = PortalUtils.findReceptacle(level, pos);
    if (be instanceof BookReceptacleBlockEntity receptacle) {
      ItemStack book = receptacle.getBook();
      if (!book.isEmpty() && book.getTag() != null) {
        // 1.18.2: Use performLink with book's NBT tag
        LinkingManager.performLink(entity, book.getTag());
      }
    }
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    // Schedule validation on next tick
    if (!level.isClientSide()) {
      level.scheduleTick(pos, this, 1);
    }
    return state;
  }

  @Override
  public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                   java.util.Random random) {
    // Validate this portal block
    PortalUtils.validatePortal(level, pos);
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      // Portal block was removed, validate neighbors
      if (!level.isClientSide) {
        PortalUtils.validatePortal(level, pos);
      }
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    return new ItemStack(ModBlocks.CRYSTAL.get());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
    return true;
  }

  /**
   * Gets the portal color from the receptacle.
   */
  public int getPortalColor(Level level, BlockPos pos) {
    BlockEntity be = PortalUtils.findReceptacle(level, pos);
    if (be instanceof BookReceptacleBlockEntity receptacle) {
      return receptacle.getPortalColor();
    }
    return 0x4444FF; // Default Mystcraft blue
  }
}
