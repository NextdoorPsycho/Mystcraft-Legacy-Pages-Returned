package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.FolderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Folder item (1.19.2 version).
 * Shows 16 page slots (2 rows x 8 columns) plus player inventory.
 * Renders slots programmatically with standard Minecraft styling.
 */
public class FolderScreen extends AbstractContainerScreen<FolderMenu> {

  // Slot colors for programmatic rendering (matches vanilla Minecraft)
  private static final int SLOT_BORDER_DARK = 0xFF373737;
  private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;
  private static final int SLOT_BG = 0xFF8B8B8B;
  private static final int CONTAINER_BG = 0xFFC6C6C6;

  public FolderScreen(FolderMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 154; // Compact height for 2 rows of pages + player inventory
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    this.inventoryLabelY = 60; // Position "Inventory" label above player inv
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

    // Draw folder page slots (2 rows of 8) - matches FolderMenu slot positions
    for (int row = 0; row < 2; row++) {
      for (int col = 0; col < 8; col++) {
        int x = this.leftPos + 17 + col * 18;
        int y = this.topPos + 18 + row * 18;
        drawSlot(poseStack, x, y);
      }
    }

    // Draw player inventory slots (3 rows of 9) - matches FolderMenu slot positions
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        int x = this.leftPos + 8 + col * 18;
        int y = this.topPos + 72 + row * 18;
        drawSlot(poseStack, x, y);
      }
    }

    // Draw hotbar slots - matches FolderMenu slot positions
    for (int col = 0; col < 9; col++) {
      int x = this.leftPos + 8 + col * 18;
      int y = this.topPos + 130;
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

    // Show page count in top-right area
    int pageCount = FolderItem.getPageCount(menu.getFolderStack());
    String countText = pageCount + "/" + FolderItem.MAX_PAGES;
    int countWidth = this.font.width(countText);
    this.font.draw(poseStack, countText, this.imageWidth - countWidth - 8, 6, 0x404040);
  }
}
