package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.util.BlockInteractionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(BlockBehaviour.class)
public class BlockUseItemOnCompatMixin {

  @Inject(
      method = "useItemOn",
      at = @At("HEAD"),
      cancellable = true
  )
  private void mystcraft$useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                   Player player, InteractionHand hand, BlockHitResult hit,
                                   CallbackInfoReturnable<ItemInteractionResult> cir) {
    Object self = this;
    if (!(self instanceof BlockInteractionCompat)) {
      return;
    }

    InteractionResult result = invokeUse(self, state, level, pos, player, hand, hit);
    if (result == null) {
      return;
    }
    cir.setReturnValue(toItemResult(result, level.isClientSide));
  }

  private static InteractionResult invokeUse(Object self, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
    try {
      Method method = self.getClass().getMethod("use", BlockState.class, Level.class, BlockPos.class,
          Player.class, InteractionHand.class, BlockHitResult.class);
      Object result = method.invoke(self, state, level, pos, player, hand, hit);
      return result instanceof InteractionResult interaction ? interaction : null;
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static ItemInteractionResult toItemResult(InteractionResult result, boolean isClient) {
    if (result == InteractionResult.FAIL) {
      return ItemInteractionResult.FAIL;
    }
    if (result == InteractionResult.CONSUME_PARTIAL) {
      return ItemInteractionResult.CONSUME_PARTIAL;
    }
    if (result == InteractionResult.CONSUME) {
      return ItemInteractionResult.CONSUME;
    }
    if (result == InteractionResult.PASS) {
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    return ItemInteractionResult.sidedSuccess(isClient);
  }
}
