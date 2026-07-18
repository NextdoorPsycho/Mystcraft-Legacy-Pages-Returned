package art.arcane.mystcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Mystcraft block entities. Provides common NBT handling and
 * sync utilities.
 */
public abstract class MystcraftBlockEntity extends BlockEntity {

  public MystcraftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
    super(type, pos, blockState);
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    writeNbt(tag);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    readNbt(tag);
  }

  /**
   * Override to save custom data to NBT.
   */
  protected void writeNbt(CompoundTag tag) {

  }

  /**
   * Override to load custom data from NBT.
   */
  protected void readNbt(CompoundTag tag) {

  }

  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag tag = super.getUpdateTag();
    writeNbt(tag);
    return tag;
  }

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  /**
   * Marks this block entity as dirty and sends an update to clients.
   */
  protected void sync() {
    setChanged();
    if (level != null && !level.isClientSide) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
  }

  /**
   * Alias for sync().
   */
  protected void markForUpdate() {
    sync();
  }
}
