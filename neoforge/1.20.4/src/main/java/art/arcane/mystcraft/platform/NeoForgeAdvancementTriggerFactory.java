package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.neoforge.NeoForgeAdvancementTriggers;
import art.arcane.mystcraft.neoforge.NeoForgeTriggers;
import art.arcane.mystcraft.platform.services.IAdvancementTriggerFactory;
import net.minecraft.advancements.CriterionTrigger;

/**
 * NeoForge 1.20.4 advancement trigger factory backed by DeferredRegister.
 */
public class NeoForgeAdvancementTriggerFactory implements IAdvancementTriggerFactory {

  private NeoForgeTriggers.WritingDeskWriteTrigger writingDeskWriteTrigger;
  private NeoForgeTriggers.EnterMystDimensionSafeTrigger enterMystDimensionSafeTrigger;
  private NeoForgeTriggers.EnterMystDimensionQuinnTrigger enterMystDimensionQuinnTrigger;

  @Override
  public CriterionTrigger<?> createEnterMystDimensionSafeTrigger() {
    return new NeoForgeTriggers.EnterMystDimensionSafeTrigger();
  }

  @Override
  public CriterionTrigger<?> createEnterMystDimensionQuinnTrigger() {
    return new NeoForgeTriggers.EnterMystDimensionQuinnTrigger();
  }

  @Override
  public CriterionTrigger<?> createWritingDeskWriteTrigger() {
    return new NeoForgeTriggers.WritingDeskWriteTrigger();
  }

  @Override
  public void registerTriggers() {
    // Registration happens via DeferredRegister; triggers are resolved lazily.
  }

  @Override
  public Object getEnterMystDimensionSafeTrigger() {
    return NeoForgeAdvancementTriggers.ENTER_MYST_DIMENSION_SAFE.get();
  }

  @Override
  public Object getEnterMystDimensionQuinnTrigger() {
    return NeoForgeAdvancementTriggers.ENTER_MYST_DIMENSION_QUINN.get();
  }

  @Override
  public Object getWritingDeskWriteTrigger() {
    return NeoForgeAdvancementTriggers.WRITING_DESK_WRITE.get();
  }
}
