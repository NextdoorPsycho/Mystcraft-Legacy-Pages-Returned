package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.FolderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Folder item.
 * Shows 16 page slots (2 rows x 8 columns) plus player inventory.
 * Renders slots programmatically with standard Minecraft styling.
 */
public class FolderScreen extends AbstractContainerScreen<FolderMenu> {

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
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    // Container panel background + raised border
    ProceduralUI.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

    // Folder page slots (2 rows of 8)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 17, this.topPos + 18, 8, 2, 0);

    // Player inventory (3 rows of 9)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 72, 9, 3, 0);

    // Hotbar (1 row of 9)
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 130, 9, 1, 0);
  }


  @Override
  protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

    // Show page count in top-right area
    int pageCount = FolderItem.getPageCount(menu.getFolderStack());
    String countText = pageCount + "/" + FolderItem.MAX_PAGES;
    int countWidth = this.font.width(countText);
    guiGraphics.drawString(this.font, countText, this.imageWidth - countWidth - 8, 6, 0x404040, false);
  }
}
