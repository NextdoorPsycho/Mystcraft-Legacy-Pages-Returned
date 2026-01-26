package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.LecternBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Mystcraft Lectern block.
 * Similar to bookstand but with a taller design for standing use.
 * Named MystcraftLecternBlock to avoid conflict with vanilla LecternBlock.
 */
public class MystcraftLecternBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public MystcraftLecternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use MODEL to render the OBJ model via JSON, BER renders book on top
        return RenderShape.MODEL;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LecternBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    @NotNull
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (lectern.hasBook()) {
            // Book is on lectern
            if (player.isShiftKeyDown() && held.isEmpty()) {
                // Shift + empty hand = pick up book
                player.setItemInHand(hand, lectern.getBook());
                lectern.setBook(ItemStack.EMPTY);
                return InteractionResult.CONSUME;
            } else {
                // Open book GUI (client-side) or perform linking (server-side)
                if (level.isClientSide) {
                    openBookScreen(lectern.getBook());
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            // No book on lectern
            if (!held.isEmpty() && BookstandBlockEntity.isValidBook(held)) {
                // Place book on lectern
                ItemStack bookCopy = held.copy();
                bookCopy.setCount(1);
                held.shrink(1);
                lectern.setBook(bookCopy);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LecternBlockEntity lectern) {
                lectern.dropContents();
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity lectern) {
            return lectern.getAnalogOutputSignal();
        }
        return 0;
    }

    /**
     * Opens the book screen on the client side.
     */
    private void openBookScreen(ItemStack book) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            art.arcane.mystcraft.client.screen.BookScreen.open(book));
    }
}
