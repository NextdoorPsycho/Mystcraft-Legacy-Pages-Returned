package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.ColoredLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * Renderer for ColoredLightningEntity.
 * Based on vanilla LightningBoltRenderer but uses the entity's color.
 * <p>
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class ColoredLightningRenderer extends EntityRenderer<ColoredLightningEntity> {

  public ColoredLightningRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  private static void quad(Matrix4f matrix, VertexConsumer consumer,
                           float x1, float z1, int y,
                           float x2, float z2,
                           float red, float green, float blue, float alpha,
                           boolean x1Neg, boolean z1Neg, boolean x2Neg, boolean z2Neg) {
    consumer.vertex(matrix, getX(x1, x1Neg), (float) (y * 16), getZ(z1, z1Neg))
        .color(red, green, blue, alpha).endVertex();
    consumer.vertex(matrix, getX(x2, x2Neg), (float) ((y + 1) * 16), getZ(z2, z2Neg))
        .color(red, green, blue, alpha).endVertex();
    consumer.vertex(matrix, getX(x2, !x2Neg), (float) ((y + 1) * 16), getZ(z2, !z2Neg))
        .color(red, green, blue, alpha).endVertex();
    consumer.vertex(matrix, getX(x1, !x1Neg), (float) (y * 16), getZ(z1, !z1Neg))
        .color(red, green, blue, alpha).endVertex();
  }

  private static float getX(float value, boolean neg) {
    if (neg) {
      return value - 0.5F;
    } else {
      return value + 0.5F;
    }
  }

  private static float getZ(float value, boolean neg) {
    if (neg) {
      return value - 0.5F;
    } else {
      return value + 0.5F;
    }
  }

  @Override
  public void render(ColoredLightningEntity entity, float entityYaw, float partialTicks,
                     PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    float[] segments = new float[8];
    float[] segmentsOld = new float[8];
    float baseBrightness = 0.0F;
    // 1.18.2: Use java.util.Random instead of RandomSource
    Random random = new Random(entity.seed);

    for (int layer = 7; layer >= 0; --layer) {
      segments[layer] = baseBrightness;
      baseBrightness += (random.nextFloat() - random.nextFloat());
    }

    // Get custom color from entity
    float red = entity.getRed();
    float green = entity.getGreen();
    float blue = entity.getBlue();

    VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
    Matrix4f matrix4f = poseStack.last().pose();

    for (int branch = 0; branch < 4; ++branch) {
      // 1.18.2: Use java.util.Random
      Random branchRandom = new Random(entity.seed);
      for (int layer = 0; layer < 3; ++layer) {
        int layerTarget = 7;
        int layerEnd = 0;
        if (layer > 0) {
          layerTarget = 7 - layer;
        }
        if (layer > 0) {
          layerEnd = layerTarget - 2;
        }

        float currentX = segments[layerTarget] - baseBrightness;
        float currentY = segments[layerEnd] - baseBrightness;

        for (int segment = layerTarget; segment >= layerEnd; --segment) {
          float nextX = currentX;
          float nextY = currentY;
          if (layer == 0) {
            currentX += (branchRandom.nextFloat() - 0.5F) * 2.0F;
            currentY += (branchRandom.nextFloat() - 0.5F) * 2.0F;
          } else {
            currentX += (branchRandom.nextFloat() - 0.5F) * 2.0F * 0.5F;
            currentY += (branchRandom.nextFloat() - 0.5F) * 2.0F * 0.5F;
          }

          float height = (float) (segment * 16);
          float heightNext = (float) ((segment + 1) * 16);
          float width = 0.1F + (float) branch * 0.2F;

          if (layer == 0) {
            width *= (float) segment * 0.1F + 1.0F;
          }

          float alpha = 0.5F;
          if (layer == 0) {
            alpha *= 0.5F;
          }

          // Draw with custom color
          quad(matrix4f, vertexConsumer, currentX, currentY, segment, nextX, nextY, red, green, blue, alpha, false, false, true, false);
          quad(matrix4f, vertexConsumer, currentX, currentY, segment, nextX, nextY, red, green, blue, alpha, true, false, true, true);
          quad(matrix4f, vertexConsumer, currentX, currentY, segment, nextX, nextY, red, green, blue, alpha, true, true, false, true);
          quad(matrix4f, vertexConsumer, currentX, currentY, segment, nextX, nextY, red, green, blue, alpha, false, true, false, false);
        }
      }
    }
  }

  @Override
  public ResourceLocation getTextureLocation(ColoredLightningEntity entity) {
    return null; // Lightning doesn't use textures
  }
}
