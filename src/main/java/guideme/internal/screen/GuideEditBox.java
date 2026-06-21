package guideme.internal.screen;

import java.util.function.Consumer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

// TODO: Mouse interaction
public class GuideEditBox extends GuiTextField {
    @Nullable
    private Consumer<String> responder;
    @Nullable
    private String hint;

    public GuideEditBox(int id, FontRenderer font, int x, int y, int width, int height) {
        super(id, font, x, y, width, height);
    }

    public GuideEditBox(FontRenderer font, int x, int y, int width, int height) {
        this(-1, font, x, y, width, height);
    }

    @Override
    public void drawTextBox() {
        if (!this.getVisible())
            return;

        boolean bordered = this.getEnableBackgroundDrawing();
        if (bordered) {
            drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1,
                    this.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);
        }

        int color = this.isEnabled ? this.enabledColor : this.disabledColor;

        int cursorPosRel = this.cursorPosition - this.lineScrollOffset;
        int selectionEndRel = this.selectionEnd - this.lineScrollOffset;

        String visibleText = this.fontRenderer.trimStringToWidth(this.getText().substring(this.lineScrollOffset),
                this.getWidth());

        boolean cursorVisible = cursorPosRel >= 0 && cursorPosRel <= visibleText.length();
        boolean drawCursor = this.isFocused() && this.cursorCounter / 6 % 2 == 0 && cursorVisible;

        int textX = bordered ? this.x + 4 : this.x;
        int textY = bordered ? this.y + (this.height - 8) / 2 : this.y;
        final int textStartX = textX;

        selectionEndRel = MathHelper.clamp(selectionEndRel, 0, visibleText.length());

        if (!visibleText.isEmpty()) {
            String beforeText = cursorVisible ? visibleText.substring(0, cursorPosRel) : visibleText;
            textX = this.fontRenderer.drawStringWithShadow(beforeText, textStartX, textY, color);
        }

        boolean textTruncated = this.cursorPosition < this.getText().length()
                || this.getText().length() >= this.getMaxStringLength();
        int cursorX = textX;

        if (!cursorVisible) {
            cursorX = cursorPosRel > 0 ? textStartX + this.width : textStartX;
        } else if (textTruncated) {
            cursorX = textX - 1;
            --textX;
        }

        if (!visibleText.isEmpty() && cursorVisible && cursorPosRel < visibleText.length()) {
            String afterText = visibleText.substring(cursorPosRel);
            textX = this.fontRenderer.drawStringWithShadow(afterText, textX, textY, color);
        }

        if (this.hint != null && !this.hint.isEmpty() && visibleText.isEmpty() && !this.isFocused()) {
            this.fontRenderer.drawStringWithShadow(this.hint, textX, textY, color);
        }

        if (drawCursor) {
            if (textTruncated) {
                drawRect(cursorX, textY - 1, cursorX + 1, textY + 1 + this.fontRenderer.FONT_HEIGHT, 0xFFCFCFD0);
            } else {
                this.fontRenderer.drawStringWithShadow("_", cursorX, textY, color);
            }
        }

        if (selectionEndRel != cursorPosRel) {
            int selectionEndX = textStartX
                    + this.fontRenderer.getStringWidth(visibleText.substring(0, selectionEndRel));
            this.drawSelectionBox(cursorX, textY - 1, selectionEndX - 1, textY + 1 + this.fontRenderer.FONT_HEIGHT);
        }
    }

    // Patch vanilla missing methods
    @Override
    public void setText(String text) {
        String preText = this.getText();
        super.setText(text);
        if (!preText.equals(this.getText())) {
            this.setResponderEntryValue(this.getId(), this.getText());
        }
    }

    @Override
    public void setMaxStringLength(int length) {
        String preText = this.getText();
        super.setMaxStringLength(length);
        if (!preText.equals(this.getText())) {
            this.setResponderEntryValue(this.getId(), this.getText());
        }
    }

    @Override
    public void setResponderEntryValue(int id, String text) {
        if (this.responder != null) {
            this.responder.accept(text);
        }
        super.setResponderEntryValue(id, text);
    }

    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    public void setResponder(@Nullable Consumer<String> responder) {
        this.responder = responder;
    }
}
