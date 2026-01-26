package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.element.MystGuiPanel;
import art.arcane.mystcraft.client.gui.element.MystGuiTextField;
import art.arcane.mystcraft.client.gui.element.MystGuiToggleButton;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Screen for the Folder item.
 * Shows pages stored in the folder with sort and search features.
 * Replicates legacy GuiInventoryFolder.
 */
public class FolderScreen extends AbstractContainerScreen<FolderMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "gui/notebook.png");

    private MystGuiPanel rootPanel;
    private MystGuiTextField searchField;
    private MystGuiToggleButton sortAZButton;
    private MystGuiToggleButton showAllButton;

    private boolean sortAlphabetically = false;
    private boolean showAll = false;
    private String searchText = "";

    public FolderScreen(FolderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 154;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        // Note: Custom GUI elements (sort, search) disabled for now - texture doesn't support them
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void highlightMatchingSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (searchText.isEmpty()) return;

        String lowerSearch = searchText.toLowerCase();

        // Highlight slots that match search
        for (int i = 0; i < FolderMenu.FOLDER_SLOTS; i++) {
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                String name = getPageDisplayName(stack);
                if (name != null && name.toLowerCase().contains(lowerSearch)) {
                    // Green highlight for matches
                    int x = leftPos + slot.x;
                    int y = topPos + slot.y;
                    guiGraphics.fill(x, y, x + 16, y + 16, 0x4000FF00);
                } else {
                    // Dim non-matches
                    int x = leftPos + slot.x;
                    int y = topPos + slot.y;
                    guiGraphics.fill(x, y, x + 16, y + 16, 0x80000000);
                }
            }
        }
    }

    private String getPageDisplayName(ItemStack stack) {
        ResourceLocation symbolId = Page.getSymbol(stack);
        if (symbolId != null) {
            var symbol = SymbolRegistry.get(symbolId);
            if (symbol != null) {
                return symbol.getLocalizedName();
            }
            return symbolId.getPath();
        }
        if (Page.isLinkPanel(stack)) {
            return "Link Panel";
        }
        if (Page.isBlank(stack)) {
            return "Blank Page";
        }
        return stack.getHoverName().getString();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // Show page count
        int pageCount = FolderItem.getPageCount(menu.getFolderStack());
        String countText = pageCount + "/" + FolderItem.MAX_PAGES;
        int countWidth = this.font.width(countText);
        guiGraphics.drawString(this.font, countText, this.imageWidth - countWidth - 8, 6, 0x404040, false);
    }
}
