package art.arcane.mystcraft.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * Per-face quad emission helper used by the Crystal/Receptacle BERs to paint
 * the tileable filigree overlay on every face of the disguise cube.
 *
 * <p>Vertex winding follows vanilla {@code BakedQuad} convention — counter-
 * clockwise looking from outside the cube — so the quads survive
 * {@link net.minecraft.client.renderer.RenderType#cutout()} backface culling.
 *
 * <p>The cube is the canonical unit cube {@code [0,0,0]..[1,1,1]} in the
 * caller's current {@link PoseStack} frame; callers wishing to inset/outset for
 * z-fighting avoidance should apply their own {@code translate}/ {@code scale}
 * before invoking {@link #drawUnitFace}.
 */
public final class QuadHelper {

  private QuadHelper() {
  }

  /**
   * Emit one face of the unit cube as a textured quad. The full sprite UV range
   * covers the face (no sub-region tiling — that's done implicitly when
   * adjacent crystals' textures stitch on the atlas).
   */
  public static void drawUnitFace(PoseStack pose, VertexConsumer consumer,
                                  TextureAtlasSprite sprite, Direction face,
                                  int packedLight, int packedOverlay,
                                  float r, float g, float b, float a) {
    PoseStack.Pose last = pose.last();
    int ir = clampByte((int) (r * 255f));
    int ig = clampByte((int) (g * 255f));
    int ib = clampByte((int) (b * 255f));
    int ia = clampByte((int) (a * 255f));
    float u0 = sprite.getU0();
    float u1 = sprite.getU1();
    float v0 = sprite.getV0();
    float v1 = sprite.getV1();
    Vec3i n = face.getUnitVec3i();
    float nx = n.getX();
    float ny = n.getY();
    float nz = n.getZ();

    switch (face) {
      case DOWN -> {
        emit(consumer, last, 0f, 0f, 1f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 0f, 1f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 0f, 0f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 0f, 0f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
      case UP -> {
        emit(consumer, last, 0f, 1f, 0f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 0f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 1f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 1f, 1f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
      case NORTH -> {
        emit(consumer, last, 0f, 0f, 0f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 0f, 0f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 0f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 1f, 0f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
      case SOUTH -> {
        emit(consumer, last, 0f, 1f, 1f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 1f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 0f, 1f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 0f, 1f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
      case WEST -> {
        emit(consumer, last, 0f, 0f, 0f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 1f, 0f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 1f, 1f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 0f, 0f, 1f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
      case EAST -> {
        emit(consumer, last, 1f, 0f, 1f, u1, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 1f, u1, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 1f, 0f, u0, v0, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
        emit(consumer, last, 1f, 0f, 0f, u0, v1, nx, ny, nz, ir, ig, ib, ia, packedLight, packedOverlay);
      }
    }
  }

  /**
   * Convenience: emit all six faces of the unit cube. Used by the filigree
   * overlay path which paints the same texture on every face.
   */
  public static void drawAllFaces(PoseStack pose, VertexConsumer consumer,
                                  TextureAtlasSprite sprite,
                                  int packedLight, int packedOverlay,
                                  float r, float g, float b, float a) {
    for (Direction face : Direction.values()) {
      drawUnitFace(pose, consumer, sprite, face, packedLight, packedOverlay, r, g, b, a);
    }
  }

  private static void emit(VertexConsumer consumer, PoseStack.Pose pose,
                           float x, float y, float z,
                           float u, float v,
                           float nx, float ny, float nz,
                           int r, int g, int b, int a,
                           int light, int overlay) {
    consumer.addVertex(pose, x, y, z)
        .setColor(r, g, b, a)
        .setUv(u, v)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(pose, nx, ny, nz);
  }

  private static int clampByte(int v) {
    return v < 0 ? 0 : (v > 255 ? 255 : v);
  }
}
