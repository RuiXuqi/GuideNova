package guideme.scene.annotation;

import guideme.color.ARGB;
import guideme.color.LightDarkMode;
import guideme.color.MutableColor;
import guideme.internal.GuideMEClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class InWorldAnnotationRenderer {

    private InWorldAnnotationRenderer() {
    }

    public static void render(Iterable<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        var mc = Minecraft.getMinecraft();
        var sprite = mc.getTextureMapBlocks().getAtlasSprite(GuideMEClient.NOISE_ID.toString());

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();

        GlStateManager.depthFunc(GL11.GL_GREATER);
        GlStateManager.depthMask(false);
        renderPass(annotations, lightDarkMode, sprite, false, true);

        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(false);
        renderPass(annotations, lightDarkMode, sprite, false, false);

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        renderPass(annotations, lightDarkMode, sprite, true, false);

        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderPass(
            Iterable<InWorldAnnotation> annotations,
            LightDarkMode lightDarkMode,
            TextureAtlasSprite sprite,
            boolean alwaysOnTop,
            boolean occluded) {
        var tess = Tessellator.getInstance();
        var buffer = tess.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

        for (var annotation : annotations) {
            if (annotation.isAlwaysOnTop() != alwaysOnTop) {
                continue;
            }
            if (occluded && annotation.isAlwaysOnTop()) {
                continue; // Don't render occlusion for always-on-top annotations
            }

            if (annotation instanceof InWorldBoxAnnotation boxAnnotation) {
                var color = MutableColor.of(boxAnnotation.color(), lightDarkMode);
                if (occluded) {
                    color.darker(50).setAlpha(color.alpha() * 0.5f);
                }
                if (boxAnnotation.isHovered()) {
                    color.lighter(50);
                }
                render(buffer,
                        boxAnnotation.min(),
                        boxAnnotation.max(),
                        color.toArgb32(),
                        boxAnnotation.thickness(),
                        sprite);
            } else if (annotation instanceof InWorldLineAnnotation lineAnnotation) {
                var color = MutableColor.of(lineAnnotation.color(), lightDarkMode);
                if (occluded) {
                    color.darker(50).setAlpha(color.alpha() * 0.5f);
                }
                if (lineAnnotation.isHovered()) {
                    color.lighter(50);
                }
                strut(buffer,
                        lineAnnotation.min(),
                        lineAnnotation.max(),
                        color.toArgb32(),
                        lineAnnotation.thickness(),
                        true,
                        true,
                        sprite);
            }
        }

        tess.draw();
    }

    public static void render(BufferBuilder consumer,
            Vector3f min,
            Vector3f max,
            int color,
            float thickness,
            TextureAtlasSprite sprite) {
        var thickHalf = thickness * 0.5f;

        var u = new Vector3f(max.x - min.x, 0, 0);
        var v = new Vector3f(0, max.y - min.y, 0);
        var t = new Vector3f(0, 0, max.z - min.z);
        var uNorm = new Vector3f(u).normalize();
        var vNorm = new Vector3f(v).normalize();
        var tNorm = new Vector3f(t).normalize();

        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f(min);
        corners[1] = new Vector3f(min).add(u);
        corners[2] = new Vector3f(min).add(v);
        corners[3] = new Vector3f(min).add(t);
        corners[4] = new Vector3f(max);
        corners[5] = new Vector3f(max).sub(u);
        corners[6] = new Vector3f(max).sub(v);
        corners[7] = new Vector3f(max).sub(t);

        // Along X-Axis
        // Extend these out to cover past the corner (half the extrude thickness)
        strut(consumer, new Vector3f(uNorm).mulAdd(-thickHalf, corners[0]),
                new Vector3f(uNorm).mulAdd(thickHalf, corners[1]), color, thickness, true, true, sprite);
        strut(consumer, new Vector3f(uNorm).mulAdd(-thickHalf, corners[2]),
                new Vector3f(uNorm).mulAdd(thickHalf, corners[7]), color, thickness, true, true, sprite);
        strut(consumer, new Vector3f(uNorm).mulAdd(-thickHalf, corners[3]),
                new Vector3f(uNorm).mulAdd(thickHalf, corners[6]), color, thickness, true, true, sprite);
        strut(consumer, new Vector3f(uNorm).mulAdd(-thickHalf, corners[5]),
                new Vector3f(uNorm).mulAdd(thickHalf, corners[4]), color, thickness, true, true, sprite);

        // Along Y-Axis
        strut(consumer, new Vector3f(vNorm).mulAdd(thickHalf, corners[0]),
                new Vector3f(vNorm).mulAdd(-thickHalf, corners[2]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(vNorm).mulAdd(thickHalf, corners[1]),
                new Vector3f(vNorm).mulAdd(-thickHalf, corners[7]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(vNorm).mulAdd(thickHalf, corners[3]),
                new Vector3f(vNorm).mulAdd(-thickHalf, corners[5]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(vNorm).mulAdd(thickHalf, corners[6]),
                new Vector3f(vNorm).mulAdd(-thickHalf, corners[4]), color, thickness, false, false, sprite);

        // Along Z-Axis
        strut(consumer, new Vector3f(tNorm).mulAdd(thickHalf, corners[0]),
                new Vector3f(tNorm).mulAdd(-thickHalf, corners[3]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(tNorm).mulAdd(thickHalf, corners[1]),
                new Vector3f(tNorm).mulAdd(-thickHalf, corners[6]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(tNorm).mulAdd(thickHalf, corners[2]),
                new Vector3f(tNorm).mulAdd(-thickHalf, corners[5]), color, thickness, false, false, sprite);
        strut(consumer, new Vector3f(tNorm).mulAdd(thickHalf, corners[7]),
                new Vector3f(tNorm).mulAdd(-thickHalf, corners[4]), color, thickness, false, false, sprite);
    }

    private static void strut(BufferBuilder consumer, Vector3f from, Vector3f to, int color, float thickness,
            boolean startCap, boolean endCap, TextureAtlasSprite sprite) {
        var norm = new Vector3f(to).sub(from).normalize();
        Vector3f prefUp;
        if (Math.abs(from.x - to.x) < 0.01f && Math.abs(from.z - to.z) < 0.01f) {
            prefUp = new Vector3f(1, 0, 0);
        } else {
            prefUp = new Vector3f(0, 1, 0);
        }

        var rightNorm = new Vector3f(norm).cross(prefUp).normalize();
        var leftNorm = new Vector3f(rightNorm).negate();
        var upNorm = new Vector3f(rightNorm).cross(norm).normalize();
        var downNorm = new Vector3f(upNorm).negate();

        var up = new Vector3f(upNorm).mul(thickness * 0.5f);
        var right = new Vector3f(rightNorm).mul(thickness * 0.5f);

        if (startCap) {
            quad(
                    consumer, downNorm, color,
                    new Vector3f(from).add(up).sub(right),
                    new Vector3f(from).sub(up).sub(right),
                    new Vector3f(from).sub(up).add(right),
                    new Vector3f(from).add(up).add(right),
                    sprite);
        }

        if (endCap) {
            quad(
                    consumer, norm, color,
                    new Vector3f(to).add(up).add(right),
                    new Vector3f(to).sub(up).add(right),
                    new Vector3f(to).sub(up).sub(right),
                    new Vector3f(to).add(up).sub(right),
                    sprite);
        }

        quad(
                consumer, leftNorm, color,
                new Vector3f(from).sub(right).add(up),
                new Vector3f(to).sub(right).add(up),
                new Vector3f(to).sub(right).sub(up),
                new Vector3f(from).sub(right).sub(up),
                sprite);
        quad(
                consumer, rightNorm, color,
                new Vector3f(to).add(right).sub(up),
                new Vector3f(to).add(right).add(up),
                new Vector3f(from).add(right).add(up),
                new Vector3f(from).add(right).sub(up),
                sprite);
        quad(
                consumer, upNorm, color,
                new Vector3f(from).add(up).sub(right),
                new Vector3f(from).add(up).add(right),
                new Vector3f(to).add(up).add(right),
                new Vector3f(to).add(up).sub(right),
                sprite);
        quad(
                consumer, downNorm, color,
                new Vector3f(to).sub(up).sub(right),
                new Vector3f(to).sub(up).add(right),
                new Vector3f(from).sub(up).add(right),
                new Vector3f(from).sub(up).sub(right),
                sprite);
    }

    private static void quad(BufferBuilder consumer, Vector3f faceNormal, int color,
            Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
            TextureAtlasSprite sprite) {
        var d = EnumFacing.getFacingFromVector(faceNormal.x, faceNormal.y, faceNormal.z);
        var shade = switch (d) {
            case DOWN -> 0.5F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
            default -> 1.0F;
        };
        color = ARGB.multiply(
                ARGB.color(255, (int) (shade * 255), (int) (shade * 255), (int) (shade * 255)),
                color);

        vertex(consumer, faceNormal, color, v1, sprite.getMinU(), sprite.getMaxV());
        vertex(consumer, faceNormal, color, v2, sprite.getMinU(), sprite.getMinV());
        vertex(consumer, faceNormal, color, v3, sprite.getMaxU(), sprite.getMinV());
        vertex(consumer, faceNormal, color, v4, sprite.getMaxU(), sprite.getMaxV());
    }

    private static void vertex(BufferBuilder consumer,
            Vector3f faceNormal,
            int color,
            Vector3f bottomLeft,
            float u, float v) {
        consumer.pos(bottomLeft.x, bottomLeft.y, bottomLeft.z)
                .tex(u, v)
                .color(ARGB.red(color), ARGB.green(color),
                        ARGB.blue(color), ARGB.alpha(color))
                .normal(faceNormal.x(), faceNormal.y(), faceNormal.z())
                .endVertex();
    }
}
