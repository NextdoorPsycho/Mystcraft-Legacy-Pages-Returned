package art.arcane.mystcraft.platform.services;

import net.minecraft.advancements.CriterionTrigger;

/**
 * Factory for creating version-specific advancement criterion triggers.
 * <p>
 * The CriterionTrigger API changed between 1.20.1 and 1.20.2:
 * <ul>
 *   <li>1.20.1: createInstance(JsonObject, ContextAwarePredicate, DeserializationContext) with getId() method</li>
 *   <li>1.20.2: createInstance(JsonObject, Optional&lt;ContextAwarePredicate&gt;, DeserializationContext) without getId()</li>
 * </ul>
 */
public interface IAdvancementTriggerFactory {

  /**
   * Creates the EnterMystDimensionSafeTrigger for this version.
   * Triggers when a player enters a Mystcraft dimension with a return linkbook.
   *
   * @return The criterion trigger
   */
  CriterionTrigger<?> createEnterMystDimensionSafeTrigger();

  /**
   * Creates the EnterMystDimensionQuinnTrigger for this version.
   * Triggers when a player enters a Mystcraft dimension without a return linkbook.
   *
   * @return The criterion trigger
   */
  CriterionTrigger<?> createEnterMystDimensionQuinnTrigger();

  /**
   * Creates the WritingDeskWriteTrigger for this version.
   * Triggers when a player writes on a page at the writing desk.
   *
   * @return The criterion trigger
   */
  CriterionTrigger<?> createWritingDeskWriteTrigger();

  /**
   * Registers all advancement triggers with the vanilla CriteriaTriggers registry.
   * Uses version-appropriate registration method.
   */
  void registerTriggers();

  /**
   * Gets the EnterMystDimensionSafeTrigger instance after registration.
   *
   * @return The registered trigger, cast appropriately for the version
   */
  Object getEnterMystDimensionSafeTrigger();

  /**
   * Gets the EnterMystDimensionQuinnTrigger instance after registration.
   *
   * @return The registered trigger, cast appropriately for the version
   */
  Object getEnterMystDimensionQuinnTrigger();

  /**
   * Gets the WritingDeskWriteTrigger instance after registration.
   *
   * @return The registered trigger, cast appropriately for the version
   */
  Object getWritingDeskWriteTrigger();
}
