package guideme.internal.util;

import guideme.color.ARGB;
import guideme.document.LytRect;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Rendering helper for rendering a rectangle with a dashed outline.
 */
public final class DashedRectangle {
    private DashedRectangle() {
    }

    public static void render(LytRect bounds, DashPattern pattern, float z) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder builder = tesselator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        var t = 0f;
        if (pattern.animationCycleMs() > 0) {
            t = (System.currentTimeMillis() % (int) pattern.animationCycleMs()) / pattern.animationCycleMs();
        }

        buildHorizontalDashedLine(builder, t, bounds.x(), bounds.right(), bounds.y(), z, pattern, false);
        buildHorizontalDashedLine(builder, t, bounds.x(), bounds.right(), bounds.bottom() - pattern.width(), z,
                pattern, true);

        buildVerticalDashedLine(builder, t, bounds.x(), bounds.y(), bounds.bottom(), z, pattern, true);
        buildVerticalDashedLine(builder, t, bounds.right() - pattern.width(), bounds.y(), bounds.bottom(), z,
                pattern, false);

        tesselator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void buildHorizontalDashedLine(BufferBuilder builder,
            float t, float x1, float x2, float y, float z,
            DashPattern pattern, boolean reverse) {
        if (!reverse) {
            t = 1 - t;
        }
        var phase = t * pattern.length();

        var color = pattern.color();
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int alpha = ARGB.alpha(color);

        for (float x = x1 - phase; x < x2; x += pattern.length()) {
            builder.pos(MathHelper.clamp(x + pattern.onLength(), x1, x2), y, z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(MathHelper.clamp(x, x1, x2), y, z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(MathHelper.clamp(x, x1, x2), y + pattern.width(), z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(MathHelper.clamp(x + pattern.onLength(), x1, x2), y + pattern.width(), z)
                    .color(red, green, blue, alpha).endVertex();
        }
    }

    private static void buildVerticalDashedLine(BufferBuilder builder,
            float t, float x, float y1, float y2, float z,
            DashPattern pattern, boolean reverse) {
        if (!reverse) {
            t = 1 - t;
        }
        var phase = t * pattern.length();

        var color = pattern.color();
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int alpha = ARGB.alpha(color);

        for (float y = y1 - phase; y < y2; y += pattern.length()) {
            builder.pos(x + pattern.width(), MathHelper.clamp(y, y1, y2), z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(x, MathHelper.clamp(y, y1, y2), z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(x, MathHelper.clamp(y + pattern.onLength(), y1, y2), z)
                    .color(red, green, blue, alpha).endVertex();
            builder.pos(x + pattern.width(), MathHelper.clamp(y + pattern.onLength(), y1, y2), z)
                    .color(red, green, blue, alpha).endVertex();
        }
    }

}
