package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.forge.ForgeTriggers;
import art.arcane.mystcraft.platform.services.IAdvancementTriggerFactory;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge 1.20.1 implementation of advancement trigger factory. Uses the older
 * API with ContextAwarePredicate (no Optional) and getId().
 */
public class ForgeAdvancementTriggerFactory_1_20_1 implements IAdvancementTriggerFactory {

  private ForgeTriggers.WritingDeskWriteTrigger writingDeskWriteTrigger;
  private ForgeTriggers.EnterMystDimensionSafeTrigger enterMystDimensionSafeTrigger;
  private ForgeTriggers.EnterMystDimensionQuinnTrigger enterMystDimensionQuinnTrigger;

  @Override
  public void registerTriggers() {
    writingDeskWriteTrigger = new ForgeTriggers.WritingDeskWriteTrigger();
    enterMystDimensionSafeTrigger = new ForgeTriggers.EnterMystDimensionSafeTrigger();
    enterMystDimensionQuinnTrigger = new ForgeTriggers.EnterMystDimensionQuinnTrigger();

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
