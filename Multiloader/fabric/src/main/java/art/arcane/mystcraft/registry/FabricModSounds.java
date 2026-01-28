package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * Sound event registrations for Mystcraft (Fabric).
 * Names match existing sounds.json definitions.
 */
public final class FabricModSounds {

    // Linking sounds
    public static final Supplier<SoundEvent> LINKING_POP = registerSound("linking.pop");
    public static final Supplier<SoundEvent> LINKING_LINK = registerSound("linking.link");
    public static final Supplier<SoundEvent> LINKING_DISARM = registerSound("linking.link-disarm");
    public static final Supplier<SoundEvent> LINKING_FOLLOWING = registerSound("linking.link-following");
    public static final Supplier<SoundEvent> LINKING_INTRA = registerSound("linking.link-intra");
    public static final Supplier<SoundEvent> LINKING_FISSURE = registerSound("linking.link-fissure");
    public static final Supplier<SoundEvent> LINKING_PORTAL = registerSound("linking.link-portal");

    // Meteor sounds
    public static final Supplier<SoundEvent> METEOR_ROAR = registerSound("entity.meteor.roar");
    public static final Supplier<SoundEvent> METEOR_IMPACT = registerSound("entity.meteor.impact");

    private FabricModSounds() {
    }

    private static Supplier<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(Mystcraft.MOD_ID,
                name.replace('.', '_').replace('-', '_')), event);
        return () -> event;
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
