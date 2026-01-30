package art.arcane.mystcraft.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Toggle button GUI element for the Mystcraft GUI system (1.20.1 version).
 */
public class MystGuiToggleButton extends MystGuiElement {

  private final String id;
  private final Supplier<Boolean> stateProvider;
  private final Consumer<MystGuiToggleButton> onClick;
  private String text = "";
  private Color color = null;

  public MystGuiToggleButton(String id, Supplier<Boolean> stateProvider, Consumer<MystGuiToggleButton> onClick,
                             int left, int top, int width, int height) {
    super(left, top, width, height);
    this.id = id;
    this.stateProvider = stateProvider;
    this.onClick = onClick;
  }

  public MystGuiToggleButton(Supplier<Boolean> stateProvider, Consumer<MystGuiToggleButton> onClick,
                             String id, List<Component> tooltip,
                             int left, int top, int width, int height) {
    super(left, top, width, height);
    this.id = id;
    this.stateProvider = stateProvider;
    this.onClick = onClick;
    this.tooltip = tooltip;
  }

  public String getId() {
    return id;
  }

  public void setText(String text) {
    this.text = text != null ? text : "";
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public boolean getState() {
    return stateProvider != null && stateProvider.get();
  }

  @Override
  protected void doRenderBackground(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    int x = getLeft();
    int y = getTop();
    boolean state = getState();
    boolean hovered = contains(mouseX, mouseY);

    // Background color based on state
    int bgColor;
    if (color != null) {
      int baseColor = color.getRGB() & 0x00FFFFFF;
      bgColor = state ? (0xFF000000 | baseColor) : 0xFF404040;
    } else {
      bgColor = state ? 0xFF4040A0 : 0xFF404040;
    }

    if (hovered) {
      bgColor = brighten(bgColor);
    }

    // Draw button
    guiGraphics.fill(x, y, x + width, y + height, 0xFF000000);
    guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);

    // Draw border
    int borderColor = state ? 0xFF8080FF : 0xFF606060;
    guiGraphics.fill(x, y, x + width, y + 1, borderColor);
    guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
    guiGraphics.fill(x, y, x + 1, y + height, borderColor);
    guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

    // Draw text centered
    if (!text.isEmpty()) {
      int textWidth = mc.font.width(text);
      int textX = x + (width - textWidth) / 2;
      int textY = y + (height - 8) / 2;
      int textColor = state ? 0xFFFFFF : 0xC0C0C0;
      guiGraphics.drawString(mc.font, text, textX, textY, textColor);
    }
  }

  private int brighten(int color) {
    int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
    int g = Math.min(255, ((color >> 8) & 0xFF) + 30);
    int b = Math.min(255, (color & 0xFF) + 30);
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  @Override
  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    if (onClick != null) {
      onClick.accept(this);
      return true;
    }
    return false;
  }

  @Override
  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    return tooltip;
  }
}
