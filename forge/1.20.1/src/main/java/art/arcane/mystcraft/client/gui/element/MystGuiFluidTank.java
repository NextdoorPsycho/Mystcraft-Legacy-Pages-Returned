package art.arcane.mystcraft.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fluid tank display element for the Mystcraft GUI system (1.20.1 version).
 */
public class MystGuiFluidTank extends MystGuiElement {

  private final Supplier<Integer> amountProvider;
  private final Supplier<Integer> capacityProvider;
  private final int fluidColor;
  private final String fluidName;

  public MystGuiFluidTank(Supplier<Integer> amountProvider, Supplier<Integer> capacityProvider,
                          int fluidColor, String fluidName,
                          int left, int top, int width, int height) {
    super(left, top, width, height);
    this.amountProvider = amountProvider;
    this.capacityProvider = capacityProvider;
    this.fluidColor = fluidColor;
    this.fluidName = fluidName;
  }

  @Override
  protected void doRenderBackground(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    int x = getLeft();
    int y = getTop();

    int amount = amountProvider.get();
    int capacity = capacityProvider.get();

    // Draw tank background
    guiGraphics.fill(x, y, x + width, y + height, 0xFF202020);
    guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF101010);

    // Draw fluid fill
    if (capacity > 0 && amount > 0) {
      int fillHeight = (amount * (height - 2)) / capacity;
      fillHeight = Math.min(fillHeight, height - 2);
      int fillY = y + height - 1 - fillHeight;
      guiGraphics.fill(x + 1, fillY, x + width - 1, y + height - 1, fluidColor);
    }

    // Draw tank border
    guiGraphics.fill(x, y, x + width, y + 1, 0xFF404040);
    guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF404040);
    guiGraphics.fill(x, y, x + 1, y + height, 0xFF404040);
    guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF404040);
  }

  @Override
  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    int amount = amountProvider.get();
    int capacity = capacityProvider.get();
    return List.of(Component.literal(fluidName + ": " + amount + "/" + capacity + " mB"));
  }
}
