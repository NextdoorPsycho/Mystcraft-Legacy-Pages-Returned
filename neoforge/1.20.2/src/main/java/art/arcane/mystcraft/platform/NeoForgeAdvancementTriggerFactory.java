package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.neoforge.NeoForgeTriggers;
import art.arcane.mystcraft.platform.services.IAdvancementTriggerFactory;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

/**
 * NeoForge implementation of advancement trigger factory.
 */
public class NeoForgeAdvancementTriggerFactory implements IAdvancementTriggerFactory {

  private NeoForgeTriggers.WritingDeskWriteTrigger writingDeskWriteTrigger;
  private NeoForgeTriggers.EnterMystDimensionSafeTrigger enterMystDimensionSafeTrigger;
  private NeoForgeTriggers.EnterMystDimensionQuinnTrigger enterMystDimensionQuinnTrigger;

  private static Method findRegisterMethod() throws NoSuchMethodException {
    Method[] methods = CriteriaTriggers.class.getDeclaredMethods();
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
    writingDeskWriteTrigger = new NeoForgeTriggers.WritingDeskWriteTrigger();
    enterMystDimensionSafeTrigger = new NeoForgeTriggers.EnterMystDimensionSafeTrigger();
    enterMystDimensionQuinnTrigger = new NeoForgeTriggers.EnterMystDimensionQuinnTrigger();

    try {
      Method registerMethod = findRegisterMethod();
      registerMethod.setAccessible(true);
      if (registerMethod.getParameterTypes()[0] == String.class) {
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
