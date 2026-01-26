package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
            // No valid receptacle - remove this portal block
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        // Get link data from the book
        ItemStack book = receptacle.getBook();
        CompoundTag linkData = getLinkData(book);
        if (linkData == null) {
            return;
        }

        // Play portal sound
        level.playSound(null, pos, ModSounds.LINKING_PORTAL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

        // Set cooldown before teleporting
        TELEPORT_COOLDOWNS.put(entityId, currentTime);

        // Perform the link
        LinkingManager.LinkResult result = LinkingManager.performLink(entity, linkData);

        // Clean up old cooldowns occasionally
        if (currentTime % 1200 == 0) { // Every minute
            TELEPORT_COOLDOWNS.entrySet().removeIf(entry -> currentTime - entry.getValue() > COOLDOWN_TICKS * 2);
        }
    }

    /**
     * Extracts link data from a book item.
     */
    private CompoundTag getLinkData(ItemStack book) {
        if (book.isEmpty() || book.getTag() == null) {
            return null;
        }

        // Both Linkbooks and Agebooks store their link data in the item tag
        if (book.getItem() instanceof LinkbookItem || book.getItem() instanceof AgebookItem) {
            return book.getTag();
        }

        return null;
    }

    /**
     * Finds the book receptacle controlling this portal.
     */
    private BookReceptacleBlockEntity findReceptacle(Level level, BlockPos pos, BlockState state) {
        Direction sourceDir = state.getValue(SOURCE_DIRECTION);

        // Search along the source direction for the receptacle
        BlockPos checkPos = pos.relative(sourceDir);
        for (int i = 0; i < 16; i++) {
            BlockState checkState = level.getBlockState(checkPos);

            if (checkState.getBlock() instanceof BookReceptacleBlock) {
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof BookReceptacleBlockEntity receptacle) {
                    return receptacle;
                }
            } else if (!(checkState.getBlock() instanceof CrystalBlock ||
                         checkState.getBlock() instanceof LinkPortalBlock)) {
                break;
            }

            checkPos = checkPos.relative(sourceDir);
        }

        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Validate portal when neighbors change
        if (!level.isClientSide()) {
            // TODO: Call PortalUtils.validatePortal when implemented
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Gets the portal color from the associated book receptacle.
     */
    public int getPortalColor(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LinkPortalBlock)) {
            return 0xFFFFFF;
        }

        // This would need to find the receptacle and get its color
        // For now return default white
        return 0xFFFFFF;
    }
}
