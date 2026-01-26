package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Writing Desk block.
 * Shows main inventory slots, tab slots, and ink level.
 */
public class WritingDeskScreen extends AbstractContainerScreen<WritingDeskMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "gui/writingdesk.png");

    // Ink tank rendering dimensions
    private static final int INK_TANK_X = 62;
    private static final int INK_TANK_Y = 17;
    private static final int INK_TANK_WIDTH = 8;
    private static final int INK_TANK_HEIGHT = 52;

    public WritingDeskScreen(WritingDeskMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Larger GUI to accommodate tab slots
        this.imageWidth = 176;
        this.imageHeight = 186;
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

        // Render ink tank fill level
        renderInkTank(guiGraphics, x, y);
    }

    private void renderInkTank(GuiGraphics guiGraphics, int x, int y) {
        int inkAmount = menu.getInkAmount();
        int inkCapacity = menu.getInkCapacity();

        if (inkCapacity <= 0 || inkAmount <= 0) {
            return;
        }

        // Calculate fill height
        int fillHeight = (inkAmount * INK_TANK_HEIGHT) / inkCapacity;
        fillHeight = Math.min(fillHeight, INK_TANK_HEIGHT);

        // Draw ink fill (dark blue/black color for ink)
        int tankX = x + INK_TANK_X;
        int tankY = y + INK_TANK_Y;
        int fillY = tankY + (INK_TANK_HEIGHT - fillHeight);

        // Ink color (dark bluish-black)
        int inkColor = 0xFF1A1A2E;
        guiGraphics.fill(tankX, fillY, tankX + INK_TANK_WIDTH, tankY + INK_TANK_HEIGHT, inkColor);
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

        // Show ink amount
        int inkAmount = menu.getInkAmount();
        int inkCapacity = menu.getInkCapacity();
        String inkText = inkAmount + "/" + inkCapacity + " mB";
        guiGraphics.drawString(this.font, inkText, 8, 72, 0x404040, false);

        // Show ink cost indicator
        if (menu.hasEnoughInk()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.mystcraft.writing_desk.ready"),
                    8, 82, 0x207020, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("gui.mystcraft.writing_desk.needs_ink"),
                    8, 82, 0x702020, false);
        }
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // Ink tank tooltip
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + INK_TANK_X && mouseX < x + INK_TANK_X + INK_TANK_WIDTH &&
            mouseY >= y + INK_TANK_Y && mouseY < y + INK_TANK_Y + INK_TANK_HEIGHT) {
            int inkAmount = menu.getInkAmount();
            int inkCapacity = menu.getInkCapacity();
            Component tooltip = Component.literal("Black Ink: " + inkAmount + "/" + inkCapacity + " mB");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}
