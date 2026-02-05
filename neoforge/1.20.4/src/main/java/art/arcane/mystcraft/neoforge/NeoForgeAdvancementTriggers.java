package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Mystcraft advancement triggers for NeoForge 1.20.4 using DeferredRegister.
 */
public final class NeoForgeAdvancementTriggers {

  private NeoForgeAdvancementTriggers() {
  }

  public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
      DeferredRegister.create(Registries.TRIGGER_TYPE, Mystcraft.MOD_ID);

  public static final DeferredHolder<CriterionTrigger<?>, NeoForgeTriggers.WritingDeskWriteTrigger> WRITING_DESK_WRITE =
      TRIGGERS.register("writing_desk_write", NeoForgeTriggers.WritingDeskWriteTrigger::new);

  public static final DeferredHolder<CriterionTrigger<?>, NeoForgeTriggers.EnterMystDimensionSafeTrigger> ENTER_MYST_DIMENSION_SAFE =
      TRIGGERS.register("enter_myst_dimension_safe", NeoForgeTriggers.EnterMystDimensionSafeTrigger::new);

  public static final DeferredHolder<CriterionTrigger<?>, NeoForgeTriggers.EnterMystDimensionQuinnTrigger> ENTER_MYST_DIMENSION_QUINN =
      TRIGGERS.register("enter_myst_dimension_quinn", NeoForgeTriggers.EnterMystDimensionQuinnTrigger::new);

  public static void register(IEventBus bus) {
    TRIGGERS.register(bus);
  }
}
