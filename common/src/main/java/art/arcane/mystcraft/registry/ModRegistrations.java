package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.platform.Services;

/**
 * Centralized registration entry point for all Mystcraft content.
 * Platform-specific registration implementations call into this class.
 *
 * This enables a single source of truth for what gets registered while
 * allowing platform-specific IRegistrationHelper implementations to
 * handle the actual registration mechanics.
 */
public final class ModRegistrations {

    /**
     * Initializes all registrations via the platform's registration helper.
     * This should be called from the platform entry point after initialize().
     * @param modEventBus The platform-specific event bus (IEventBus for Forge/NeoForge)
     */
    public static void registerAll(Object modEventBus) {
        Services.REGISTRATION.initialize(modEventBus);
        Services.REGISTRATION.register();
        Services.REGISTRATION.populateCommonRegistries();
    }

    private ModRegistrations() {
    }
}
