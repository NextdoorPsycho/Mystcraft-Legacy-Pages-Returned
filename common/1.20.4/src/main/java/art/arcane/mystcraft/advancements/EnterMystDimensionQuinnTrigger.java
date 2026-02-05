package art.arcane.mystcraft.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Triggers when a player enters a Mystcraft dimension without a return linkbook.
 * 1.20.4+ codec-based criterion implementation.
 */
public class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {

  @Override
  public Codec<TriggerInstance> codec() {
    return TriggerInstance.CODEC;
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, triggerInstance -> true);
  }

  public record TriggerInstance(Optional<ContextAwarePredicate> player)
      implements SimpleCriterionTrigger.SimpleInstance {

    public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EntityPredicate.ADVANCEMENT_CODEC
            .optionalFieldOf("player")
            .forGetter(TriggerInstance::player)
    ).apply(instance, TriggerInstance::new));

    public static TriggerInstance create() {
      return new TriggerInstance(Optional.empty());
    }
  }
}
