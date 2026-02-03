package art.arcane.mystcraft.forge;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Consolidated Forge 1.20.1 advancement triggers.
 * Uses the older API with ContextAwarePredicate (no Optional) and getId().
 */
public final class ForgeTriggers {

  private ForgeTriggers() {
  }

  // ========== ENTER MYST DIMENSION (QUINN) ==========
  public static class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "enter_myst_dimension_quinn");

    @Override
    public ResourceLocation getId() {
      return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    public void trigger(ServerPlayer player) {
      this.trigger(player, triggerInstance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

      public TriggerInstance(ContextAwarePredicate player) {
        super(ID, player);
      }

      public static TriggerInstance create() {
        return new TriggerInstance(ContextAwarePredicate.ANY);
      }
    }
  }

  // ========== ENTER MYST DIMENSION (SAFE) ==========
  public static class EnterMystDimensionSafeTrigger extends SimpleCriterionTrigger<EnterMystDimensionSafeTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "enter_myst_dimension_safe");

    @Override
    public ResourceLocation getId() {
      return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    public void trigger(ServerPlayer player) {
      this.trigger(player, triggerInstance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

      public TriggerInstance(ContextAwarePredicate player) {
        super(ID, player);
      }

      public static TriggerInstance create() {
        return new TriggerInstance(ContextAwarePredicate.ANY);
      }
    }
  }

  // ========== WRITING DESK WRITE ==========
  public static class WritingDeskWriteTrigger extends SimpleCriterionTrigger<WritingDeskWriteTrigger.TriggerInstance> {

    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "writing_desk_write");

    @Override
    public ResourceLocation getId() {
      return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    public void trigger(ServerPlayer player) {
      this.trigger(player, triggerInstance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

      public TriggerInstance(ContextAwarePredicate player) {
        super(ID, player);
      }

      public static TriggerInstance create() {
        return new TriggerInstance(ContextAwarePredicate.ANY);
      }
    }
  }
}
