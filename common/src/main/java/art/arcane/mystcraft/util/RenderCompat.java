package art.arcane.mystcraft.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class RenderCompat {

  private static final Method NORMAL_POSE = findMethod(VertexConsumer.class, "normal", PoseStack.Pose.class, float.class, float.class, float.class);
  private static final Method NORMAL_MATRIX = findMethod(VertexConsumer.class, "normal", Matrix3f.class, float.class, float.class, float.class);
  private static final Method NORMAL_SIMPLE = findMethod(VertexConsumer.class, "normal", float.class, float.class, float.class);

  private static final Method SHADER_POSITION_TEX_COLOR_NORMAL = findMethod(GameRenderer.class, "getPositionTexColorNormalShader");
  private static final Method SHADER_POSITION_TEX_COLOR = findMethod(GameRenderer.class, "getPositionTexColorShader");

  private RenderCompat() {
  }

  public static VertexConsumer vertexNormal(VertexConsumer consumer, PoseStack.Pose pose, Matrix3f normal, float nx, float ny, float nz) {
    if (NORMAL_POSE != null) {
      invoke(consumer, NORMAL_POSE, pose, nx, ny, nz);
      return consumer;
    }
    if (NORMAL_MATRIX != null) {
      invoke(consumer, NORMAL_MATRIX, normal, nx, ny, nz);
      return consumer;
    }
    if (NORMAL_SIMPLE != null) {
      invoke(consumer, NORMAL_SIMPLE, nx, ny, nz);
      return consumer;
    }
    return consumer;
  }

  public static ShaderInstance getPositionTexColorNormalShader() {
    if (SHADER_POSITION_TEX_COLOR_NORMAL != null) {
      ShaderInstance shader = invokeStatic(SHADER_POSITION_TEX_COLOR_NORMAL);
      if (shader != null) {
        return shader;
      }
    }
    if (SHADER_POSITION_TEX_COLOR != null) {
      ShaderInstance shader = invokeStatic(SHADER_POSITION_TEX_COLOR);
      if (shader != null) {
        return shader;
      }
    }
    return RenderSystem.getShader();
  }

  public static Supplier<ShaderInstance> positionTexColorNormalShaderSupplier() {
    return RenderCompat::getPositionTexColorNormalShader;
  }

  private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
    try {
      return owner.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static void invoke(Object target, Method method, Object... args) {
    if (method == null) {
      return;
    }
    try {
      method.invoke(target, args);
    } catch (ReflectiveOperationException ignored) {
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invokeStatic(Method method, Object... args) {
    if (method == null) {
      return null;
    }
    try {
      return (T) method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
