package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the vanilla death broadcast only for players dying in an Age.
 * In 26.2 gamerules are server-global, so mutating SHOW_DEATH_MESSAGES while
 * creating a dynamic level would also silence deaths in every vanilla world.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMessageMixin {

  @Redirect(
      method = "die",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
      )
  )
  private Object mystcraft$useScopedDeathMessages(GameRules rules, GameRule<?> rule) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    if (rule == GameRules.SHOW_DEATH_MESSAGES
        && MystcraftConfig.deathEffectsEnabled.get()
        && AgeDimensionFactory.isMystcraftAge(player.level().dimension())) {
      return Boolean.FALSE;
    }
    return rules.get(rule);
  }
}
