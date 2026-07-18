package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.gui.element.MystGuiPanel;
import art.arcane.mystcraft.client.gui.element.MystGuiTextField;
import art.arcane.mystcraft.client.gui.element.MystGuiToggleButton;
import art.arcane.mystcraft.client.gui.procedural.GuiTheme;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the Link Modifier block.
 */
public class LinkModifierScreen extends AbstractContainerScreen<LinkModifierMenu> {

  private final List<MystGuiToggleButton> flagButtons = new ArrayList<>();
  private MystGuiPanel rootPanel;
  private MystGuiTextField seedTextField;
  private MystGuiTextField nameTextField;
  private MystGuiToggleButton applyButton;
  private int warningPulse = 0;

  public LinkModifierScreen(LinkModifierMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title, 248, 224);
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    this.inventoryLabelX = 43;
    this.inventoryLabelY = 134;

    rootPanel = new MystGuiPanel(0, 0, imageWidth, imageHeight);

    nameTextField = new MystGuiTextField(
        "BookTitle",
        menu::getBookTitle,
        this::onNameChanged,
        43, 26, 189, 14
    );
    nameTextField.setMaxLength(21);
    rootPanel.addChild(nameTextField);

    seedTextField = new MystGuiTextField(
        "AgeSeed",
        menu::getItemSeed,
        this::onSeedChanged,
        43, 51, 189, 14
    );
    seedTextField.setMaxLength(20);
    seedTextField.setTooltip(List.of(Component.literal("Optional signed 64-bit Age seed")));
    rootPanel.addChild(seedTextField);

    flagButtons.clear();
    for (int i = 0; i < LinkFlags.ALL_FLAGS.length; i++) {
      String property = LinkFlags.ALL_FLAGS[i];
      MystGuiToggleButton button = createFlagButton(property, 16 + i * 31, 78, 29);
      button.setHeight(18);
      button.setText(shortFlagName(property));
      flagButtons.add(button);
      rootPanel.addChild(button);
    }

    applyButton = new MystGuiToggleButton(
        "ApplyPages",
        menu::canModify,
        button -> MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.LINK_MODIFIER_APPLY,
            menu.containerId,
            false
        )),
        94, 109, 70, 18
    );
    applyButton.setText("APPLY PAGES");
    applyButton.setTooltip(List.of(Component.literal("Consume modifier pages and apply their link properties")));
    rootPanel.addChild(applyButton);
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

  private String shortFlagName(String property) {
    return switch (property) {
      case LinkFlags.INTRA_LINKING -> "INTRA";
      case LinkFlags.INTRA_LINKING_ONLY -> "ONLY";
      case LinkFlags.GENERATE_PLATFORM -> "PLAT";
      case LinkFlags.MAINTAIN_MOMENTUM -> "MOM";
      case LinkFlags.DISARM -> "DIS";
      case LinkFlags.RELATIVE -> "REL";
      case LinkFlags.FOLLOWING -> "FOL";
      default -> "?";
    };
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
    if (rootPanel != null) {
      rootPanel.tick();
    }
    if (seedTextField != null) {
      seedTextField.setVisible(menu.hasItemSeed());
    }
  }

  @Override
  public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                float partialTick) {
    super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
    ProceduralUI.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

    ProceduralUI.drawSlot(guiGraphics, this.leftPos + 16, this.topPos + 26);
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 16, this.topPos + 109, 4, 1, 0);

    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 42, this.topPos + 145, 9, 3, 0);
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 42, this.topPos + 203, 9, 1, 0);

    if (rootPanel != null) {
      rootPanel.setLeft(this.leftPos);
      rootPanel.setTop(this.topPos);
      rootPanel.renderBackground(guiGraphics, partialTick, mouseX, mouseY);
    }

    if (hasDeadLink()) {
      renderDeadLinkWarning(guiGraphics);
    }
  }

  @Override
  public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                 float partialTick) {
    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

    if (rootPanel != null) {
      rootPanel.renderForeground(guiGraphics, mouseX, mouseY);
      List<Component> tooltip = rootPanel.getTooltip(mouseX, mouseY);
      if (tooltip != null && !tooltip.isEmpty()) {
        guiGraphics.setTooltipForNextFrame(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
      }
    }

    if (hasDeadLink()) {
      int bookSlotX = this.leftPos + 17;
      int bookSlotY = this.topPos + 27;
      if (mouseX >= bookSlotX && mouseX < bookSlotX + 16 &&
          mouseY >= bookSlotY && mouseY < bookSlotY + 16) {
        renderDeadLinkTooltip(guiGraphics, mouseX, mouseY);
      }
    }
  }

  @Override
  protected void extractLabels(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    int textColor = GuiTheme.color("text_primary") & 0x00FFFFFF;
    guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, textColor, false);
    guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, textColor, false);
    guiGraphics.text(this.font, "Book", 16, 17, textColor, false);
    guiGraphics.text(this.font, "Title", 43, 17, textColor, false);
    if (menu.hasItemSeed()) {
      guiGraphics.text(this.font, "Seed", 43, 42, textColor, false);
    }
    guiGraphics.text(this.font, "Link Flags", 16, 68, textColor, false);
    guiGraphics.text(this.font, "Modifier Pages", 16, 99, textColor, false);

    if (hasDeadLink()) {
      float pulse = (float) (0.5 + 0.5 * Math.sin(warningPulse * 0.15));
      int color = lerpColor(GuiTheme.color("text_warning_dark"), GuiTheme.color("text_warning"), pulse);
      String warning = "DEAD LINK";
      int textWidth = this.font.width(warning);
      guiGraphics.text(this.font, warning, (this.imageWidth - textWidth) / 2, 128, color, false);
    }
  }

  private boolean hasDeadLink() {
    ItemStack book = menu.getBookItem();
    if (book.isEmpty()) {
      return false;
    }

    LinkOptions linkOptions = LinkOptions.fromItemStack(book);
    if (linkOptions == null) {
      return false;
    }

    Integer linkedAgeId = linkOptions.getDimensionUID();
    if (linkedAgeId == null || linkedAgeId <= 0) {
      return false;
    }

    return menu.isLinkedAgeDead();
  }

  private void renderDeadLinkWarning(GuiGraphicsExtractor guiGraphics) {
    float pulse = (float) (0.5 + 0.5 * Math.sin(warningPulse * 0.15));
    int color = lerpColor(GuiTheme.color("text_warning_dark"), GuiTheme.color("text_warning"), pulse);

    int slotX = this.leftPos + 16;
    int slotY = this.topPos + 26;
    int slotSize = 18;

    guiGraphics.fill(slotX - 1, slotY - 1, slotX + slotSize + 1, slotY, color);

    guiGraphics.fill(slotX - 1, slotY + slotSize, slotX + slotSize + 1, slotY + slotSize + 1, color);

    guiGraphics.fill(slotX - 1, slotY, slotX, slotY + slotSize, color);

    guiGraphics.fill(slotX + slotSize, slotY, slotX + slotSize + 1, slotY + slotSize, color);
  }

  private void renderDeadLinkTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    List<Component> tooltip = new ArrayList<>();
    tooltip.add(Component.literal("Dead Link").withStyle(style -> style.withColor(0xFF4444)));
    tooltip.add(Component.literal("The linked Age no longer exists."));
    tooltip.add(Component.literal(""));
    tooltip.add(Component.literal("This book cannot be used to link."));
    tooltip.add(Component.literal("Consider recycling it for ink."));

    guiGraphics.setTooltipForNextFrame(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
  }

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

  @Override
  public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
    if (rootPanel != null && rootPanel.mouseClicked(event.x(), event.y(), event.button())) {
      return true;
    }
    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseReleased(@NotNull MouseButtonEvent event) {
    if (rootPanel != null && rootPanel.mouseReleased(event.x(), event.y(), event.button())) {
      return true;
    }
    return super.mouseReleased(event);
  }

  @Override
  public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
    if (rootPanel != null && rootPanel.mouseDragged(
        event.x(), event.y(), event.button(), dragX, dragY)) {
      return true;
    }
    return super.mouseDragged(event, dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    if (rootPanel != null && rootPanel.mouseScrolled(mouseX, mouseY, deltaY)) {
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
  }

  @Override
  public boolean keyPressed(@NotNull KeyEvent event) {
    if (rootPanel != null && rootPanel.keyPressed(event.key(), event.scancode(), event.modifiers())) {
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(@NotNull CharacterEvent event) {
    int codePoint = event.codepoint();
    if (Character.isBmpCodePoint(codePoint) && rootPanel != null &&
        rootPanel.charTyped((char) codePoint, 0)) {
      return true;
    }
    return super.charTyped(event);
  }
}
