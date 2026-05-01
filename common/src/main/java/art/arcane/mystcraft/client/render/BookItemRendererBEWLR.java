package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.procedural.BookItemTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.BookTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.CoverPalette;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.GuidebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.LinkbookUnlinkedItem;
import art.arcane.mystcraft.util.ItemStackNbt;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Custom {@link BlockEntityWithoutLevelRenderer} for the book items (Agebook,
 * Linkbook, Personal Link Book, Linkbook Unlinked, Guidebook).
 *
 * <p>Mirrors the structure of {@link PageItemRendererBEWLR}: each book stack
 * is mapped to a unique procedural icon via
 * {@link BookItemTextureFactory#getItemTexture(ItemStack)}, which composes a
 * closed-book sprite using the same palette and motif that
 * {@link BookTextureFactory} uses for the open-book GUI cover. The inventory
 * icon, the open-book cover, and the in-world dropped book therefore all read
 * as the same object.
 *
 * <p><b>Display contexts:</b> the GUI inventory icon is rendered as a
 * single flat quad (vanilla item-icon convention), while every other context
 * (held, ground, fixed/item-frame, head) renders the book as a
 * <i>3D cuboid</i> — front cover, back cover, spine, top edge, bottom
 * edge, and fore-edge — so the held book reads as a small physical object
 * rather than a 2D sticker. The textured faces use the procedural cover; the
 * four edge faces are tinted with the leather/page colours sourced from
 * {@link CoverPalette#get(ResourceLocation)}.
 *
 * <p>Generic / non-book stacks are left to vanilla rendering by exiting early.
 */
public class BookItemRendererBEWLR extends BlockEntityWithoutLevelRenderer {

  private static final float MAX_OPEN_DEGREES = 160f;
  private static final float OPEN_LERP_PER_SECOND = 2.5f;
  private static final float MAX_DT_SECONDS = 0.1f;
  private static final float BOOK_W = 0.84f;
  private static final float BOOK_H = 1.00f;
  private static final float BOOK_D = 0.18f;
  private static final float COVER_T = 0.020f;
  private static final float OVH = 0.030f;
  private static final float X0 = 0.5f - BOOK_W / 2f;
  private static final float PAGE_X0 = X0;
  private static final float X1 = 0.5f + BOOK_W / 2f;
  private static final float PAGE_X1 = X1 - OVH;
  private static final float Y0 = 0.5f - BOOK_H / 2f;
  private static final float PAGE_Y0 = Y0 + OVH;
  private static final float Y1 = 0.5f + BOOK_H / 2f;
  private static final float PAGE_Y1 = Y1 - OVH;
  private static final float Z0 = 0.5f - BOOK_D / 2f;
  private static final float PAGE_Z0 = Z0 + COVER_T;
  private static final float Z1 = 0.5f + BOOK_D / 2f;
  private static final float PAGE_Z1 = Z1 - COVER_T;
  private static final float COVER_U_LEFT = 14.5f / 64f;
  private static final float COVER_U_RIGHT = 57.5f / 64f;
  private static final float COVER_V_TOP = 6.5f / 64f;
  private static final float COVER_V_BOTTOM = 57.5f / 64f;
  private static BookItemRendererBEWLR instance;
  private static ResourceLocation whiteTextureLocation;
  private static volatile float openTarget = 0f;
  private static float openAmount = 0f;
  private static long lastTickNanos = 0L;

  private BookItemRendererBEWLR() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
        Minecraft.getInstance().getEntityModels());
  }

  public static BookItemRendererBEWLR getInstance() {
    if (instance == null) {
      instance = new BookItemRendererBEWLR();
    }
    return instance;
  }

  /**
   * Drops every cached book-icon texture and the associated GPU memory. Call on
   * resource-pack reload so palette changes pick up.
   */
  public static void clearCache() {
    BookItemTextureFactory.reset();
  }

  /**
   * Sets the target open state for the held first-person book animation. Called
   * by {@link art.arcane.mystcraft.client.screen.BookScreen} on init (true) and
   * removed (false). The actual open amount lerps toward this target so the
   * cover animates smoothly rather than snapping.
   */
  public static void setHeldBookOpen(boolean open) {
    openTarget = open ? 1f : 0f;
  }

  private static void tickOpenAnimation() {
    long now = System.nanoTime();
    if (lastTickNanos == 0L) {
      lastTickNanos = now;
      return;
    }
    float dt = Math.min(MAX_DT_SECONDS, (now - lastTickNanos) / 1_000_000_000f);
    lastTickNanos = now;
    float step = OPEN_LERP_PER_SECOND * dt;
    float t = openTarget;
    if (openAmount < t) openAmount = Math.min(t, openAmount + step);
    else if (openAmount > t) openAmount = Math.max(t, openAmount - step);
  }

  @NotNull
  private static synchronized ResourceLocation whiteTexture() {
    if (whiteTextureLocation != null) {
      return whiteTextureLocation;
    }
    NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, true);
    image.setPixelRGBA(0, 0, 0xFFFFFFFF);
    DynamicTexture tex = new DynamicTexture(image);
    whiteTextureLocation = Minecraft.getInstance().getTextureManager()
        .register("mystcraft_book_solid_white", tex);
    Mystcraft.LOGGER.debug("[BookItemRendererBEWLR] Registered solid-white edge texture: {}",
        whiteTextureLocation);
    return whiteTextureLocation;
  }

  @Override
  public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

    if (!(stack.getItem() instanceof AgebookItem
        || stack.getItem() instanceof LinkbookItem
        || stack.getItem() instanceof LinkbookUnlinkedItem
        || stack.getItem() instanceof GuidebookItem)) {
      return;
    }

    tickOpenAnimation();

    ResourceLocation texture = BookItemTextureFactory.getItemTexture(stack);
    int light = (displayContext == ItemDisplayContext.GUI) ? 0xF000F0 : packedLight;

    boolean isFirstPersonHeld =
        displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    float effectiveOpen = isFirstPersonHeld ? openAmount : 0f;

    poseStack.pushPose();

    if (displayContext == ItemDisplayContext.GUI) {

      renderFlatIcon(poseStack, bufferSource, texture, light, packedOverlay);
    } else {

      renderCuboid(poseStack, bufferSource, texture, stack, light, packedOverlay, effectiveOpen);
    }

    poseStack.popPose();
  }

  private void renderFlatIcon(PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, int light, int overlay) {
    RenderType renderType = RenderType.entityCutoutNoCull(texture);
    VertexConsumer consumer = bufferSource.getBuffer(renderType);
    Matrix4f matrix = poseStack.last().pose();

    quad2D(consumer, matrix, 0f, 0f, 1f, 1f, 0.5f + 0.003f,
        0f, 1f, 1f, 0f, 1f, light, overlay, 255, 255, 255);
  }

  private void renderCuboid(PoseStack poseStack, MultiBufferSource bufferSource,
                            ResourceLocation cover, ItemStack stack,
                            int light, int overlay, float openAmount) {
    Matrix4f matrix = poseStack.last().pose();
    BookColors colors = colorsFor(stack);

    int leatherR = (colors.leather >>> 16) & 0xFF;
    int leatherG = (colors.leather >>> 8) & 0xFF;
    int leatherB = colors.leather & 0xFF;

    int pageR = (colors.page >>> 16) & 0xFF;
    int pageG = (colors.page >>> 8) & 0xFF;
    int pageB = colors.page & 0xFF;

    int bandR = Math.max(0, leatherR - 30);
    int bandG = Math.max(0, leatherG - 30);
    int bandB = Math.max(0, leatherB - 30);

    VertexConsumer coverConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(cover));

    poseStack.pushPose();
    if (openAmount > 0.001f) {
      poseStack.translate(X0, 0f, Z1);
      poseStack.mulPose(Axis.YP.rotationDegrees(-openAmount * MAX_OPEN_DEGREES));
      poseStack.translate(-X0, 0f, -Z1);
    }
    Matrix4f coverMatrix = poseStack.last().pose();
    Matrix4f coverMatrixCopy = new Matrix4f(coverMatrix);

    quad2D(coverConsumer, coverMatrix, X0, Y0, X1, Y1, Z1,
        COVER_U_LEFT, COVER_V_BOTTOM, COVER_U_RIGHT, COVER_V_TOP, +1f,
        light, overlay, 255, 255, 255);
    poseStack.popPose();

    quad2D(coverConsumer, matrix, X1, Y0, X0, Y1, Z0,
        COVER_U_LEFT, COVER_V_BOTTOM, COVER_U_RIGHT, COVER_V_TOP, -1f,
        light, overlay, 255, 255, 255);

    ResourceLocation white = whiteTexture();
    VertexConsumer solidConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(white));

    if (openAmount > 0.001f) {
      quad2D(solidConsumer, coverMatrixCopy, X0, Y0, X1, Y1, Z1 - 0.002f,
          0f, 0f, 1f, 1f, -1f, light, overlay, pageR, pageG, pageB);
    }

    quad2D(solidConsumer, matrix, PAGE_X0, PAGE_Y0, PAGE_X1, PAGE_Y1, PAGE_Z1,
        0f, 0f, 1f, 1f, +1f, light, overlay, pageR, pageG, pageB);

    float lpX0 = PAGE_X0 + 0.06f;
    float lpX1 = PAGE_X1 - 0.08f;
    float lpY0 = PAGE_Y1 - 0.50f;
    float lpY1 = PAGE_Y1 - 0.10f;
    float lpZ = PAGE_Z1 + 0.001f;
    quad2D(solidConsumer, matrix, lpX0, lpY0, lpX1, lpY1, lpZ,
        0f, 0f, 1f, 1f, +1f, light, overlay, 0, 0, 0);

    quadYZ(solidConsumer, matrix, X0, Y0, Z0, Y1, Z1, -1f,
        light, overlay, leatherR, leatherG, leatherB);

    float bandProtrude = 0.020f;
    float bandHalfHeight = 0.025f;
    float titleHalfHeight = 0.060f;
    float[] bandY = {0.20f, 0.40f, 0.60f, 0.80f};
    int titleBandIndex = 1;

    for (int i = 0; i < bandY.length; i++) {
      float by = Y0 + (Y1 - Y0) * bandY[i];
      float halfH = (i == titleBandIndex) ? titleHalfHeight : bandHalfHeight;

      quadYZ(solidConsumer, matrix, X0 - bandProtrude,
          by - halfH, Z0, by + halfH, Z1, -1f,
          light, overlay, bandR, bandG, bandB);

      quadXZ(solidConsumer, matrix, X0 - bandProtrude, X0,
          by + halfH, Z0, Z1, +1f,
          light, overlay, bandR, bandG, bandB);

      quadXZ(solidConsumer, matrix, X0 - bandProtrude, X0,
          by - halfH, Z0, Z1, -1f,
          light, overlay, bandR, bandG, bandB);

      quad2D(solidConsumer, matrix, X0 - bandProtrude, by - halfH, X0, by + halfH, Z1,
          0f, 0f, 1f, 1f, +1f, light, overlay, bandR, bandG, bandB);

      quad2D(solidConsumer, matrix, X0 - bandProtrude, by - halfH, X0, by + halfH, Z0,
          0f, 0f, 1f, 1f, -1f, light, overlay, bandR, bandG, bandB);
    }

    quadXZ(solidConsumer, matrix, X0, X1, Y1, Z1 - COVER_T, Z1, +1f,
        light, overlay, leatherR, leatherG, leatherB);

    quadXZ(solidConsumer, matrix, X0, X1, Y1, Z0, Z0 + COVER_T, +1f,
        light, overlay, leatherR, leatherG, leatherB);

    quadXZ(solidConsumer, matrix, PAGE_X0, PAGE_X1, PAGE_Y1, PAGE_Z0, PAGE_Z1, +1f,
        light, overlay, pageR, pageG, pageB);

    quad2D(solidConsumer, matrix, PAGE_X0, PAGE_Y1, PAGE_X1, Y1, PAGE_Z1,
        0f, 0f, 1f, 1f, -1f, light, overlay, leatherR, leatherG, leatherB);

    quad2D(solidConsumer, matrix, PAGE_X0, PAGE_Y1, PAGE_X1, Y1, PAGE_Z0,
        0f, 0f, 1f, 1f, +1f, light, overlay, leatherR, leatherG, leatherB);

    quadXZ(solidConsumer, matrix, X0, X1, Y0, Z1 - COVER_T, Z1, -1f,
        light, overlay, leatherR, leatherG, leatherB);
    quadXZ(solidConsumer, matrix, X0, X1, Y0, Z0, Z0 + COVER_T, -1f,
        light, overlay, leatherR, leatherG, leatherB);
    quadXZ(solidConsumer, matrix, PAGE_X0, PAGE_X1, PAGE_Y0, PAGE_Z0, PAGE_Z1, -1f,
        light, overlay, pageR, pageG, pageB);
    quad2D(solidConsumer, matrix, PAGE_X0, Y0, PAGE_X1, PAGE_Y0, PAGE_Z1,
        0f, 0f, 1f, 1f, -1f, light, overlay, leatherR, leatherG, leatherB);
    quad2D(solidConsumer, matrix, PAGE_X0, Y0, PAGE_X1, PAGE_Y0, PAGE_Z0,
        0f, 0f, 1f, 1f, +1f, light, overlay, leatherR, leatherG, leatherB);

    quadYZ(solidConsumer, matrix, X1, Y0, Z1 - COVER_T, Y1, Z1, +1f,
        light, overlay, leatherR, leatherG, leatherB);

    quadYZ(solidConsumer, matrix, X1, Y0, Z0, Y1, Z0 + COVER_T, +1f,
        light, overlay, leatherR, leatherG, leatherB);

    quadYZ(solidConsumer, matrix, PAGE_X1, PAGE_Y0, PAGE_Z0, PAGE_Y1, PAGE_Z1, +1f,
        light, overlay, pageR, pageG, pageB);

    quad2D(solidConsumer, matrix, PAGE_X1, PAGE_Y0, X1, PAGE_Y1, PAGE_Z1,
        0f, 0f, 1f, 1f, -1f, light, overlay, leatherR, leatherG, leatherB);

    quad2D(solidConsumer, matrix, PAGE_X1, PAGE_Y0, X1, PAGE_Y1, PAGE_Z0,
        0f, 0f, 1f, 1f, +1f, light, overlay, leatherR, leatherG, leatherB);
  }

  private void quad2D(VertexConsumer consumer, Matrix4f matrix,
                      float x1, float y1, float x2, float y2, float z,
                      float u1, float v1, float u2, float v2, float normalZ,
                      int light, int overlay, int r, int g, int b) {
    if (normalZ > 0) {
      consumer.vertex(matrix, x1, y1, z).color(r, g, b, 255)
          .uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y1, z).color(r, g, b, 255)
          .uv(u2, v1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y2, z).color(r, g, b, 255)
          .uv(u2, v2).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x1, y2, z).color(r, g, b, 255)
          .uv(u1, v2).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
    } else {

      consumer.vertex(matrix, x1, y2, z).color(r, g, b, 255)
          .uv(u1, v2).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y2, z).color(r, g, b, 255)
          .uv(u2, v2).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y1, z).color(r, g, b, 255)
          .uv(u2, v1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x1, y1, z).color(r, g, b, 255)
          .uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
    }
  }

  private void quadYZ(VertexConsumer consumer, Matrix4f matrix,
                      float x, float y1, float z1, float y2, float z2, float normalX,
                      int light, int overlay, int r, int g, int b) {
    if (normalX > 0) {
      consumer.vertex(matrix, x, y1, z2).color(r, g, b, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y1, z1).color(r, g, b, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y2, z1).color(r, g, b, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y2, z2).color(r, g, b, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
    } else {
      consumer.vertex(matrix, x, y1, z1).color(r, g, b, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y1, z2).color(r, g, b, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y2, z2).color(r, g, b, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
      consumer.vertex(matrix, x, y2, z1).color(r, g, b, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(normalX, 0, 0).endVertex();
    }
  }

  private void quadXZ(VertexConsumer consumer, Matrix4f matrix,
                      float x1, float x2, float y, float z1, float z2, float normalY,
                      int light, int overlay, int r, int g, int b) {
    if (normalY > 0) {
      consumer.vertex(matrix, x1, y, z1).color(r, g, b, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x1, y, z2).color(r, g, b, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x2, y, z2).color(r, g, b, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x2, y, z1).color(r, g, b, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
    } else {
      consumer.vertex(matrix, x1, y, z2).color(r, g, b, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x1, y, z1).color(r, g, b, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x2, y, z1).color(r, g, b, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
      consumer.vertex(matrix, x2, y, z2).color(r, g, b, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, normalY, 0).endVertex();
    }
  }

  @NotNull
  private BookColors colorsFor(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    ResourceLocation coverId = tag != null ? LinkOptions.getCoverItemId(tag) : null;
    if (!MystcraftConfig.proceduralBookCoversEnabled.get()) {
      coverId = null;
    }
    CoverPalette.Entry palette = CoverPalette.get(coverId);
    return new BookColors(palette.baseColor(), pageColorFor(palette));
  }

  private int pageColorFor(CoverPalette.Entry palette) {
    int trim = palette.trimColor();
    int r = (trim >>> 16) & 0xFF;
    int g = (trim >>> 8) & 0xFF;
    int b = trim & 0xFF;

    int pr = Math.min(255, (int) (r * 0.30 + 230 * 0.70));
    int pg = Math.min(255, (int) (g * 0.30 + 220 * 0.70));
    int pb = Math.min(255, (int) (b * 0.30 + 200 * 0.70));
    return (pr << 16) | (pg << 8) | pb;
  }

  private record BookColors(int leather, int page) {
  }
}
