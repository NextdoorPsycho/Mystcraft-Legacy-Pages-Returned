package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.procedural.BookItemTextureFactory;
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
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.2 special-model renderer for procedural Mystcraft books.
 *
 * <p>The historical BEWLR name is retained as a source-compatible handoff for
 * loader registration, while the implementation uses Minecraft's extracted
 * special-model pipeline. A book is submitted as a page block, two leather
 * covers, a spine, and the stack-specific procedural cover texture.</p>
 */
public final class BookItemRendererBEWLR
    implements SpecialModelRenderer<BookItemRendererBEWLR.RenderData> {

  private static final float MAX_OPEN_DEGREES = 160.0F;
  private static final float OPEN_LERP_PER_SECOND = 2.5F;
  private static final float MAX_DT_SECONDS = 0.1F;
  private static final float BOOK_W = 0.84F;
  private static final float BOOK_H = 1.00F;
  private static final float BOOK_D = 0.18F;
  private static final float COVER_T = 0.020F;
  private static final float OVERHANG = 0.030F;
  private static final float X0 = 0.5F - BOOK_W / 2.0F;
  private static final float X1 = 0.5F + BOOK_W / 2.0F;
  private static final float Y0 = 0.5F - BOOK_H / 2.0F;
  private static final float Y1 = 0.5F + BOOK_H / 2.0F;
  private static final float Z0 = 0.5F - BOOK_D / 2.0F;
  private static final float Z1 = 0.5F + BOOK_D / 2.0F;
  private static final float PAGE_X0 = X0 + COVER_T;
  private static final float PAGE_X1 = X1 - OVERHANG;
  private static final float PAGE_Y0 = Y0 + OVERHANG;
  private static final float PAGE_Y1 = Y1 - OVERHANG;
  private static final float PAGE_Z0 = Z0 + COVER_T;
  private static final float PAGE_Z1 = Z1 - COVER_T;
  private static final float COVER_U_LEFT = 14.5F / 64.0F;
  private static final float COVER_U_RIGHT = 57.5F / 64.0F;
  private static final float COVER_V_TOP = 6.5F / 64.0F;
  private static final float COVER_V_BOTTOM = 57.5F / 64.0F;
  private static final float EPSILON = 0.002F;
  private static final BookItemRendererBEWLR INSTANCE = new BookItemRendererBEWLR();

  private static Identifier whiteTextureLocation;
  private static volatile float openTarget;
  private static float openAmount;
  private static long lastTickNanos;

  private BookItemRendererBEWLR() {
  }

  public static BookItemRendererBEWLR getInstance() {
    return INSTANCE;
  }

  public static void clearCache() {
    BookItemTextureFactory.reset();
  }

  /** Sets the target for the first-person cover-opening animation. */
  public static void setHeldBookOpen(boolean open) {
    openTarget = open ? 1.0F : 0.0F;
  }

  private static void tickOpenAnimation() {
    long now = System.nanoTime();
    if (lastTickNanos == 0L) {
      lastTickNanos = now;
      return;
    }

    float deltaSeconds = Math.min(
        MAX_DT_SECONDS, (now - lastTickNanos) / 1_000_000_000.0F);
    lastTickNanos = now;
    float step = OPEN_LERP_PER_SECOND * deltaSeconds;
    if (openAmount < openTarget) {
      openAmount = Math.min(openTarget, openAmount + step);
    } else if (openAmount > openTarget) {
      openAmount = Math.max(openTarget, openAmount - step);
    }
  }

  private static synchronized Identifier whiteTexture() {
    if (whiteTextureLocation != null) {
      return whiteTextureLocation;
    }

    NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, true);
    image.setPixelABGR(0, 0, 0xFFFFFFFF);
    DynamicTexture texture = new DynamicTexture(
        () -> "Mystcraft solid-white book texture", image);
    whiteTextureLocation =
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "book_solid_white");
    Minecraft.getInstance().getTextureManager().register(whiteTextureLocation, texture);
    return whiteTextureLocation;
  }

  @Override
  public @Nullable RenderData extractArgument(ItemStack stack) {
    if (!isRenderedBook(stack)) {
      return null;
    }

    CoverPalette.Entry palette = paletteFor(stack);
    return new RenderData(
        BookItemTextureFactory.getItemTexture(stack),
        palette.baseColor(),
        pageColorFor(palette));
  }

  private static boolean isRenderedBook(ItemStack stack) {
    return stack.getItem() instanceof AgebookItem
        || stack.getItem() instanceof LinkbookItem
        || stack.getItem() instanceof LinkbookUnlinkedItem
        || stack.getItem() instanceof GuidebookItem;
  }

  @Override
  public void submit(@Nullable RenderData data, PoseStack poseStack,
                     SubmitNodeCollector submitNodeCollector, int lightCoords,
                     int overlayCoords, boolean hasFoil, int outlineColor) {
    if (data == null) {
      return;
    }

    tickOpenAnimation();
    submitBook(data, poseStack, submitNodeCollector, lightCoords, overlayCoords, hasFoil);
  }

  private static void submitBook(RenderData data, PoseStack poseStack,
                                 SubmitNodeCollector submitNodeCollector,
                                 int lightCoords, int overlayCoords, boolean hasFoil) {
    int leather = opaque(data.leatherColor());
    int pages = opaque(data.pageColor());
    int band = darken(leather, 30);

    // Back cover remains fixed.
    submitSolidBox(poseStack, submitNodeCollector,
        X0, Y0, Z0, X1, Y1, Z0 + COVER_T,
        leather, lightCoords, overlayCoords);
    submitNodeCollector.submitCustomGeometry(
        poseStack,
        RenderTypes.entityCutout(data.coverTexture()),
        (pose, consumer) -> quadXY(
            consumer, pose, X1, Y0, X0, Y1, Z0 - EPSILON,
            COVER_U_LEFT, COVER_V_BOTTOM, COVER_U_RIGHT, COVER_V_TOP,
            -1.0F, lightCoords, overlayCoords, -1));

    // Page block and spine are independent of the animated front cover.
    submitSolidBox(poseStack, submitNodeCollector,
        PAGE_X0, PAGE_Y0, PAGE_Z0, PAGE_X1, PAGE_Y1, PAGE_Z1,
        pages, lightCoords, overlayCoords);
    submitSolidBox(poseStack, submitNodeCollector,
        X0 - 0.02F, Y0, Z0, PAGE_X0, Y1, Z1,
        band, lightCoords, overlayCoords);
    submitSpineBands(
        poseStack, submitNodeCollector, band, lightCoords, overlayCoords);

    // The open front cover and its leather thickness share one transformed
    // pose so the hinge remains visually coherent.
    poseStack.pushPose();
    if (openAmount > 0.001F) {
      poseStack.translate(X0, 0.0F, Z1);
      poseStack.mulPose(Axis.YP.rotationDegrees(-openAmount * MAX_OPEN_DEGREES));
      poseStack.translate(-X0, 0.0F, -Z1);
    }
    submitSolidBox(poseStack, submitNodeCollector,
        X0, Y0, Z1 - COVER_T, X1, Y1, Z1,
        leather, lightCoords, overlayCoords);
    submitNodeCollector.submitCustomGeometry(
        poseStack,
        RenderTypes.entityCutout(data.coverTexture()),
        (pose, consumer) -> quadXY(
            consumer, pose, X0, Y0, X1, Y1, Z1 + EPSILON,
            COVER_U_LEFT, COVER_V_BOTTOM, COVER_U_RIGHT, COVER_V_TOP,
            1.0F, lightCoords, overlayCoords, -1));
    if (openAmount > 0.001F) {
      submitNodeCollector.submitCustomGeometry(
          poseStack,
          RenderTypes.entityCutout(whiteTexture()),
          (pose, consumer) -> quadXY(
              consumer, pose, X0, Y0, X1, Y1, Z1 - COVER_T - EPSILON,
              0, 0, 1, 1, -1.0F, lightCoords, overlayCoords, pages));
    }
    if (hasFoil) {
      submitNodeCollector.submitCustomGeometry(
          poseStack,
          RenderTypes.entityGlint(),
          (pose, consumer) -> quadXY(
              consumer, pose, X0, Y0, X1, Y1, Z1 + EPSILON * 2.0F,
              0, 0, 1, 1, 1.0F, lightCoords, overlayCoords, -1));
    }
    poseStack.popPose();

    // A dark inset on the visible page reads as the linking panel when the
    // cover animates open.
    if (openAmount > 0.001F) {
      submitNodeCollector.submitCustomGeometry(
          poseStack,
          RenderTypes.entityCutout(whiteTexture()),
          (pose, consumer) -> quadXY(
              consumer,
              pose,
              PAGE_X0 + 0.06F,
              PAGE_Y1 - 0.50F,
              PAGE_X1 - 0.08F,
              PAGE_Y1 - 0.10F,
              PAGE_Z1 + EPSILON,
              0, 0, 1, 1, 1.0F,
              lightCoords, overlayCoords, 0xFF000000));
    }
  }

  private static void submitSpineBands(PoseStack poseStack,
                                       SubmitNodeCollector submitNodeCollector,
                                       int color, int lightCoords, int overlayCoords) {
    float[] bandPositions = {0.20F, 0.40F, 0.60F, 0.80F};
    for (int index = 0; index < bandPositions.length; index++) {
      float centerY = Y0 + (Y1 - Y0) * bandPositions[index];
      float halfHeight = index == 1 ? 0.060F : 0.025F;
      submitSolidBox(
          poseStack,
          submitNodeCollector,
          X0 - 0.04F,
          centerY - halfHeight,
          Z0 - EPSILON,
          X0 + COVER_T,
          centerY + halfHeight,
          Z1 + EPSILON,
          color,
          lightCoords,
          overlayCoords);
    }
  }

  private static void submitSolidBox(PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     int color, int lightCoords, int overlayCoords) {
    submitNodeCollector.submitCustomGeometry(
        poseStack,
        RenderTypes.entityCutout(whiteTexture()),
        (pose, consumer) -> emitBox(
            consumer, pose, minX, minY, minZ, maxX, maxY, maxZ,
            color, lightCoords, overlayCoords));
  }

  private static void emitBox(VertexConsumer consumer, PoseStack.Pose pose,
                              float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ,
                              int color, int lightCoords, int overlayCoords) {
    quadXY(consumer, pose, minX, minY, maxX, maxY, maxZ,
        0, 1, 1, 0, 1, lightCoords, overlayCoords, color);
    quadXY(consumer, pose, maxX, minY, minX, maxY, minZ,
        0, 1, 1, 0, -1, lightCoords, overlayCoords, color);
    quadYZ(consumer, pose, minX, minY, minZ, maxY, maxZ,
        -1, lightCoords, overlayCoords, color);
    quadYZ(consumer, pose, maxX, minY, maxZ, maxY, minZ,
        1, lightCoords, overlayCoords, color);
    quadXZ(consumer, pose, minX, maxX, maxY, minZ, maxZ,
        1, lightCoords, overlayCoords, color);
    quadXZ(consumer, pose, minX, maxX, minY, maxZ, minZ,
        -1, lightCoords, overlayCoords, color);
  }

  private static void quadXY(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float y1, float x2, float y2, float z,
                             float u1, float v1, float u2, float v2, float normalZ,
                             int lightCoords, int overlayCoords, int color) {
    vertex(consumer, pose, x1, y1, z, u1, v1,
        0, 0, normalZ, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x2, y1, z, u2, v1,
        0, 0, normalZ, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x2, y2, z, u2, v2,
        0, 0, normalZ, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x1, y2, z, u1, v2,
        0, 0, normalZ, color, lightCoords, overlayCoords);
  }

  private static void quadYZ(VertexConsumer consumer, PoseStack.Pose pose,
                             float x, float y1, float z1, float y2, float z2,
                             float normalX, int lightCoords, int overlayCoords, int color) {
    vertex(consumer, pose, x, y1, z1, 0, 1,
        normalX, 0, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x, y1, z2, 1, 1,
        normalX, 0, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x, y2, z2, 1, 0,
        normalX, 0, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x, y2, z1, 0, 0,
        normalX, 0, 0, color, lightCoords, overlayCoords);
  }

  private static void quadXZ(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float x2, float y, float z1, float z2,
                             float normalY, int lightCoords, int overlayCoords, int color) {
    vertex(consumer, pose, x1, y, z1, 0, 0,
        0, normalY, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x1, y, z2, 0, 1,
        0, normalY, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x2, y, z2, 1, 1,
        0, normalY, 0, color, lightCoords, overlayCoords);
    vertex(consumer, pose, x2, y, z1, 1, 0,
        0, normalY, 0, color, lightCoords, overlayCoords);
  }

  private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                             float x, float y, float z, float u, float v,
                             float normalX, float normalY, float normalZ,
                             int color, int lightCoords, int overlayCoords) {
    consumer.addVertex(pose, x, y, z)
        .setColor(color)
        .setUv(u, v)
        .setOverlay(overlayCoords)
        .setLight(lightCoords)
        .setNormal(pose, normalX, normalY, normalZ);
  }

  private static int opaque(int rgb) {
    return 0xFF000000 | rgb & 0x00FFFFFF;
  }

  private static int darken(int argb, int amount) {
    int red = Math.max(0, (argb >>> 16 & 0xFF) - amount);
    int green = Math.max(0, (argb >>> 8 & 0xFF) - amount);
    int blue = Math.max(0, (argb & 0xFF) - amount);
    return 0xFF000000 | red << 16 | green << 8 | blue;
  }

  private static CoverPalette.Entry paletteFor(ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    Identifier coverId = tag != null ? LinkOptions.getCoverItemId(tag) : null;
    if (!MystcraftConfig.proceduralBookCoversEnabled.get()) {
      coverId = null;
    }
    return CoverPalette.get(coverId);
  }

  private static int pageColorFor(CoverPalette.Entry palette) {
    int trim = palette.trimColor();
    int red = trim >>> 16 & 0xFF;
    int green = trim >>> 8 & 0xFF;
    int blue = trim & 0xFF;
    int pageRed = Math.min(255, (int) (red * 0.30 + 230 * 0.70));
    int pageGreen = Math.min(255, (int) (green * 0.30 + 220 * 0.70));
    int pageBlue = Math.min(255, (int) (blue * 0.30 + 200 * 0.70));
    return pageRed << 16 | pageGreen << 8 | pageBlue;
  }

  @Override
  public void getExtents(Consumer<Vector3fc> output) {
    // Include the full hinge swing so held books are never clipped mid-open.
    float minX = X0 - BOOK_W;
    float maxX = X1;
    float minZ = Z0 - BOOK_W * 0.4F;
    float maxZ = Z1 + BOOK_W * 0.4F;
    output.accept(new Vector3f(minX, Y0, minZ));
    output.accept(new Vector3f(minX, Y0, maxZ));
    output.accept(new Vector3f(minX, Y1, minZ));
    output.accept(new Vector3f(minX, Y1, maxZ));
    output.accept(new Vector3f(maxX, Y0, minZ));
    output.accept(new Vector3f(maxX, Y0, maxZ));
    output.accept(new Vector3f(maxX, Y1, minZ));
    output.accept(new Vector3f(maxX, Y1, maxZ));
  }

  public record RenderData(Identifier coverTexture, int leatherColor, int pageColor) {
  }
}
