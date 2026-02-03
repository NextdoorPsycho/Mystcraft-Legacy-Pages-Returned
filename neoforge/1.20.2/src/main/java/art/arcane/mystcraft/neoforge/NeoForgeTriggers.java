package art.arcane.mystcraft.neoforge;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Consolidated NeoForge 1.20.2 advancement triggers.
 * Uses the 1.20.2 API with Optional<ContextAwarePredicate>.
 */
public final class NeoForgeTriggers {

  private NeoForgeTriggers() {
  }

  // ========== ENTER MYST DIMENSION (QUINN) ==========
  public static class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {

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

  // ========== ENTER MYST DIMENSION (SAFE) ==========
  public static class EnterMystDimensionSafeTrigger extends SimpleCriterionTrigger<EnterMystDimensionSafeTrigger.TriggerInstance> {

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

  // ========== WRITING DESK WRITE ==========
  public static class WritingDeskWriteTrigger extends SimpleCriterionTrigger<WritingDeskWriteTrigger.TriggerInstance> {

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
}
