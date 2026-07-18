package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.client.gui.procedural.GuiTheme;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Screen for the Ink Mixer block. Click on the basin area while holding an item
 * to add it to the ink.
 */
public class InkMixerScreen extends AbstractContainerScreen<InkMixerMenu> {

  private static final int BASIN_CENTER_X = 88;
  private static final int BASIN_CENTER_Y = 49;
  private static final int BASIN_RADIUS = 30;
  private static final int BASIN_RADIUS_SQ = BASIN_RADIUS * BASIN_RADIUS;

  private int frame = 0;

  public InkMixerScreen(InkMixerMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title, 176, 181);
  }

  private static void addAffinitySection(List<Component> out, InkBlend blend) {
    record Row(String label, float weight, ChatFormatting color) {
    }
    List<Row> rows = new ArrayList<>();

    for (Map.Entry<Identifier, Float> e : blend.symbolWeights().entrySet()) {
      String key = e.getKey().getNamespace().equals("minecraft")
          ? e.getKey().getPath() : e.getKey().toString();
      rows.add(new Row(prettify(key), e.getValue(), ChatFormatting.WHITE));
    }
    for (Map.Entry<SymbolCategory, Float> e : blend.categoryWeights().entrySet()) {
      rows.add(new Row(prettify(e.getKey().getName()) + " (category)",
          e.getValue(), ChatFormatting.GRAY));
    }
    for (Map.Entry<String, Float> e : blend.poemTokenWeights().entrySet()) {
      rows.add(new Row("\"" + capitaliseWord(e.getKey()) + "\" (theme)",
          e.getValue(), ChatFormatting.DARK_AQUA));
    }
    rows.sort(Comparator.<Row, Float>comparing(r -> r.weight).reversed());
    int max = Math.min(3, rows.size());
    for (int i = 0; i < max; i++) {
      Row r = rows.get(i);
      String pct = String.format("%.0f%%", Math.min(100f, r.weight * 100f));
      out.add(Component.literal("  " + r.label + ": " + pct).withStyle(r.color));
    }
    if (rows.size() > max) {
      out.add(Component.literal("  +" + (rows.size() - max) + " more")
          .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
  }

  private static String prettify(String raw) {
    String snake = raw.replace('_', ' ').trim();
    StringBuilder sb = new StringBuilder(snake.length());
    boolean cap = true;
    for (int i = 0; i < snake.length(); i++) {
      char c = snake.charAt(i);
      if (cap && Character.isLetter(c)) {
        sb.append(Character.toUpperCase(c));
        cap = false;
      } else if (c == ' ') {
        sb.append(c);
        cap = true;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String capitaliseWord(String word) {
    if (word == null || word.isEmpty()) return "";
    return Character.toUpperCase(word.charAt(0)) + word.substring(1);
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
  public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                float partialTick) {
    super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
    int x = this.leftPos;
    int y = this.topPos;

    ProceduralUI.drawPanel(guiGraphics, x, y, this.imageWidth, this.imageHeight);

    ProceduralUI.drawSlot(guiGraphics, x + 7, y + 26);
    ProceduralUI.drawSlot(guiGraphics, x + 7, y + 47);
    ProceduralUI.drawSlot(guiGraphics, x + 151, y + 26);
    ProceduralUI.drawSlot(guiGraphics, x + 151, y + 47);

    ProceduralUI.drawSlotGrid(guiGraphics, x + 7, y + 98, 9, 3, 0);
    ProceduralUI.drawSlotGrid(guiGraphics, x + 7, y + 156, 9, 1, 0);

    ProceduralUI.drawBasin(guiGraphics, x + BASIN_CENTER_X, y + BASIN_CENTER_Y, BASIN_RADIUS);

    if (menu.hasInk()) {
      renderInkTank(guiGraphics, x + BASIN_CENTER_X, y + BASIN_CENTER_Y, BASIN_RADIUS - 2);
    }
  }

  private void renderInkTank(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius) {
    int baseColor = GuiTheme.color("ink_base");
    ProceduralUI.drawDisc(guiGraphics, centerX, centerY, radius, baseColor);

    Map<String, Float> probabilities = menu.getInkProbabilities();
    if (probabilities != null && !probabilities.isEmpty()) {

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

        totalR /= totalWeight;
        totalG /= totalWeight;
        totalB /= totalWeight;

        float pulse = (float) (0.5 + 0.5 * Math.sin(frame / 20.0));
        float intensity = 0.3f + 0.4f * pulse;

        int r = (int) (totalR * 255 * intensity) & 0xFF;
        int g = (int) (totalG * 255 * intensity) & 0xFF;
        int b = (int) (totalB * 255 * intensity) & 0xFF;

        int topColor = (0x40 << 24) | (r << 16) | (g << 8) | b;
        int bottomColor = (0xB0 << 24) | (r << 16) | (g << 8) | b;
        int midY = centerY;
        for (int dy = -radius; dy <= radius; dy++) {
          int span = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
          int color = dy < 0 ? topColor : bottomColor;
          guiGraphics.fill(centerX - span, midY + dy, centerX + span + 1, midY + dy + 1, color);
        }

        renderColorBall(guiGraphics, centerX, centerY, 20, totalR, totalG, totalB, pulse);
      }
    }
  }

  private void renderColorBall(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, float radius,
                               float r, float g, float b, float pulse) {

    float animRadius = radius * (0.7f + 0.3f * pulse);

    int red = Math.max(0, Math.min(255, Math.round(r * 255.0F)));
    int green = Math.max(0, Math.min(255, Math.round(g * 255.0F)));
    int blue = Math.max(0, Math.min(255, Math.round(b * 255.0F)));
    int maxRadius = Math.max(1, Math.round(animRadius));
    for (int currentRadius = maxRadius; currentRadius > 0; currentRadius--) {
      float centerFactor = 1.0F - currentRadius / (float) maxRadius;
      int alpha = Math.round(pulse * 204.0F * centerFactor);
      int color = alpha << 24 | red << 16 | green << 8 | blue;
      ProceduralUI.drawDisc(guiGraphics, centerX, centerY, currentRadius, color);
    }
  }

  @Override
  public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                 float partialTick) {
    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

    if (menu.hasInk()) {
      int relX = mouseX - this.leftPos;
      int relY = mouseY - this.topPos;
      if (isInBasin(relX, relY)) {
        renderInkTooltip(guiGraphics, mouseX, mouseY);
      }
    }
  }

  private void renderInkTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    InkBlend blend = menu.getBlend();
    Map<String, Float> probabilities = menu.getInkProbabilities();
    boolean hasProperties = probabilities != null && !probabilities.isEmpty();
    boolean hasAffinity = blend != null && !blend.isEmpty();
    if (!hasProperties && !hasAffinity) {
      return;
    }

    List<Component> tooltip = new ArrayList<>();

    if (hasProperties) {
      tooltip.add(Component.literal("Link Properties").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      List<Map.Entry<String, Float>> sortedProps = new ArrayList<>(probabilities.entrySet());
      sortedProps.sort(Comparator.<Map.Entry<String, Float>, Float>comparing(Map.Entry::getValue).reversed());
      for (Map.Entry<String, Float> entry : sortedProps) {
        String name = InkEffects.getLocalizedName(entry.getKey());
        int percent = Math.round(entry.getValue() * 100f);
        int color = InkEffects.getPropertyColorRGB(entry.getKey());
        tooltip.add(Component.literal("  " + name + ": " + percent + "%")
            .withStyle(style -> style.withColor(color)));
      }
    }

    if (hasAffinity) {
      List<Component> affinityLines = new ArrayList<>();
      addAffinitySection(affinityLines, blend);
      if (!affinityLines.isEmpty()) {
        if (!tooltip.isEmpty()) tooltip.add(Component.empty());
        tooltip.add(Component.literal("Symbol Affinity").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.addAll(affinityLines);
      }

      int tier = blend.tierBonus();
      if (tier > 0) {
        if (!tooltip.isEmpty()) tooltip.add(Component.empty());
        tooltip.add(Component.literal("Rarity Boost: +" + tier)
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
      }
    }

    if (!tooltip.isEmpty()) {
      guiGraphics.setTooltipForNextFrame(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
    }
  }

  @Override
  protected void extractLabels(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    int textColor = GuiTheme.color("text_primary") & 0x00FFFFFF;
    guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, textColor, false);
    guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, textColor, false);

    if (menu.hasInk() && !menu.getCarried().isEmpty()) {
      int relX = mouseX - this.leftPos;
      int relY = mouseY - this.topPos;
      if (isInBasin(relX, relY)) {
        if (InkEffects.hasEffects(menu.getCarried())) {
          guiGraphics.text(this.font, Component.literal("Click to add"), 55, 75, GuiTheme.color("text_ok") & 0x00FFFFFF, false);
        } else {
          guiGraphics.text(this.font, Component.literal("No ink effect"), 55, 75, GuiTheme.color("text_warning") & 0x00FFFFFF, false);
        }
      }
    }
  }

  @Override
  public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
    double mouseX = event.x();
    double mouseY = event.y();
    int button = event.button();

    int relX = (int) mouseX - this.leftPos;
    int relY = (int) mouseY - this.topPos;

    if (isInBasin(relX, relY)) {

      if (!menu.getCarried().isEmpty() && menu.hasInk()) {

        boolean rightClick = (button == 1);
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.INK_MIXER_ADD_ITEM,
            menu.containerId,
            rightClick
        ));
        return true;
      }
    }

    return super.mouseClicked(event, doubleClick);
  }

  private boolean isInBasin(int relX, int relY) {
    int dx = relX - BASIN_CENTER_X;
    int dy = relY - BASIN_CENTER_Y;
    return dx * dx + dy * dy < BASIN_RADIUS_SQ;
  }
}
