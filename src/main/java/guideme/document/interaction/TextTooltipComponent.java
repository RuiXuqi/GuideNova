package guideme.document.interaction;

import guideme.render.RenderContext;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class TextTooltipComponent implements GuideTooltipComponent {
    private final String text;

    public TextTooltipComponent(String text) {
        this.text = text;
    }

    @Override
    public int getWidth(RenderContext context) {
        return context.font().getStringWidth(this.text);
    }

    @Override
    public int getHeight(RenderContext context) {
        return 10;
    }

    @Override
    public void render(RenderContext context, int x, int y) {
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.disableDepth();
        context.font().drawString(this.text, x, y, -1, true);
        if (depth)
            GlStateManager.enableDepth();
        else
            GlStateManager.disableDepth();
    }
}
