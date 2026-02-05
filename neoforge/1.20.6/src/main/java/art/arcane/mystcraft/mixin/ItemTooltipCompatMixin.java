package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Item.class)
public class ItemTooltipCompatMixin {

  private static final Map<Class<?>, Method> OLD_TOOLTIP_METHOD = new ConcurrentHashMap<>();

  @Inject(
      method = "appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
      at = @At("HEAD"),
      cancellable = true
  )
  private void mystcraft$appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
    Object self = this;
    if (!(self instanceof TooltipCompat)) {
      return;
    }

    Method method = OLD_TOOLTIP_METHOD.computeIfAbsent(self.getClass(), ItemTooltipCompatMixin::findOldTooltipMethod);
    if (method == null) {
      return;
    }

    Level level = null;
    if (context != null) {
      try {
        Method levelMethod = context.getClass().getMethod("level");
        Object value = levelMethod.invoke(context);
        if (value instanceof Level cast) {
          level = cast;
        }
      } catch (ReflectiveOperationException ignored) {
      }
    }

    try {
      method.invoke(self, stack, level, tooltip, flag);
      ci.cancel();
    } catch (ReflectiveOperationException ignored) {
    }
  }

  private static Method findOldTooltipMethod(Class<?> clazz) {
    try {
      Method method = clazz.getMethod("appendHoverText", ItemStack.class, Level.class, List.class, TooltipFlag.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
