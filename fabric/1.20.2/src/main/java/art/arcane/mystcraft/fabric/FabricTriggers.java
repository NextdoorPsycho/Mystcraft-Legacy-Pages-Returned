package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.advancements.EnterMystDimensionQuinnTrigger;
import art.arcane.mystcraft.advancements.EnterMystDimensionSafeTrigger;
import art.arcane.mystcraft.advancements.WritingDeskWriteTrigger;

/**
 * Consolidated Fabric 1.20.2 advancement triggers.
 * Uses the newer API with Optional<ContextAwarePredicate> - no getId() requirement.
 * Re-exports the common trigger classes which use 1.20.2+ API.
 */
public final class FabricTriggers {

  private FabricTriggers() {
  }

  // ========== ENTER MYST DIMENSION (QUINN) ==========
  public static EnterMystDimensionQuinnTrigger createEnterMystDimensionQuinnTrigger() {
    return new EnterMystDimensionQuinnTrigger();
  }

  // ========== ENTER MYST DIMENSION (SAFE) ==========
  public static EnterMystDimensionSafeTrigger createEnterMystDimensionSafeTrigger() {
    return new EnterMystDimensionSafeTrigger();
  }

  // ========== WRITING DESK WRITE ==========
  public static WritingDeskWriteTrigger createWritingDeskWriteTrigger() {
    return new WritingDeskWriteTrigger();
  }
}
