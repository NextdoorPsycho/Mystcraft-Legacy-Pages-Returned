package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.fabric.FabricTriggers;
import art.arcane.mystcraft.platform.services.IAdvancementTriggerFactory;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 1.20.1 implementation of advancement trigger factory. Uses the legacy
 * API with ContextAwarePredicate and getId().
 */
public class FabricAdvancementTriggerFactory_1_20_1 implements IAdvancementTriggerFactory {

  private FabricTriggers.WritingDeskWriteTrigger writingDeskWriteTrigger;
  private FabricTriggers.EnterMystDimensionSafeTrigger enterMystDimensionSafeTrigger;
  private FabricTriggers.EnterMystDimensionQuinnTrigger enterMystDimensionQuinnTrigger;

  @Override
  public void registerTriggers() {
    writingDeskWriteTrigger = FabricTriggers.createWritingDeskWriteTrigger();
    enterMystDimensionSafeTrigger = FabricTriggers.createEnterMystDimensionSafeTrigger();
    enterMystDimensionQuinnTrigger = FabricTriggers.createEnterMystDimensionQuinnTrigger();

    CriteriaTriggers.register(writingDeskWriteTrigger);
    CriteriaTriggers.register(enterMystDimensionSafeTrigger);
    CriteriaTriggers.register(enterMystDimensionQuinnTrigger);
  }

  @Override
  public void triggerEnterMystDimensionSafe(ServerPlayer player) {
    if (enterMystDimensionSafeTrigger != null) {
      enterMystDimensionSafeTrigger.trigger(player);
    }
  }

  @Override
  public void triggerEnterMystDimensionQuinn(ServerPlayer player) {
    if (enterMystDimensionQuinnTrigger != null) {
      enterMystDimensionQuinnTrigger.trigger(player);
    }
  }

  @Override
  public void triggerWritingDeskWrite(ServerPlayer player) {
    if (writingDeskWriteTrigger != null) {
      writingDeskWriteTrigger.trigger(player);
    }
  }
}
