package guideme.internal.screen;

import guideme.internal.util.TooltipRenderUtil;
import guideme.render.RenderContext;
import guideme.render.SimpleRenderContext;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

public abstract class IndepentScaleScreen extends BaseScreen {
    /**
     * The scale vs. the current gui scale to reach the desired scaling.
     */
    private double effectiveScale;

    protected IndepentScaleScreen() {
        this.effectiveScale = calculateEffectiveScale();
    }

    protected abstract float calculateEffectiveScale();

    @Override
    public void initGui() {
        super.initGui();

        final ScaledResolution sr = new ScaledResolution(this.mc);
        this.width = toVirtual(sr.getScaledWidth());
        this.height = toVirtual(sr.getScaledHeight());
    }

    // Do not throw exception
    @Override
    protected void actionPerformed(GuiButton button) {
    }

    @Override
    public final void onResize(Minecraft minecraft, int width, int height) {
        this.effectiveScale = calculateEffectiveScale();
        super.onResize(minecraft, toVirtual(width), toVirtual(height));
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTick) {
        var renderContext = new SimpleRenderContext();

        renderContext.push();
        // This scale has to be uniform, otherwise items rendered with it will have messed up normals (and broken
        // lighting)
        renderContext.scale((float) effectiveScale, (float) effectiveScale, (float) effectiveScale);
        scaledRender(renderContext, toVirtual(mouseX), toVirtual(mouseY), partialTick);
        renderContext.pop();
        renderContext.assertClean();
    }

    protected void scaledRender(RenderContext context, int mouseX, int mouseY, float partialTick) {
        super.drawScreen(mouseX, mouseY, partialTick);
        for (var guiButton : this.buttonList) {
            if (guiButton instanceof GuideButton guideButton && guideButton.isMouseOver()) {
                var tooltip = guideButton.getTooltip();
                if (tooltip != null && !tooltip.isEmpty()) {
                    var tooltipPos = TooltipRenderUtil.positionMenuTooltip(
                            this.fontRenderer, tooltip, this.width, this.height,
                            mouseX, mouseY, guideButton.y, guideButton.height);
                    tooltipPos = TooltipRenderUtil.applyDrawingOffset(tooltipPos);
                    this.drawHoveringText(tooltip, tooltipPos.x(), tooltipPos.y());
                }
            }
        }
    }

    @Override
    protected boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Hope this will not break someone's mixin...
        if (button == LEFT_MOUSE_BUTTON) {
            for (var guiButton : this.buttonList) {
                if (guiButton instanceof GuideButton guideButton ? guideButton.mouseClicked(mouseX, mouseY, button)
                        : guiButton.mousePressed(this.mc, (int) mouseX, (int) mouseY)) {
                    var event = new GuiScreenEvent.ActionPerformedEvent.Pre(
                            this, guiButton, this.buttonList);
                    if (MinecraftForge.EVENT_BUS.post(event))
                        break;
                    guiButton = event.getButton();

                    this.selectedButton = guiButton;
                    guiButton.playPressSound(this.mc.getSoundHandler());
                    this.actionPerformed(guiButton);

                    if (this.equals(this.mc.currentScreen))
                        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(
                                this, guiButton, this.buttonList));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.selectedButton instanceof GuideButton guideButton && button == LEFT_MOUSE_BUTTON) {
            this.selectedButton = null;
            return guideButton.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE)
            this.onClose();
    }

    /**
     * Call to close the screen.<br>
     * {@link #onGuiClosed()} is a simple call back. Do not display screens there.
     */
    protected void onClose() {
        this.mc.displayGuiScreen(null);
        // Don't know why vanilla called setIngameFocus() here, displayGuiScreen(null) have called it
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
    @Override
    protected boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.selectedButton instanceof GuideButton guideButton) {
            return guideButton.mouseDragged((int) mouseX, (int) mouseY, button, (int) dragX, (int) dragY);
        }
        return false;
    }

    @Override
    protected boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.getChildAt(mouseX, mouseY) instanceof GuideButton guideButton) {
            guideButton.mouseScrolled(mouseX, mouseY, delta);
            return true;
        }
        return false;
    }

    @Nullable
    protected GuiButton getChildAt(double mouseX, double mouseY) {
        for (var button : this.buttonList) {
            if (button instanceof GuideButton guideButton
                    && guideButton.isMouseOver(mouseX, mouseY))
                return button;
            else if (button.enabled && button.visible && button.isMouseOver())
                return button;
        }
        return null;
    }

    protected final int toVirtual(int value) {
        return (int) Math.round(value / effectiveScale);
    }

    protected final double toVirtual(double value) {
        return value / effectiveScale;
    }

    public double getEffectiveScale() {
        return effectiveScale;
    }

    @Override
    public List<String> getItemToolTip(ItemStack stack) {
        return getTooltipFromItem(mc, stack);
    }

    // Make it static
    public static List<String> getTooltipFromItem(Minecraft mc, ItemStack stack) {
        List<String> list = stack.getTooltip(mc.player,
                mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED
                        : ITooltipFlag.TooltipFlags.NORMAL);
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                list.set(i, stack.getItem().getForgeRarity(stack).getColor() + list.get(i));
            } else {
                list.set(i, TextFormatting.GRAY + list.get(i));
            }
        }
        return list;
    }
}
