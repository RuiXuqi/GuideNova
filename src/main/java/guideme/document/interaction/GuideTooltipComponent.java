package guideme.document.interaction;

import guideme.render.RenderContext;

public interface GuideTooltipComponent {

    int getWidth(RenderContext context);

    int getHeight(RenderContext context);

    default void render(RenderContext context, int x, int y) {
    }

}
