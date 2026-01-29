package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.element.MystGuiPanel;
import art.arcane.mystcraft.client.gui.element.MystGuiTextField;
import art.arcane.mystcraft.client.gui.element.MystGuiToggleButton;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the Link Modifier block.
 */
public class LinkModifierScreen extends AbstractContainerScreen<LinkModifierMenu> {

  private static final ResourceLocation TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "gui/linkmodifier.png");
  // Dead link warning colors
  private static final int WARNING_COLOR_LIGHT = 0xFFFF4444;
  private static final int WARNING_COLOR_DARK = 0xFFAA0000;
  private final List<MystGuiToggleButton> flagButtons = new ArrayList<>();
  private MystGuiPanel rootPanel;
  private MystGuiTextField seedTextField;
  private MystGuiTextField nameTextField;
  private MystGuiToggleButton armKillButton;
  private MystGuiToggleButton confirmKillButton;
  private final boolean isArmed = false;
  private int warningPulse = 0;

  public LinkModifierScreen(LinkModifierMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    // Note: Custom GUI elements (property toggles, text fields) disabled for now
    // Basic book slot GUI only
  }

  private MystGuiToggleButton createFlagButton(String property, int x, int y, int size) {
    return new MystGuiToggleButton(
        () -> menu.getLinkFlag(property),
        (btn) -> {
          boolean newValue = !menu.getLinkFlag(property);
          menu.setLinkFlagClient(property, newValue);
          MystcraftNetwork.sendToServer(new ContainerActionPacket(
              ContainerActionPacket.Action.LINK_MODIFIER_SET_FLAG,
              menu.containerId,
              newValue,
              property
          ));
        },
        property,
        List.of(Component.literal(InkEffects.getLocalizedName(property))),
        x, y, size, size
    );
  }

  private void onSeedChanged(String newSeed) {
    menu.setItemSeedClient(newSeed);
    MystcraftNetwork.sendToServer(new ContainerActionPacket(
        ContainerActionPacket.Action.LINK_MODIFIER_SET_SEED,
        menu.containerId,
        false,
        newSeed
    ));
  }

  private void onNameChanged(String newName) {
    menu.setBookTitleClient(newName);
    MystcraftNetwork.sendToServer(new ContainerActionPacket(
        ContainerActionPacket.Action.LINK_MODIFIER_SET_TITLE,
        menu.containerId,
        false,
        newName
    ));
  }

  @Override
  public void containerTick() {
    super.containerTick();
    warningPulse++;
  }

  @Override
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

    // Render dead link warning border if book has dead link
    if (hasDeadLink()) {
      renderDeadLinkWarning(guiGraphics);
    }
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);

    // Render dead link tooltip when hovering over book slot
    if (hasDeadLink()) {
      int bookSlotX = this.leftPos + 80;
      int bookSlotY = this.topPos + 35;
      if (mouseX >= bookSlotX && mouseX < bookSlotX + 16 &&
          mouseY >= bookSlotY && mouseY < bookSlotY + 16) {
        renderDeadLinkTooltip(guiGraphics, mouseX, mouseY);
      }
    }
  }

  @Override
  protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

    // Render "DEAD LINK" warning text if applicable
    if (hasDeadLink()) {
      float pulse = (float) (0.5 + 0.5 * Math.sin(warningPulse * 0.15));
      int color = lerpColor(WARNING_COLOR_DARK, WARNING_COLOR_LIGHT, pulse);
      String warning = "DEAD LINK";
      int textWidth = this.font.width(warning);
      guiGraphics.drawString(this.font, warning, (this.imageWidth - textWidth) / 2, 55, color, false);
    }
  }

  /**
   * Checks if the current book has a dead link (links to non-existent Age).
   */
  private boolean hasDeadLink() {
    ItemStack book = menu.getBookItem();
    if (book.isEmpty()) {
      return false;
    }

    // Check if the linked age exists
    LinkOptions linkOptions = LinkOptions.fromItemStack(book);
    if (linkOptions == null) {
      return false;
    }

    Integer linkedAgeId = linkOptions.getDimensionUID();
    if (linkedAgeId == null || linkedAgeId <= 0) {
      return false;
    }

    // Check if Age exists (client-side heuristic - check if Age data is valid)
    // The menu should track this information
    return menu.isLinkedAgeDead();
  }

  /**
   * Renders pulsing red border around book slot for dead link warning.
   */
  private void renderDeadLinkWarning(GuiGraphics guiGraphics) {
    float pulse = (float) (0.5 + 0.5 * Math.sin(warningPulse * 0.15));
    int color = lerpColor(WARNING_COLOR_DARK, WARNING_COLOR_LIGHT, pulse);

    // Draw border around book slot area (slot at ~80,35 in GUI)
    int slotX = this.leftPos + 79;
    int slotY = this.topPos + 34;
    int slotSize = 18;

    // Top border
    guiGraphics.fill(slotX - 1, slotY - 1, slotX + slotSize + 1, slotY, color);
    // Bottom border
    guiGraphics.fill(slotX - 1, slotY + slotSize, slotX + slotSize + 1, slotY + slotSize + 1, color);
    // Left border
    guiGraphics.fill(slotX - 1, slotY, slotX, slotY + slotSize, color);
    // Right border
    guiGraphics.fill(slotX + slotSize, slotY, slotX + slotSize + 1, slotY + slotSize, color);
  }

  /**
   * Renders tooltip explaining the dead link.
   */
  private void renderDeadLinkTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    List<Component> tooltip = new ArrayList<>();
    tooltip.add(Component.literal("Dead Link").withStyle(style -> style.withColor(0xFF4444)));
    tooltip.add(Component.literal("The linked Age no longer exists."));
    tooltip.add(Component.literal(""));
    tooltip.add(Component.literal("This book cannot be used to link."));
    tooltip.add(Component.literal("Consider recycling it for ink."));

    guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
  }

  /**
   * Linearly interpolates between two colors.
   */
  private int lerpColor(int color1, int color2, float t) {
    int a1 = (color1 >> 24) & 0xFF;
    int r1 = (color1 >> 16) & 0xFF;
    int g1 = (color1 >> 8) & 0xFF;
    int b1 = color1 & 0xFF;

    int a2 = (color2 >> 24) & 0xFF;
    int r2 = (color2 >> 16) & 0xFF;
    int g2 = (color2 >> 8) & 0xFF;
    int b2 = color2 & 0xFF;

    int a = (int) (a1 + (a2 - a1) * t);
    int r = (int) (r1 + (r2 - r1) * t);
    int g = (int) (g1 + (g2 - g1) * t);
    int b = (int) (b1 + (b2 - b1) * t);

    return (a << 24) | (r << 16) | (g << 8) | b;
  }
}
