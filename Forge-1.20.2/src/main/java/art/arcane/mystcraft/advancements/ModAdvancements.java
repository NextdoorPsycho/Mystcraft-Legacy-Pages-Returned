package art.arcane.mystcraft.advancements;

import net.minecraft.advancements.CriteriaTriggers;

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
     * Must be called during common setup.
     */
    public static void register() {
        CriteriaTriggers.register("mystcraft:writing_desk_write", WRITING_DESK_WRITE);
        CriteriaTriggers.register("mystcraft:enter_myst_dimension_safe", ENTER_MYST_DIMENSION_SAFE);
        CriteriaTriggers.register("mystcraft:enter_myst_dimension_quinn", ENTER_MYST_DIMENSION_QUINN);
    }
}
