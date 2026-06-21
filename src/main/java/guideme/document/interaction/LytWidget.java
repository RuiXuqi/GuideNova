package guideme.document.interaction;

import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.internal.screen.BaseScreen;
import guideme.internal.screen.GuideButton;
import guideme.layout.LayoutContext;
import guideme.render.RenderContext;
import guideme.ui.GuideUiHost;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

/**
 * Wraps an {@link GuideButton} for use within the guidebook layout tree.
 */
public class LytWidget extends LytBlock implements InteractiveElement {
    private final GuideButton widget;

    public LytWidget(GuideButton widget) {
        this.widget = widget;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(
                x, y,
                widget.width, widget.height);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        widget.x += deltaX;
        widget.y += deltaY;
    }

    @Override
    public void render(RenderContext context) {
        updateWidgetPosition();

        var minecraft = Minecraft.getMinecraft();
        var screen = minecraft.currentScreen;

        if (!(screen instanceof GuideUiHost uiHost)) {
            return; // Can't render if we can't translate
        }

        var mouseDocPos = screen instanceof BaseScreen baseScreen
                ? uiHost.getDocumentPoint(baseScreen.getMouseX(), baseScreen.getMouseY())
                : uiHost.getDocumentPoint(
                        (double) (Mouse.getX() * screen.width) / minecraft.displayWidth,
                        screen.height - (double) (Mouse.getY() * screen.height) / minecraft.displayHeight - 1);

        widget.drawButton(
                minecraft,
                mouseDocPos != null ? mouseDocPos.x() : -100,
                mouseDocPos != null ? mouseDocPos.y() : -100,
                minecraft.getRenderPartialTicks());
    }

    private void updateWidgetPosition() {
        widget.x = bounds.x();
        widget.y = bounds.y();
    }

    @Override
    public boolean mouseMoved(GuideUiHost screen, int x, int y) {
        widget.mouseMoved(x, y);
        return true;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button) {
        if (this.widget.mouseClicked(x, y, button)) {
            this.widget.playPressSound(Minecraft.getMinecraft().getSoundHandler());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(GuideUiHost screen, int x, int y, int button) {
        return widget.mouseReleased(x, y, button);
    }

    // No tooltips

    public GuideButton getWidget() {
        return widget;
    }
}
