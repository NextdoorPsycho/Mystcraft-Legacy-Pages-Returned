package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import com.floopowder.screen.FlooContainerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screen for the Ink Mixer block.
 * Click on the basin area while holding an item to add it to the ink.
 */
public class InkMixerScreen extends FlooContainerScreen<InkMixerMenu> {

  private static final ResourceLocation TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "gui/inkmixer.png");

  // Basin area - center of the GUI where ink is displayed
  // Radius check: x*x + y*y < 900 (radius 30) around center (88, 49)
  private static final int BASIN_CENTER_X = 88;
  private static final int BASIN_CENTER_Y = 49;
  private static final int BASIN_RADIUS_SQ = 900; // 30^2
  private static final int BASIN_LEFT = 54;
  private static final int BASIN_TOP = 16;
  private static final int BASIN_WIDTH = 66;
  private static final int BASIN_HEIGHT = 65;

  // Animation frame counter
  private int frame = 0;

  public InkMixerScreen(InkMixerMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 181;
  }

  @Override
  protected void init() {
    super.init();
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  public void containerTick() {
    super.containerTick();
    frame++;
  }

  @Override
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    int x = this.leftPos;
    int y = this.topPos;

    // Draw basin background area (from texture at 179,16)
    guiGraphics.blit(TEXTURE, x + BASIN_LEFT, y + BASIN_TOP, 179, 16, BASIN_WIDTH, BASIN_HEIGHT);

    // Draw ink fill if mixer has ink
    if (menu.hasInk()) {
      renderInkTank(guiGraphics, x + BASIN_LEFT, y + BASIN_TOP, BASIN_WIDTH, BASIN_HEIGHT);
    }

    // Draw main GUI texture on top (has transparent basin area)
    guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
  }

  /**
   * Renders the ink tank with animated color gradient based on ink properties.
   */
  private void renderInkTank(GuiGraphics guiGraphics, int left, int top, int width, int height) {
    // Base ink color (dark blue/black)
    int baseColor = 0xFF101030;
    guiGraphics.fill(left, top, left + width, top + height, baseColor);

    // Get ink probabilities for color overlay
    Map<String, Float> probabilities = menu.getInkProbabilities();
    if (probabilities != null && !probabilities.isEmpty()) {
      // Calculate blended color from property probabilities
      float totalR = 0, totalG = 0, totalB = 0;
      float totalWeight = 0;

      List<Map.Entry<String, Float>> entries = new ArrayList<>(probabilities.entrySet());
      for (Map.Entry<String, Float> entry : entries) {
        InkEffects.PropertyColor color = InkEffects.getPropertyColor(entry.getKey());
        if (color != null) {
          float weight = entry.getValue();
          totalR += color.r() * weight;
          totalG += color.g() * weight;
          totalB += color.b() * weight;
          totalWeight += weight;
        }
      }

      if (totalWeight > 0) {
        // Normalize colors
        totalR /= totalWeight;
        totalG /= totalWeight;
        totalB /= totalWeight;

        // Animate with pulsing effect
        float pulse = (float) (0.5 + 0.5 * Math.sin(frame / 20.0));
        float intensity = 0.3f + 0.4f * pulse;

        int r = (int) (totalR * 255 * intensity) & 0xFF;
        int g = (int) (totalG * 255 * intensity) & 0xFF;
        int b = (int) (totalB * 255 * intensity) & 0xFF;

        // Draw color gradient overlay
        int topColor = (0x40 << 24) | (r << 16) | (g << 8) | b;
        int bottomColor = (0xB0 << 24) | (r << 16) | (g << 8) | b;
        guiGraphics.fillGradient(left, top, left + width, top + height, topColor, bottomColor);

        // Draw animated color ball in center
        renderColorBall(guiGraphics, left + width / 2, top + height / 2, 20, totalR, totalG, totalB, pulse);
      }
    }
  }

  /**
   * Renders an animated color "ball" effect in the center of the ink.
   * Animated D'ni color renderer effect.
   */
  private void renderColorBall(GuiGraphics guiGraphics, int centerX, int centerY, float radius,
                               float r, float g, float b, float pulse) {
    // Animated radius
    float animRadius = radius * (0.7f + 0.3f * pulse);

    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    Matrix4f matrix = guiGraphics.pose().last().pose();
    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

    // Center vertex (brightest)
    float centerAlpha = pulse * 0.8f;
    buffer.vertex(matrix, centerX, centerY, 0)
        .color(r, g, b, centerAlpha)
        .endVertex();

    // Outer vertices (transparent)
    int segments = 16;
    for (int i = 0; i <= segments; i++) {
      double angle = (2 * Math.PI * i) / segments;
      float px = centerX + (float) Math.cos(angle) * animRadius;
      float py = centerY + (float) Math.sin(angle) * animRadius;
      buffer.vertex(matrix, px, py, 0)
          .color(r, g, b, 0f)
          .endVertex();
    }

    tesselator.end();
    RenderSystem.disableBlend();
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);

    // Render property tooltips when hovering over basin
    if (menu.hasInk()) {
      int relX = mouseX - this.leftPos;
      int relY = mouseY - this.topPos;
      if (isInBasin(relX, relY)) {
        renderInkTooltip(guiGraphics, mouseX, mouseY);
      }
    }
  }

  /**
   * Renders a tooltip showing current ink properties.
   */
  private void renderInkTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    Map<String, Float> probabilities = menu.getInkProbabilities();
    if (probabilities == null || probabilities.isEmpty()) {
      return;
    }

    List<Component> tooltip = new ArrayList<>();
    tooltip.add(Component.literal("Ink Properties:"));

    for (Map.Entry<String, Float> entry : probabilities.entrySet()) {
      String name = InkEffects.getLocalizedName(entry.getKey());
      int percent = (int) (entry.getValue() * 100);
      int color = InkEffects.getPropertyColorRGB(entry.getKey());
      tooltip.add(Component.literal("  " + name + ": " + percent + "%")
          .withStyle(style -> style.withColor(color)));
    }

    guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
  }

  @Override
  protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    // Only render inventory label (title is baked into texture)
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

    // Show "Click to add" hint when hovering over basin with item
    if (menu.hasInk() && !menu.getCarried().isEmpty()) {
      int relX = mouseX - this.leftPos;
      int relY = mouseY - this.topPos;
      if (isInBasin(relX, relY)) {
        if (InkEffects.hasEffects(menu.getCarried())) {
          guiGraphics.drawString(this.font, Component.literal("Click to add"), 55, 75, 0x40FF40, false);
        } else {
          guiGraphics.drawString(this.font, Component.literal("No ink effect"), 55, 75, 0xFF4040, false);
        }
      }
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Check if clicking on basin area
    int relX = (int) mouseX - this.leftPos;
    int relY = (int) mouseY - this.topPos;

    if (isInBasin(relX, relY)) {
      // Only process if player is holding an item and ink is present
      if (!menu.getCarried().isEmpty() && menu.hasInk()) {
        // Send packet to server to consume item
        boolean rightClick = (button == 1);
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.INK_MIXER_ADD_ITEM,
            menu.containerId,
            rightClick
        ));
        return true;
      }
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  /**
   * Checks if the given position (relative to GUI) is within the basin area.
   */
  private boolean isInBasin(int relX, int relY) {
    int dx = relX - BASIN_CENTER_X;
    int dy = relY - BASIN_CENTER_Y;
    return dx * dx + dy * dy < BASIN_RADIUS_SQ;
  }
}
