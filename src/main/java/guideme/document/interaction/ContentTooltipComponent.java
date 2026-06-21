package guideme.document.interaction;

import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.layout.LayoutContext;
import guideme.layout.MinecraftFontMetrics;
import guideme.render.RenderContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public class ContentTooltipComponent implements GuideTooltipComponent {
    // The window size for which we performed layout
    @Nullable
    private LytRect layoutViewport;
    @Nullable
    private LytRect layoutBox;

    private final LytBlock content;

    public ContentTooltipComponent(LytBlock content) {
        this.content = content;
    }

    @Override
    public int getHeight(RenderContext context) {
        return getLayoutBox().height();
    }

    @Override
    public int getWidth(RenderContext context) {
        return getLayoutBox().width();
    }

    @Override
    public void render(RenderContext context, int x, int y) {
        getLayoutBox(); // Updates layout

        context.push();
        context.translate(x, y, 0);
        content.render(context);
        context.pop();
    }

    private LytRect getLayoutBox() {
        var screen = Minecraft.getMinecraft().currentScreen;
        var currentViewport = new LytRect(0, 0, screen.width, screen.height);
        if (layoutBox == null || !currentViewport.equals(layoutViewport)) {
            layoutViewport = currentViewport;
            var layoutContext = new LayoutContext(new MinecraftFontMetrics());
            layoutBox = content.layout(layoutContext, 0, 0, screen.width / 2);
        }
        return layoutBox;
    }
}
