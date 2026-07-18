package art.arcane.mystcraft.advancements;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Triggers when a player enters a Mystcraft dimension while carrying a
 * linkbook.
 */
public class EnterMystDimensionSafeTrigger extends SimpleCriterionTrigger<EnterMystDimensionSafeTrigger.TriggerInstance> {

  @Override
  public Codec<TriggerInstance> codec() {
    return TriggerInstance.CODEC;
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, triggerInstance -> true);
  }

  public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<TriggerInstance> CODEC = EntityPredicate.ADVANCEMENT_CODEC
        .optionalFieldOf("player")
        .xmap(TriggerInstance::new, TriggerInstance::player)
        .codec();

    public static TriggerInstance create() {
      return new TriggerInstance(Optional.empty());
    }
  }
}
