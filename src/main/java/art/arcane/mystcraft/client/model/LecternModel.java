package art.arcane.mystcraft.client.model;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Modern 1.20 model for the Lectern, ported from the original 1.12 ModelLectern.
 * Uses the 64x32 entity texture.
 *
 * Original used ModelPrism for a wedge shape (height1=1, height2=7, width=16, depth=16).
 * This creates a true wedge geometry using manual vertex rendering since ModelPart
 * doesn't support non-rectangular shapes.
 */
public class LecternModel {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/lectern.png");

    // Texture dimensions
    private static final float TEX_WIDTH = 64.0F;
    private static final float TEX_HEIGHT = 32.0F;

    // Original prism dimensions converted to block units (divide by 16)
    // ModelPrism at (-8, -0.5, -8), width=16, height1=1, height2=7, depth=16
    // height1 = left side (x-), height2 = right side (x+)
    // Adjusted so bottom sits at Y=0 (on the ground)
    private static final float X1 = -8F / 16F;
    private static final float X2 = 8F / 16F;
    private static final float Y_BOTTOM = 0F;             // Sits on ground
    private static final float Y_TOP_LEFT = 1F / 16F;     // height1 = 1 pixel
    private static final float Y_TOP_RIGHT = 7F / 16F;    // height2 = 7 pixels
    private static final float Z1 = -8F / 16F;
    private static final float Z2 = 8F / 16F;

    // Back support box: ModelBox(base, 32, 2, -8, -0.5F, -7, 1, 2, 14, 0)
    // This is a thin lip/ridge along the back edge to hold books
    // Adjusted Y so it sits on the prism surface
    private static final float SUPPORT_X1 = -8F / 16F;
    private static final float SUPPORT_X2 = -7F / 16F;    // width = 1
    private static final float SUPPORT_Y1 = 0F;           // Start at ground level
    private static final float SUPPORT_Y2 = 2F / 16F;     // height = 2
    private static final float SUPPORT_Z1 = -7F / 16F;
    private static final float SUPPORT_Z2 = 7F / 16F;     // depth = 14

    public LecternModel() {
    }

    public RenderType renderType() {
        return RenderType.entitySolid(TEXTURE);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Render the wedge (prism) shape
        renderPrism(pose, normal, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);

        // Render the back support box
        renderBackSupport(pose, normal, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void renderPrism(Matrix4f pose, Matrix3f normal, VertexConsumer vc, int light, int overlay,
                             float r, float g, float b, float a) {
        // Prism vertices (8 points with different heights on left vs right)
        // point1: (X1, Y_BOTTOM, Z1) - front bottom left
        // point2: (X2, Y_BOTTOM, Z1) - front bottom right
        // point3: (X2, Y_TOP_RIGHT, Z1) - front top right
        // point4: (X1, Y_TOP_LEFT, Z1) - front top left
        // point5: (X1, Y_BOTTOM, Z2) - back bottom left
        // point6: (X2, Y_BOTTOM, Z2) - back bottom right
        // point7: (X2, Y_TOP_RIGHT, Z2) - back top right
        // point8: (X1, Y_TOP_LEFT, Z2) - back top left

        // Texture UV mapping based on original (texture coords in pixels, convert to 0-1)
        // Bottom face: texOffs(0, 0) to (width, depth) = (0,0) to (16, 16)
        // Top face: texOffs(16, 0) to (32, 16)
        // x+ face (right): texOffs(32, 0) to (32+height2, depth) = (32, 0) to (39, 16)
        // x- face (left): texOffs(39, 0) to (39+height1, depth) = (39, 0) to (40, 16)
        // z- face (front): texOffs(0, 16) to (16, 16+height2) = (0, 16) to (16, 23)
        // z+ face (back): texOffs(16, 16) to (32, 16+height2) = (16, 16) to (32, 23)

        // Bottom face (quad: point2, point6, point5, point1)
        float bottomU1 = 0 / TEX_WIDTH;
        float bottomV1 = 0 / TEX_HEIGHT;
        float bottomU2 = 16 / TEX_WIDTH;
        float bottomV2 = 16 / TEX_HEIGHT;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X2, Y_BOTTOM, Z1,   // point2
                X2, Y_BOTTOM, Z2,   // point6
                X1, Y_BOTTOM, Z2,   // point5
                X1, Y_BOTTOM, Z1,   // point1
                0, -1, 0,           // normal down
                bottomU1, bottomV1, bottomU2, bottomV2);

        // Top face (quad: point7, point3, point4, point8) - sloped surface
        float topU1 = 16 / TEX_WIDTH;
        float topV1 = 0 / TEX_HEIGHT;
        float topU2 = 32 / TEX_WIDTH;
        float topV2 = 16 / TEX_HEIGHT;
        // Calculate normal for sloped top surface
        float topNx = (Y_TOP_LEFT - Y_TOP_RIGHT);  // slope in X
        float topNy = 1.0f;
        float topNz = 0;
        float topNlen = (float) Math.sqrt(topNx * topNx + topNy * topNy);
        topNx /= topNlen;
        topNy /= topNlen;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X2, Y_TOP_RIGHT, Z2,  // point7
                X2, Y_TOP_RIGHT, Z1,  // point3
                X1, Y_TOP_LEFT, Z1,   // point4
                X1, Y_TOP_LEFT, Z2,   // point8
                topNx, topNy, topNz,
                topU1, topV1, topU2, topV2);

        // Right face x+ (quad: point7, point6, point2, point3)
        float rightU1 = 32 / TEX_WIDTH;
        float rightV1 = 0 / TEX_HEIGHT;
        float rightU2 = 39 / TEX_WIDTH;
        float rightV2 = 16 / TEX_HEIGHT;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X2, Y_TOP_RIGHT, Z2,  // point7
                X2, Y_BOTTOM, Z2,     // point6
                X2, Y_BOTTOM, Z1,     // point2
                X2, Y_TOP_RIGHT, Z1,  // point3
                1, 0, 0,              // normal right
                rightU1, rightV1, rightU2, rightV2);

        // Left face x- (quad: point4, point1, point5, point8)
        float leftU1 = 39 / TEX_WIDTH;
        float leftV1 = 0 / TEX_HEIGHT;
        float leftU2 = 40 / TEX_WIDTH;
        float leftV2 = 16 / TEX_HEIGHT;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X1, Y_TOP_LEFT, Z1,   // point4
                X1, Y_BOTTOM, Z1,     // point1
                X1, Y_BOTTOM, Z2,     // point5
                X1, Y_TOP_LEFT, Z2,   // point8
                -1, 0, 0,             // normal left
                leftU1, leftV1, leftU2, leftV2);

        // Front face z- (quad: point4, point3, point2, point1) - trapezoid
        float frontU1 = 0 / TEX_WIDTH;
        float frontV1 = 16 / TEX_HEIGHT;
        float frontU2 = 16 / TEX_WIDTH;
        float frontV2 = 23 / TEX_HEIGHT;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X1, Y_TOP_LEFT, Z1,   // point4
                X2, Y_TOP_RIGHT, Z1,  // point3
                X2, Y_BOTTOM, Z1,     // point2
                X1, Y_BOTTOM, Z1,     // point1
                0, 0, -1,             // normal front
                frontU1, frontV1, frontU2, frontV2);

        // Back face z+ (quad: point5, point6, point7, point8)
        float backU1 = 16 / TEX_WIDTH;
        float backV1 = 16 / TEX_HEIGHT;
        float backU2 = 32 / TEX_WIDTH;
        float backV2 = 23 / TEX_HEIGHT;
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                X1, Y_BOTTOM, Z2,     // point5
                X2, Y_BOTTOM, Z2,     // point6
                X2, Y_TOP_RIGHT, Z2,  // point7
                X1, Y_TOP_LEFT, Z2,   // point8
                0, 0, 1,              // normal back
                backU1, backV1, backU2, backV2);
    }

    private void renderBackSupport(Matrix4f pose, Matrix3f normal, VertexConsumer vc, int light, int overlay,
                                   float r, float g, float b, float a) {
        // Simple box for back support
        // Texture at (32, 2), using a small portion
        float u1 = 32 / TEX_WIDTH;
        float v1 = 2 / TEX_HEIGHT;
        float u2 = 46 / TEX_WIDTH;  // width of texture area
        float v2 = 18 / TEX_HEIGHT;

        // Bottom
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z1,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z2,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z2,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z1,
                0, -1, 0, u1, v1, u2, v2);

        // Top
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z1,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z1,
                0, 1, 0, u1, v1, u2, v2);

        // Front
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z1,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z1,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z1,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z1,
                0, 0, -1, u1, v1, u2, v2);

        // Back
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z2,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z2,
                0, 0, 1, u1, v1, u2, v2);

        // Left
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X1, SUPPORT_Y2, SUPPORT_Z1,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z1,
                SUPPORT_X1, SUPPORT_Y1, SUPPORT_Z2,
                -1, 0, 0, u1, v1, u2, v2);

        // Right
        addQuad(pose, normal, vc, light, overlay, r, g, b, a,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z1,
                SUPPORT_X2, SUPPORT_Y2, SUPPORT_Z2,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z2,
                SUPPORT_X2, SUPPORT_Y1, SUPPORT_Z1,
                1, 0, 0, u1, v1, u2, v2);
    }

    private void addQuad(Matrix4f pose, Matrix3f normal, VertexConsumer vc, int light, int overlay,
                         float r, float g, float b, float a,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float nx, float ny, float nz,
                         float u1, float v1, float u2, float v2) {
        addVertex(pose, normal, vc, light, overlay, r, g, b, a, x1, y1, z1, u1, v1, nx, ny, nz);
        addVertex(pose, normal, vc, light, overlay, r, g, b, a, x2, y2, z2, u2, v1, nx, ny, nz);
        addVertex(pose, normal, vc, light, overlay, r, g, b, a, x3, y3, z3, u2, v2, nx, ny, nz);
        addVertex(pose, normal, vc, light, overlay, r, g, b, a, x4, y4, z4, u1, v2, nx, ny, nz);
    }

    private void addVertex(Matrix4f pose, Matrix3f normal, VertexConsumer vc, int light, int overlay,
                           float r, float g, float b, float a,
                           float x, float y, float z, float u, float v,
                           float nx, float ny, float nz) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
