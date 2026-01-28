package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import net.minecraft.world.effect.MobEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Generic instability provider that creates effects via reflection.
 * This allows flexible effect instantiation with custom arguments.
 */
public class InstabilityProvider implements IInstabilityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstabilityProvider.class);

    private final Class<? extends IEnvironmentalEffect> effectClass;
    private final Object[] constructorArgs;
    private final boolean useLevel;
    private final Constructor<? extends IEnvironmentalEffect> constructor;

    /**
     * Creates a provider that instantiates effects via reflection.
     *
     * @param useLevel     If true, level is passed as first constructor argument
     * @param effectClass  The effect class to instantiate
     * @param constructorArgs Arguments to pass to the constructor
     */
    public InstabilityProvider(boolean useLevel, Class<? extends IEnvironmentalEffect> effectClass, Object... constructorArgs) {
        this.effectClass = effectClass;
        this.constructorArgs = constructorArgs;
        this.useLevel = useLevel;

        // Build constructor argument types
        Class<?>[] argTypes = new Class<?>[constructorArgs.length + (useLevel ? 1 : 0)];
        if (useLevel) {
            argTypes[0] = int.class;
        }

        for (int i = 0; i < constructorArgs.length; i++) {
            int index = i + (useLevel ? 1 : 0);
            Class<?> argClass = constructorArgs[i].getClass();

            // Handle special cases for primitive wrappers
            if (argClass == Integer.class) {
                argTypes[index] = int.class;
            } else if (argClass == Boolean.class) {
                argTypes[index] = boolean.class;
            } else if (argClass == Float.class) {
                argTypes[index] = float.class;
            } else if (argClass == Double.class) {
                argTypes[index] = double.class;
            } else if (argClass == Long.class) {
                argTypes[index] = long.class;
            } else if (MobEffect.class.isAssignableFrom(argClass)) {
                // MobEffect subclasses should match MobEffect parameter
                argTypes[index] = MobEffect.class;
            } else {
                argTypes[index] = argClass;
            }
        }

        try {
            this.constructor = effectClass.getDeclaredConstructor(argTypes);
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Failed to find constructor for {} with args {}", effectClass.getName(), Arrays.toString(argTypes));
            throw new RuntimeException("Error building instability provider for " + effectClass.getCanonicalName(), e);
        }
    }

    @Override
    public void addEffects(InstabilityDirector director, Integer level) {
        try {
            Object[] args = constructorArgs;
            if (useLevel) {
                // Prepend level to arguments
                args = new Object[constructorArgs.length + 1];
                args[0] = level;
                System.arraycopy(constructorArgs, 0, args, 1, constructorArgs.length);
            }

            // Create effects based on level
            int count = useLevel ? 1 : level;
            for (int i = 0; i < count; i++) {
                IEnvironmentalEffect effect = constructor.newInstance(args);
                director.registerEffect(effect);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating instability effect {}: {}", effectClass.getName(), e.getMessage());
            throw new RuntimeException("Error building effect from " + effectClass.getCanonicalName(), e);
        }
    }
}
