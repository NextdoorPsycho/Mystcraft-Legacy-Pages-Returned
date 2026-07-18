package art.arcane.mystcraft.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Mystcraft block entities. Provides common NBT handling and
 * sync utilities.
 */
public abstract class MystcraftBlockEntity extends BlockEntity {

  private static final MapCodec<CompoundTag> FLAT_NBT_CODEC =
      MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

  public MystcraftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
    super(type, pos, blockState);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag tag = new CompoundTag();
    writeNbt(tag);
    output.store(FLAT_NBT_CODEC, tag);
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    readNbt(input.read(FLAT_NBT_CODEC).orElseGet(CompoundTag::new));
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
  public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
    CompoundTag tag = super.getUpdateTag(provider);
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
    if (level != null && !level.isClientSide()) {
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
