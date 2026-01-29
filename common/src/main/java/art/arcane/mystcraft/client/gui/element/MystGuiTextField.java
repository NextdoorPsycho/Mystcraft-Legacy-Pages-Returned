package art.arcane.mystcraft.client.gui.element;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Text field GUI element for the Mystcraft GUI system.
 */
public class MystGuiTextField extends MystGuiElement {

  private final String id;
  private final Supplier<String> textProvider;
  private final Consumer<String> textConsumer;

  private String text = "";
  private int maxLength = 32;
  private int cursorPosition = 0;
  private int selectionEnd = 0;
  private boolean focused = false;
  private boolean editable = true;
  private int textColor = 0xE0E0E0;
  private int borderColor = 0xA0A0A0;
  private int cursorCounter = 0;

  public MystGuiTextField(String id, Supplier<String> textProvider, Consumer<String> textConsumer,
                          int left, int top, int width, int height) {
    super(left, top, width, height);
    this.id = id;
    this.textProvider = textProvider;
    this.textConsumer = textConsumer;
  }

  public MystGuiTextField(Supplier<String> textProvider, Consumer<String> textConsumer,
                          int left, int top, int width, int height) {
    super(left, top, width, height);
    this.id = "";
    this.textProvider = textProvider;
    this.textConsumer = textConsumer;
  }

  public String getId() {
    return id;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  public void setTextColor(int color) {
    this.textColor = color;
  }

  public void setBorderColor(int color) {
    this.borderColor = color;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text != null ? text : "";
    this.cursorPosition = Math.min(cursorPosition, this.text.length());
    this.selectionEnd = cursorPosition;
  }

  public boolean isFocused() {
    return focused;
  }

  public void setFocused(boolean focused) {
    this.focused = focused;
  }

  @Override
  protected void onTick() {
    cursorCounter++;

    // Sync text from provider
    if (textProvider != null && !focused) {
      String provided = textProvider.get();
      if (provided != null && !provided.equals(text)) {
        text = provided;
        cursorPosition = Math.min(cursorPosition, text.length());
        selectionEnd = cursorPosition;
      }
    }
  }

  @Override
  protected void doRenderBackground(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    int x = getLeft();
    int y = getTop();

    // Draw border
    int borderColorToUse = focused ? 0xFFFFFF : borderColor;
    guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000 | borderColorToUse);

    // Draw background
    guiGraphics.fill(x, y, x + width, y + height, 0xFF000000);

    // Draw text
    int textX = x + 4;
    int textY = y + (height - 8) / 2;

    String displayText = text;
    int textWidth = mc.font.width(displayText);

    // Scroll if text is too long
    int maxTextWidth = width - 8;
    int scrollOffset = 0;
    if (textWidth > maxTextWidth && focused) {
      int cursorX = mc.font.width(text.substring(0, cursorPosition));
      if (cursorX > maxTextWidth) {
        scrollOffset = cursorX - maxTextWidth;
      }
    }

    // Enable scissor
    guiGraphics.enableScissor(x + 2, y, x + width - 2, y + height);
    guiGraphics.drawString(mc.font, displayText, textX - scrollOffset, textY, textColor);

    // Draw cursor
    if (focused && editable && (cursorCounter / 6) % 2 == 0) {
      int cursorX = textX + mc.font.width(text.substring(0, cursorPosition)) - scrollOffset;
      guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, 0xFFD0D0D0);
    }

    // Draw selection
    if (selectionEnd != cursorPosition) {
      int startPos = Math.min(cursorPosition, selectionEnd);
      int endPos = Math.max(cursorPosition, selectionEnd);
      int selStartX = textX + mc.font.width(text.substring(0, startPos)) - scrollOffset;
      int selEndX = textX + mc.font.width(text.substring(0, endPos)) - scrollOffset;
      guiGraphics.fill(selStartX, textY - 1, selEndX, textY + 9, 0x803030FF);
    }

    guiGraphics.disableScissor();
  }

  @Override
  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    boolean wasClicked = contains(mouseX, mouseY);
    setFocused(wasClicked && editable);

    if (focused && button == 0) {
      // Calculate cursor position from click
      int x = getLeft() + 4;
      int relX = (int) mouseX - x;
      int pos = 0;
      for (int i = 0; i <= text.length(); i++) {
        int charWidth = mc.font.width(text.substring(0, i));
        if (charWidth > relX) {
          break;
        }
        pos = i;
      }
      cursorPosition = pos;
      selectionEnd = pos;
      return true;
    }

    return wasClicked;
  }

  @Override
  protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
    if (!focused || !editable) return false;

    boolean ctrl = Screen.hasControlDown();
    boolean shift = Screen.hasShiftDown();

    switch (keyCode) {
      case GLFW.GLFW_KEY_BACKSPACE:
        if (ctrl) {
          deleteWord(-1);
        } else if (selectionEnd != cursorPosition) {
          deleteSelection();
        } else if (cursorPosition > 0) {
          text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
          cursorPosition--;
          selectionEnd = cursorPosition;
          onTextChanged();
        }
        return true;

      case GLFW.GLFW_KEY_DELETE:
        if (ctrl) {
          deleteWord(1);
        } else if (selectionEnd != cursorPosition) {
          deleteSelection();
        } else if (cursorPosition < text.length()) {
          text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
          onTextChanged();
        }
        return true;

      case GLFW.GLFW_KEY_LEFT:
        if (ctrl) {
          cursorPosition = findWordBoundary(-1);
        } else if (cursorPosition > 0) {
          cursorPosition--;
        }
        if (!shift) selectionEnd = cursorPosition;
        return true;

      case GLFW.GLFW_KEY_RIGHT:
        if (ctrl) {
          cursorPosition = findWordBoundary(1);
        } else if (cursorPosition < text.length()) {
          cursorPosition++;
        }
        if (!shift) selectionEnd = cursorPosition;
        return true;

      case GLFW.GLFW_KEY_HOME:
        cursorPosition = 0;
        if (!shift) selectionEnd = cursorPosition;
        return true;

      case GLFW.GLFW_KEY_END:
        cursorPosition = text.length();
        if (!shift) selectionEnd = cursorPosition;
        return true;

      case GLFW.GLFW_KEY_A:
        if (ctrl) {
          cursorPosition = text.length();
          selectionEnd = 0;
          return true;
        }
        break;

      case GLFW.GLFW_KEY_C:
        if (ctrl) {
          mc.keyboardHandler.setClipboard(getSelectedText());
          return true;
        }
        break;

      case GLFW.GLFW_KEY_V:
        if (ctrl) {
          insertText(mc.keyboardHandler.getClipboard());
          return true;
        }
        break;

      case GLFW.GLFW_KEY_X:
        if (ctrl) {
          mc.keyboardHandler.setClipboard(getSelectedText());
          deleteSelection();
          return true;
        }
        break;

      case GLFW.GLFW_KEY_ESCAPE:
        setFocused(false);
        return true;
    }

    return false;
  }

  @Override
  protected boolean onCharTyped(char codePoint, int modifiers) {
    if (!focused || !editable) return false;

    if (SharedConstants.isAllowedChatCharacter(codePoint)) {
      insertText(String.valueOf(codePoint));
      return true;
    }
    return false;
  }

  private void insertText(String str) {
    if (str == null || str.isEmpty()) return;

    deleteSelection();

    String newText = text.substring(0, cursorPosition) + str + text.substring(cursorPosition);
    if (newText.length() <= maxLength) {
      text = newText;
      cursorPosition += str.length();
      selectionEnd = cursorPosition;
      onTextChanged();
    }
  }

  private void deleteSelection() {
    if (selectionEnd == cursorPosition) return;
    int start = Math.min(cursorPosition, selectionEnd);
    int end = Math.max(cursorPosition, selectionEnd);
    text = text.substring(0, start) + text.substring(end);
    cursorPosition = start;
    selectionEnd = start;
    onTextChanged();
  }

  private String getSelectedText() {
    int start = Math.min(cursorPosition, selectionEnd);
    int end = Math.max(cursorPosition, selectionEnd);
    return text.substring(start, end);
  }

  private void deleteWord(int direction) {
    if (selectionEnd != cursorPosition) {
      deleteSelection();
      return;
    }
    int boundary = findWordBoundary(direction);
    int start = Math.min(cursorPosition, boundary);
    int end = Math.max(cursorPosition, boundary);
    text = text.substring(0, start) + text.substring(end);
    cursorPosition = start;
    selectionEnd = start;
    onTextChanged();
  }

  private int findWordBoundary(int direction) {
    int pos = cursorPosition;
    if (direction < 0) {
      while (pos > 0 && text.charAt(pos - 1) == ' ') pos--;
      while (pos > 0 && text.charAt(pos - 1) != ' ') pos--;
    } else {
      while (pos < text.length() && text.charAt(pos) == ' ') pos++;
      while (pos < text.length() && text.charAt(pos) != ' ') pos++;
    }
    return pos;
  }

  private void onTextChanged() {
    if (textConsumer != null) {
      textConsumer.accept(text);
    }
  }
}
