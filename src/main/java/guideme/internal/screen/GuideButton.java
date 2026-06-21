package guideme.internal.screen;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.jetbrains.annotations.Nullable;

public abstract class GuideButton extends GuiButton {
    private @Nullable List<String> tooltip;

    public GuideButton(int x, int y, int width, int height, String text) {
        super(-1, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            this.hovered = mouseX >= this.x && mouseY >= this.y
                    && mouseX < this.x + this.width && mouseY < this.y + this.height;
            this.renderWidget(mc, mouseX, mouseY, partialTick);
        }
    }

    // Disable GuiButton event
    @Deprecated
    @Override
    public final boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return this.mouseClicked(mouseX, mouseY, 0);
    }

    @Deprecated
    @Override
    protected final void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
    }

    @Deprecated
    @Override
    public final void mouseReleased(int mouseX, int mouseY) {
        this.mouseReleased(mouseX, mouseY, BaseScreen.LEFT_MOUSE_BUTTON);
    }

    protected abstract void renderWidget(Minecraft mc, int mouseX, int mouseY, float partialTick);

    protected boolean isValidClickButton(int button) {
        return button == BaseScreen.LEFT_MOUSE_BUTTON;
    }

    /**
     * Called when the mouse is moved within the GUI element.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    public void mouseMoved(double mouseX, double mouseY) {
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.enabled && this.visible && mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.enabled && this.visible && mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    protected void onClick(double mouseX, double mouseY) {
    }

    protected void onRelease(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was clicked.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.enabled && this.visible && this.isValidClickButton(button) && this.clicked(mouseX, mouseY)) {
            this.onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was released.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that is being dragged.
     * @param dragX  the X distance of the drag.
     * @param dragY  the Y distance of the drag.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return false;
    }

    /**
     * Called when the mouse wheel is scrolled within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param delta  the scrolling delta.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    public void setTooltip(@Nullable List<String> tooltip) {
        this.tooltip = tooltip;
    }

    public @Nullable List<String> getTooltip() {
        return tooltip;
    }
}
