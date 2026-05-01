package art.arcane.mystcraft.fabric;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 1.20.1 advancement triggers using the legacy API with
 * ContextAwarePredicate and getId().
 */
public final class FabricTriggers {

  private FabricTriggers() {
  }

  public static EnterMystDimensionQuinnTrigger createEnterMystDimensionQuinnTrigger() {
    return new EnterMystDimensionQuinnTrigger();
  }

  public static EnterMystDimensionSafeTrigger createEnterMystDimensionSafeTrigger() {
    return new EnterMystDimensionSafeTrigger();
  }

  public static WritingDeskWriteTrigger createWritingDeskWriteTrigger() {
    return new WritingDeskWriteTrigger();
  }

  public static class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {
    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "enter_myst_dimension_quinn");

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    @Override
    public ResourceLocation getId() {
      return ID;
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

  public static class EnterMystDimensionSafeTrigger extends SimpleCriterionTrigger<EnterMystDimensionSafeTrigger.TriggerInstance> {
    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "enter_myst_dimension_safe");

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    @Override
    public ResourceLocation getId() {
      return ID;
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

  public static class WritingDeskWriteTrigger extends SimpleCriterionTrigger<WritingDeskWriteTrigger.TriggerInstance> {
    private static final ResourceLocation ID = new ResourceLocation("mystcraft", "writing_desk_write");

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
      return new TriggerInstance(player);
    }

    @Override
    public ResourceLocation getId() {
      return ID;
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
