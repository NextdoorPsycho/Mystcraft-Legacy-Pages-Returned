package art.arcane.mystcraft.advancements;

import net.minecraft.advancements.CriteriaTriggers;

import java.lang.reflect.Method;

/**
 * Registers Mystcraft custom advancement criteria triggers.
 */
public final class ModAdvancements {

    public static final WritingDeskWriteTrigger WRITING_DESK_WRITE = new WritingDeskWriteTrigger();
    public static final EnterMystDimensionSafeTrigger ENTER_MYST_DIMENSION_SAFE = new EnterMystDimensionSafeTrigger();
    public static final EnterMystDimensionQuinnTrigger ENTER_MYST_DIMENSION_QUINN = new EnterMystDimensionQuinnTrigger();

    private ModAdvancements() {
    }

    /**
     * Registers all custom criteria triggers with the vanilla registry.
     * Uses reflection because CriteriaTriggers.register is private in vanilla 1.20.2.
     * Must be called during common setup.
     */
    public static void register() {
        try {
            Method registerMethod = CriteriaTriggers.class.getDeclaredMethod("register", String.class, net.minecraft.advancements.CriterionTrigger.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(null, "mystcraft:writing_desk_write", WRITING_DESK_WRITE);
            registerMethod.invoke(null, "mystcraft:enter_myst_dimension_safe", ENTER_MYST_DIMENSION_SAFE);
            registerMethod.invoke(null, "mystcraft:enter_myst_dimension_quinn", ENTER_MYST_DIMENSION_QUINN);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Mystcraft advancement triggers", e);
        }
    }
}
