package guideme.render;

import guideme.color.ARGB;
import guideme.color.ColorValue;
import guideme.color.LightDarkMode;
import guideme.document.LytRect;
import guideme.internal.GuideMEClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

public final class SimpleRenderContext implements RenderContext {
    private final List<Matrix4f> transformStack = new ArrayList<>();
    private final List<LytRect> scissorStack = new ArrayList<>();
    private final LytRect viewport;
    private final LightDarkMode lightDarkMode;

    public SimpleRenderContext(LytRect viewport, LightDarkMode lightDarkMode) {
        this.transformStack.addLast(new Matrix4f());
        this.viewport = viewport;
        this.lightDarkMode = lightDarkMode;
    }

    public SimpleRenderContext(LytRect viewport) {
        this(viewport, GuideMEClient.currentLightDarkMode());
    }

    public SimpleRenderContext() {
        this(getDefaultViewport());
    }

    private static LytRect getDefaultViewport() {
        final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return new LytRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight());
    }

    @Override
    public int resolveColor(ColorValue ref) {
        return ref.resolve(lightDarkMode);
    }

    @Override
    public void fillRect(LytRect rect,
            ColorValue topLeft, ColorValue topRight, ColorValue bottomRight, ColorValue bottomLeft) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        var tesselator = Tessellator.getInstance();
        var builder = tesselator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        final int z = 0;
        final int trARGB = resolveColor(topRight);
        builder.pos(rect.right(), rect.y(), z)
                .color(ARGB.red(trARGB), ARGB.green(trARGB),
                        ARGB.blue(trARGB), ARGB.alpha(trARGB))
                .endVertex();
        final int tlARGB = resolveColor(topLeft);
        builder.pos(rect.x(), rect.y(), z)
                .color(ARGB.red(tlARGB), ARGB.green(tlARGB),
                        ARGB.blue(tlARGB), ARGB.alpha(tlARGB))
                .endVertex();
        final int blARGB = resolveColor(bottomLeft);
        builder.pos(rect.x(), rect.bottom(), z)
                .color(ARGB.red(blARGB), ARGB.green(blARGB),
                        ARGB.blue(blARGB), ARGB.alpha(blARGB))
                .endVertex();
        final int brARGB = resolveColor(bottomRight);
        builder.pos(rect.right(), rect.bottom(), z)
                .color(ARGB.red(brARGB), ARGB.green(brARGB),
                        ARGB.blue(brARGB), ARGB.alpha(brARGB))
                .endVertex();
        tesselator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void fillTexturedRect(LytRect rect, ITextureObject texture,
            ColorValue topLeft, ColorValue topRight, ColorValue bottomRight, ColorValue bottomLeft,
            float u0, float v0, float u1, float v1) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.bindTexture(texture.getGlTextureId());
        var tesselator = Tessellator.getInstance();
        var builder = tesselator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        final int z = 0;
        final int trARGB = resolveColor(topRight);
        builder.pos(rect.right(), rect.y(), z).tex(u1, v0)
                .color(ARGB.red(trARGB), ARGB.green(trARGB),
                        ARGB.blue(trARGB), ARGB.alpha(trARGB))
                .endVertex();
        final int tlARGB = resolveColor(topLeft);
        builder.pos(rect.x(), rect.y(), z).tex(u0, v0)
                .color(ARGB.red(tlARGB), ARGB.green(tlARGB),
                        ARGB.blue(tlARGB), ARGB.alpha(tlARGB))
                .endVertex();
        final int blARGB = resolveColor(bottomLeft);
        builder.pos(rect.x(), rect.bottom(), z).tex(u0, v1)
                .color(ARGB.red(blARGB), ARGB.green(blARGB),
                        ARGB.blue(blARGB), ARGB.alpha(blARGB))
                .endVertex();
        final int brARGB = resolveColor(bottomRight);
        builder.pos(rect.right(), rect.bottom(), z).tex(u1, v1)
                .color(ARGB.red(brARGB), ARGB.green(brARGB),
                        ARGB.blue(brARGB), ARGB.alpha(brARGB))
                .endVertex();
        tesselator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
    }

    @Override
    public void fillTriangle(Vector2f p1, Vector2f p2, Vector2f p3, ColorValue color) {
        var resolvedColor = resolveColor(color);
        int red = ARGB.red(resolvedColor);
        int green = ARGB.green(resolvedColor);
        int blue = ARGB.blue(resolvedColor);
        int alpha = ARGB.alpha(resolvedColor);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        var tesselator = Tessellator.getInstance();
        var builder = tesselator.getBuffer();
        builder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        final int z = 0;
        builder.pos(p1.x, p1.y, z).color(red, green, blue, alpha).endVertex();
        builder.pos(p2.x, p2.y, z).color(red, green, blue, alpha).endVertex();
        builder.pos(p3.x, p3.y, z).color(red, green, blue, alpha).endVertex();
        tesselator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y, int z, float width, float height) {
        var renderItem = Minecraft.getMinecraft().getRenderItem();

        push();
        translate(x, y, z);
        // Purposefully do NOT scale the normals!
        // this happens on non-uniform scales when calling the normal scale method
        scale(width / 16, height / 16, Math.max(width / 16, height / 16));

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        var zLevel = renderItem.zLevel;
        renderItem.zLevel = 0;

        renderItem.renderItemAndEffectIntoGUI(stack, 0, 0);
        renderItem.renderItemOverlays(font(), stack, 0, 0);

        renderItem.zLevel = zLevel;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        pop();
    }

    @Override
    public Matrix4f pose() {
        return transformStack.getLast();
    }

    @Override
    public void push() {
        transformStack.addLast(new Matrix4f(pose()));
        GlStateManager.pushMatrix();
    }

    @Override
    public void pop() {
        if (transformStack.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root render transform.");
        }

        transformStack.removeLast();
        GlStateManager.popMatrix();
    }

    @Override
    public void pushScissor(LytRect bounds) {
        var rootBounds = bounds.transform(pose());
        var parentBounds = scissorStack.isEmpty() ? viewport : scissorStack.getLast();
        var effectiveBounds = LytRect.intersect(parentBounds, rootBounds);

        scissorStack.addLast(effectiveBounds);
        scissor(effectiveBounds);
    }

    @Override
    public void popScissor() {
        if (scissorStack.isEmpty()) {
            throw new IllegalStateException("There is no active scissor rectangle.");
        }

        scissorStack.removeLast();
        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            scissor(scissorStack.getLast());
        }
    }

    @Override
    public LytRect viewport() {
        var rootViewport = scissorStack.isEmpty() ? viewport : scissorStack.getLast();
        var pose = new Matrix4f(pose());
        pose.invert();
        return rootViewport.transform(pose);
    }

    @Override
    public LightDarkMode lightDarkMode() {
        return lightDarkMode;
    }

    public void assertClean() {
        if (transformStack.size() != 1) {
            throw new IllegalStateException("Unbalanced render transform stack.");
        }
        if (!scissorStack.isEmpty()) {
            throw new IllegalStateException("Unbalanced render scissor stack.");
        }
    }

    private static void scissor(LytRect bounds) {
        final var mc = Minecraft.getMinecraft();
        final var scale = new ScaledResolution(mc).getScaleFactor();

        var x = bounds.x() * scale;
        var y = mc.displayHeight - bounds.bottom() * scale;
        var width = bounds.width() * scale;
        var height = bounds.height() * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, Math.max(0, y), Math.max(0, width), Math.max(0, height));
    }
}
