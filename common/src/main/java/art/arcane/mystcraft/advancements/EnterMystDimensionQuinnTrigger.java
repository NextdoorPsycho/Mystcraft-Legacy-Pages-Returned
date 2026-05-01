package art.arcane.mystcraft.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Triggers when a player enters a Mystcraft dimension without carrying a
 * linkbook.
 */
public class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {

  @Override
  protected TriggerInstance createInstance(JsonObject json, Optional<ContextAwarePredicate> player, DeserializationContext context) {
    return new TriggerInstance(player);
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, triggerInstance -> true);
  }

  public static class TriggerInstance extends AbstractCriterionTriggerInstance {

    public TriggerInstance(Optional<ContextAwarePredicate> player) {
      super(player);
    }

    public static TriggerInstance create() {
      return new TriggerInstance(Optional.empty());
    }
  }
}
