package art.arcane.mystcraft.advancements.trigger;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 1.18.2 version of WritingDeskWriteTrigger.
 * Uses the older API with EntityPredicate.Composite (no Optional) and getId().
 */
public class WritingDeskWriteTrigger_1_18_2 extends SimpleCriterionTrigger<WritingDeskWriteTrigger_1_18_2.TriggerInstance> {

  private static final ResourceLocation ID = new ResourceLocation("mystcraft", "writing_desk_write");

  @Override
  public ResourceLocation getId() {
    return ID;
  }

  @Override
  protected TriggerInstance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext context) {
    return new TriggerInstance(player);
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, triggerInstance -> true);
  }

  public static class TriggerInstance extends AbstractCriterionTriggerInstance {

    public TriggerInstance(EntityPredicate.Composite player) {
      super(ID, player);
    }

    public static TriggerInstance create() {
      return new TriggerInstance(EntityPredicate.Composite.ANY);
    }
  }
}
