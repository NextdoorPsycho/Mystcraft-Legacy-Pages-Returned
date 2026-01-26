package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Writing Desk block.
 * Used for writing symbols onto pages using ink.
 * This is a multi-block structure (2 blocks wide).
 */
public class WritingDeskBlock extends BaseEntityBlock {

    // TODO: Add multiblock state properties (left/right)

    public WritingDeskBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WritingDeskBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    // TODO: Add multiblock placement logic
    // TODO: Add GUI opening logic
    // TODO: Add symbol writing interaction
}
