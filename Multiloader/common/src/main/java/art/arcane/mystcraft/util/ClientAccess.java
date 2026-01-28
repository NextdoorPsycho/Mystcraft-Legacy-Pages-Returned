package art.arcane.mystcraft.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-based access to client-only classes to keep common code server-safe.
 */
public final class ClientAccess {

    private static Method getInstanceMethod;
    private static Field levelField;

    private ClientAccess() {
    }

    @Nullable
    public static Object getMinecraftInstance() {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            if (getInstanceMethod == null) {
                getInstanceMethod = mcClass.getMethod("getInstance");
            }
            return getInstanceMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    public static Object getClientLevel() {
        Object minecraft = getMinecraftInstance();
        if (minecraft == null) {
            return null;
        }
        try {
            if (levelField == null) {
                levelField = minecraft.getClass().getDeclaredField("level");
                levelField.setAccessible(true);
            }
            return levelField.get(minecraft);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
