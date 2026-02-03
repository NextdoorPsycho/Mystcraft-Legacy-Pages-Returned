package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.ForgeMystcraftNetwork_1_18_2;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Portfolio item (1.19.2 version).
 * Shows 64 page slots (8 rows x 8 columns) plus player inventory.
 * Includes a SORT button - Portfolio's unique feature for organizing pages.
 */
public class PortfolioScreen extends AbstractContainerScreen<PortfolioMenu> {

  // Slot colors for programmatic rendering (matches vanilla Minecraft)
  private static final int SLOT_BORDER_DARK = 0xFF373737;
  private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;
  private static final int SLOT_BG = 0xFF8B8B8B;
  private static final int CONTAINER_BG = 0xFFC6C6C6;

  // Button colors
  private static final int BUTTON_BG = 0xFF555555;
  private static final int BUTTON_HOVER = 0xFF666666;

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
    // 1.19.2 API: Button constructor instead of builder
    sortButton = new Button(buttonX, buttonY, 36, 12, new TextComponent("Sort"), this::onSortPressed);
    this.addRenderableWidget(sortButton);
  }

  @Override
  protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    // Draw container background
    fill(poseStack, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, CONTAINER_BG);

    // Draw border (3D effect)
    fill(poseStack, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, SLOT_BORDER_LIGHT);
    fill(poseStack, this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.imageHeight, SLOT_BORDER_LIGHT);
    fill(poseStack, this.leftPos + this.imageWidth - 2, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, SLOT_BORDER_DARK);
    fill(poseStack, this.leftPos, this.topPos + this.imageHeight - 2, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, SLOT_BORDER_DARK);

    // Draw portfolio page slots (8 rows of 8) - matches PortfolioMenu slot positions
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        int x = this.leftPos + 8 + col * 18;
        int y = this.topPos + 18 + row * 18;
        drawSlot(poseStack, x, y);
      }
    }

    // Draw player inventory slots (3 rows of 9) - matches PortfolioMenu slot positions
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        int x = this.leftPos + 8 + col * 18;
        int y = this.topPos + 174 + row * 18;
        drawSlot(poseStack, x, y);
      }
    }

    // Draw hotbar slots - matches PortfolioMenu slot positions
    for (int col = 0; col < 9; col++) {
      int x = this.leftPos + 8 + col * 18;
      int y = this.topPos + 232;
      drawSlot(poseStack, x, y);
    }
  }

  /**
   * Draws a single slot background at the given position.
   * Mimics the standard Minecraft slot appearance with 3D border effect.
   */
  private void drawSlot(PoseStack poseStack, int x, int y) {
    // Draw slot border (3D inset effect)
    fill(poseStack, x, y, x + 18, y + 1, SLOT_BORDER_DARK);      // Top edge
    fill(poseStack, x, y, x + 1, y + 18, SLOT_BORDER_DARK);      // Left edge
    fill(poseStack, x + 17, y, x + 18, y + 18, SLOT_BORDER_LIGHT); // Right edge
    fill(poseStack, x, y + 17, x + 18, y + 18, SLOT_BORDER_LIGHT); // Bottom edge
    // Draw slot interior
    fill(poseStack, x + 1, y + 1, x + 17, y + 17, SLOT_BG);
  }

  @Override
  public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    // 1.19.2 API: renderBackground takes only PoseStack
    this.renderBackground(poseStack);
    super.render(poseStack, mouseX, mouseY, partialTick);
    this.renderTooltip(poseStack, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
    this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752);

    // Show page count (moved left to avoid sort button)
    int pageCount = PortfolioItem.getPageCount(menu.getPortfolioStack());
    String countText = pageCount + "/" + PortfolioItem.MAX_PAGES;
    int countWidth = this.font.width(countText);
    this.font.draw(poseStack, countText, this.imageWidth - countWidth - 48, 6, 0x404040);
  }

  /**
   * Called when the Sort button is pressed.
   * Sends a packet to the server to sort pages by category/name.
   * This is Portfolio's unique feature - automatic organization.
   */
  private void onSortPressed(Button button) {
    // Send sort request to server
    ForgeMystcraftNetwork_1_18_2.sendToServer(new ContainerActionPacket(
        ContainerActionPacket.Action.PORTFOLIO_SORT,
        menu.containerId,
        false,
        ""
    ));
  }
}
