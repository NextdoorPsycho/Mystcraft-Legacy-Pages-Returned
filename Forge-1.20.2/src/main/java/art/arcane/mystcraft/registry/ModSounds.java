package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound event registrations for Mystcraft.
 * Names match existing sounds.json definitions.
 */
public final class ModSounds {

    // Linking sounds
    public static final RegistryObject<SoundEvent> LINKING_POP =
            registerSound("linking.pop");

    public static final RegistryObject<SoundEvent> LINKING_LINK =
            registerSound("linking.link");

    public static final RegistryObject<SoundEvent> LINKING_DISARM =
            registerSound("linking.link-disarm");

    public static final RegistryObject<SoundEvent> LINKING_FOLLOWING =
            registerSound("linking.link-following");

    public static final RegistryObject<SoundEvent> LINKING_INTRA =
            registerSound("linking.link-intra");

    public static final RegistryObject<SoundEvent> LINKING_FISSURE =
            registerSound("linking.link-fissure");

    public static final RegistryObject<SoundEvent> LINKING_PORTAL =
            registerSound("linking.link-portal");

    // Meteor sounds
    public static final RegistryObject<SoundEvent> METEOR_ROAR =
            registerSound("entity.meteor.roar");

    public static final RegistryObject<SoundEvent> METEOR_IMPACT =
            registerSound("entity.meteor.impact");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, name);
        return MystcraftRegistries.SOUNDS.register(name.replace('.', '_').replace('-', '_'),
                () -> SoundEvent.createVariableRangeEvent(id));
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
