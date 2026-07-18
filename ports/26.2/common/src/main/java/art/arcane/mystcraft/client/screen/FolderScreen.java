package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.FolderMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Folder item. Shows 16 page slots (2 rows x 8 columns) plus
 * player inventory. Renders slots programmatically with standard Minecraft
 * styling.
 */
public class FolderScreen extends AbstractContainerScreen<FolderMenu> {

  public FolderScreen(FolderMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title, 176, 154);
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    this.inventoryLabelY = 60;
  }

  @Override
  public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                float partialTick) {
    super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
    ProceduralUI.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 17, this.topPos + 18, 8, 2, 0);

    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 72, 9, 3, 0);

    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + 130, 9, 1, 0);
  }

  @Override
  protected void extractLabels(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

    int pageCount = FolderItem.getPageCount(menu.getFolderStack());
    String countText = pageCount + "/" + FolderItem.MAX_PAGES;
    int countWidth = this.font.width(countText);
    guiGraphics.text(this.font, countText, this.imageWidth - countWidth - 8, 6, 0x404040, false);
  }
}
