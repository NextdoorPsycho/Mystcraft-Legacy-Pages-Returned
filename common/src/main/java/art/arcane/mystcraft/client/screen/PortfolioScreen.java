package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Portfolio item.
 * Shows 64 page slots (8 rows x 8 columns) plus player inventory.
 * Includes a SORT button - Portfolio's unique feature for organizing pages.
 */
public class PortfolioScreen extends AbstractContainerScreen<PortfolioMenu> {

  private Button sortButton;

  public PortfolioScreen(PortfolioMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 256; // Tall height for 8 rows of pages + player inventory
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = 8; // Left-align title to make room for sort button
    this.inventoryLabelY = 162; // Position "Inventory" label above player inv (imageHeight - 94)

    // Add SORT button - Portfolio's unique feature
    // Position it in the top-right area
    int buttonX = this.leftPos + this.imageWidth - 40;
    int buttonY = this.topPos + 4;
    sortButton = Button.builder(Component.literal("Sort"), this::onSortPressed)
        .bounds(buttonX, buttonY, 36, 12)
        .build();
    this.addRenderableWidget(sortButton);
  }

  @Override
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    // Container panel background + raised border
    ProceduralUI.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

    // Portfolio page slots (8 rows of 8)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 18, 8, 8, 0);

    // Player inventory (3 rows of 9)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 174, 9, 3, 0);

    // Hotbar (1 row of 9)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 232, 9, 1, 0);
  }


  @Override
  protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

    // Show page count (moved left to avoid sort button)
    int pageCount = PortfolioItem.getPageCount(menu.getPortfolioStack());
    String countText = pageCount + "/" + PortfolioItem.MAX_PAGES;
    int countWidth = this.font.width(countText);
    guiGraphics.drawString(this.font, countText, this.imageWidth - countWidth - 48, 6, 0x404040, false);
  }

  /**
   * Called when the Sort button is pressed.
   * Sends a packet to the server to sort pages by category/name.
   * This is Portfolio's unique feature - automatic organization.
   */
  private void onSortPressed(Button button) {
    // Send sort request to server
    MystcraftNetwork.sendToServer(new ContainerActionPacket(
        ContainerActionPacket.Action.PORTFOLIO_SORT,
        menu.containerId,
        false,
        ""
    ));
  }
}
