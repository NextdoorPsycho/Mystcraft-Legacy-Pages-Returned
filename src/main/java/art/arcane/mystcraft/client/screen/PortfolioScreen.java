package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.PortfolioMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Portfolio item.
 * Shows pages stored in the portfolio (64 slots in 8x8 grid).
 */
public class PortfolioScreen extends AbstractContainerScreen<PortfolioMenu> {

    // Use vanilla generic 54 slot texture as placeholder until custom texture is created
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    public PortfolioScreen(PortfolioMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Larger GUI for 8x8 grid + player inventory
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
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
        int pageCount = PortfolioItem.getPageCount(menu.getPortfolioStack());
        String countText = pageCount + "/" + PortfolioItem.MAX_PAGES;
        int countWidth = this.font.width(countText);
        guiGraphics.drawString(this.font, countText, this.imageWidth - countWidth - 8, this.titleLabelY, 0x404040, false);
    }
}
