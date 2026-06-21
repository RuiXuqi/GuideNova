package guideme.internal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class GuideScrollbar extends GuideButton {
    private static final int WIDTH = 8;
    private int contentHeight;
    private int scrollAmount;
    private Double thumbHeldAt;

    public GuideScrollbar() {
        super(0, 0, WIDTH, 0, "");
    }

    protected int getMaxScrollAmount() {
        return Math.max(0, contentHeight - (this.height - 4));
    }

    @Override
    protected void renderWidget(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }

        var maxScrollAmount = getMaxScrollAmount();
        if (maxScrollAmount <= 0) {
            return;
        }

        int thumbHeight = getThumbHeight();
        int left = x;
        int right = left + 8;
        int top = y + getThumbTop();
        int bottom = top + thumbHeight;

        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuffer();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        bufferBuilder.pos(right, top, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(left, top, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(left, bottom, 0.0).color(128, 128, 128, 255).endVertex();
        bufferBuilder.pos(right, bottom, 0.0).color(128, 128, 128, 255).endVertex();

        bufferBuilder.pos(right - 1, top, 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos(left, top, 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos(left, bottom - 1, 0.0).color(192, 192, 192, 255).endVertex();
        bufferBuilder.pos(right - 1, bottom - 1, 0.0).color(192, 192, 192, 255).endVertex();

        tesselator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    /**
     * The thumb is the draggable rectangle representing the current viewport being manipulated by the scrollbar.
     */
    private int getThumbTop() {
        if (getMaxScrollAmount() == 0) {
            return 0;
        }
        return Math.max(0, scrollAmount * (height - getThumbHeight()) / getMaxScrollAmount());
    }

    private int getThumbHeight() {
        if (contentHeight <= 0) {
            return 0;
        }
        return MathHelper.clamp((int) ((float) (this.height * this.height) / (float) contentHeight), 32, this.height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || button != 0) {
            return false;
        }

        var thumbTop = y + getThumbTop();
        var thumbBottom = thumbTop + getThumbHeight();

        boolean thumbHit = mouseX >= x
                && mouseX <= x + WIDTH
                && mouseY >= thumbTop
                && mouseY < thumbBottom;
        if (thumbHit) {
            this.thumbHeldAt = mouseY - thumbTop;
            return true;
        } else {
            this.thumbHeldAt = null;
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        this.thumbHeldAt = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.visible && this.thumbHeldAt != null) {

            var thumbY = (int) Math.round(mouseY - y - thumbHeldAt);
            var maxThumbY = height - getThumbHeight();
            var scrollAmount = (int) Math.round(thumbY / (double) maxThumbY * getMaxScrollAmount());
            setScrollAmount(scrollAmount);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.visible) {
            this.setScrollAmount((int) (this.scrollAmount - delta * 10));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void playPressSound(SoundHandler handler) {
        // Mute default sound
    }

    public void move(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        if (this.scrollAmount > getMaxScrollAmount()) {
            this.scrollAmount = getMaxScrollAmount();
        }
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(int scrollAmount) {
        this.scrollAmount = MathHelper.clamp(scrollAmount, 0, getMaxScrollAmount());
    }
}
