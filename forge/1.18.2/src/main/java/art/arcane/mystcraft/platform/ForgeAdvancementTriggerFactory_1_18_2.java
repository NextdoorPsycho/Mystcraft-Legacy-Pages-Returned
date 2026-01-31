package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.advancements.trigger.EnterMystDimensionQuinnTrigger_1_18_2;
import art.arcane.mystcraft.advancements.trigger.EnterMystDimensionSafeTrigger_1_18_2;
import art.arcane.mystcraft.advancements.trigger.WritingDeskWriteTrigger_1_18_2;
import art.arcane.mystcraft.platform.services.IAdvancementTriggerFactory;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

/**
 * Forge 1.18.2 implementation of advancement trigger factory.
 * Uses the older API with ContextAwarePredicate (no Optional) and getId().
 */
public class ForgeAdvancementTriggerFactory_1_18_2 implements IAdvancementTriggerFactory {

  private WritingDeskWriteTrigger_1_18_2 writingDeskWriteTrigger;
  private EnterMystDimensionSafeTrigger_1_18_2 enterMystDimensionSafeTrigger;
  private EnterMystDimensionQuinnTrigger_1_18_2 enterMystDimensionQuinnTrigger;

  private static Method findRegisterMethod() throws NoSuchMethodException {
    Method[] methods = CriteriaTriggers.class.getDeclaredMethods();
    // 1.18.2 uses register(CriterionTrigger) where the trigger has getId()
    for (Method method : methods) {
      Class<?>[] params = method.getParameterTypes();
      if (params.length == 1 && CriterionTrigger.class.isAssignableFrom(params[0])) {
        return method;
      }
    }
    // Fallback: try 2-param version (shouldn't be needed for 1.18.2)
    for (Method method : methods) {
      Class<?>[] params = method.getParameterTypes();
      if (params.length != 2) {
        continue;
      }
      if (!CriterionTrigger.class.isAssignableFrom(params[1])) {
        continue;
      }
      if (params[0] == ResourceLocation.class || params[0] == String.class) {
        return method;
      }
    }
    throw new NoSuchMethodException("No CriteriaTriggers register method found");
  }

  @Override
  public CriterionTrigger<?> createEnterMystDimensionSafeTrigger() {
    return new EnterMystDimensionSafeTrigger_1_18_2();
  }

  @Override
  public CriterionTrigger<?> createEnterMystDimensionQuinnTrigger() {
    return new EnterMystDimensionQuinnTrigger_1_18_2();
  }

  @Override
  public CriterionTrigger<?> createWritingDeskWriteTrigger() {
    return new WritingDeskWriteTrigger_1_18_2();
  }

  @Override
  public void registerTriggers() {
    writingDeskWriteTrigger = new WritingDeskWriteTrigger_1_18_2();
    enterMystDimensionSafeTrigger = new EnterMystDimensionSafeTrigger_1_18_2();
    enterMystDimensionQuinnTrigger = new EnterMystDimensionQuinnTrigger_1_18_2();

    try {
      Method registerMethod = findRegisterMethod();
      registerMethod.setAccessible(true);
      Class<?>[] params = registerMethod.getParameterTypes();

      if (params.length == 1) {
        // 1.18.2 style: register(CriterionTrigger) - trigger has getId()
        registerMethod.invoke(null, writingDeskWriteTrigger);
        registerMethod.invoke(null, enterMystDimensionSafeTrigger);
        registerMethod.invoke(null, enterMystDimensionQuinnTrigger);
      } else if (params[0] == String.class) {
        registerMethod.invoke(null, "mystcraft:writing_desk_write", writingDeskWriteTrigger);
        registerMethod.invoke(null, "mystcraft:enter_myst_dimension_safe", enterMystDimensionSafeTrigger);
        registerMethod.invoke(null, "mystcraft:enter_myst_dimension_quinn", enterMystDimensionQuinnTrigger);
      } else {
        registerMethod.invoke(null, new ResourceLocation("mystcraft", "writing_desk_write"), writingDeskWriteTrigger);
        registerMethod.invoke(null, new ResourceLocation("mystcraft", "enter_myst_dimension_safe"), enterMystDimensionSafeTrigger);
        registerMethod.invoke(null, new ResourceLocation("mystcraft", "enter_myst_dimension_quinn"), enterMystDimensionQuinnTrigger);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to register Mystcraft advancement triggers", e);
    }
  }

  @Override
  public Object getEnterMystDimensionSafeTrigger() {
    return enterMystDimensionSafeTrigger;
  }

  @Override
  public Object getEnterMystDimensionQuinnTrigger() {
    return enterMystDimensionQuinnTrigger;
  }

  @Override
  public Object getWritingDeskWriteTrigger() {
    return writingDeskWriteTrigger;
  }
}
