package guideme.render;

import guideme.color.ARGB;
import guideme.color.LightDarkMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Helper to build and draw a layer of sprites in a single draw-call.
 */
final class SpriteLayer {
    private final ResourceLocation atlasLocation;
    private final Tessellator tess;
    private BufferBuilder builder;

    public SpriteLayer() {
        atlasLocation = GuiAssets.GUI_SPRITE_ATLAS;
        tess = Tessellator.getInstance();
        builder = tess.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    public void fillSprite(ResourceLocation id, float x, float y, float z, float width, float height, int color) {
        fillSprite(id, x, y, z, width, height, color, SpriteFillDirection.TOP_TO_BOTTOM);
    }

    public void fillSprite(ResourceLocation id, float x, float y, float z, float width, float height, int color,
            SpriteFillDirection fillDirection) {
        // Too large values for width / height cause immediate crashes of the VM due to graphics driver bugs<
        // These maximum values are picked without too much thought.
        width = Math.min(65535, width);
        height = Math.min(65535, height);

        var guiSprite = GuiAssets.sprite(id);
        var scaling = guiSprite.spriteScaling();
        var sprite = guiSprite.atlasSprite(LightDarkMode.current());
        var u0 = sprite.getMinU();
        var u1 = sprite.getMaxU();
        var v0 = sprite.getMinV();
        var v1 = sprite.getMaxV();
        if (scaling instanceof GuiSpriteScaling.Tile(int width1, int height1)) {
            fillTiled(x, y, z, width, height, color, width1, height1, u0, u1, v0, v1, fillDirection);
        } else if (scaling instanceof GuiSpriteScaling.Stretch) {
            addQuad(x, y, z, width, height, color, u0, u1, v0, v1);
        } else if (scaling instanceof GuiSpriteScaling.NineSlice(int width1, int height1, GuiSpriteScaling.NineSlice.Border border)) {
            addTiledNineSlice(id, x, y, z, width, height, color, width1, height1, border, u0, u1, v0, v1);
        }
    }

    private void addTiledNineSlice(ResourceLocation id,
            float x,
            float y,
            float z,
            float width,
            float height,
            int color,
            float nineSliceWidth,
            float nineSliceHeight,
            GuiSpriteScaling.NineSlice.Border border,
            float u0,
            float u1,
            float v0,
            float v1) {

        var leftWidth = Math.min(border.left(), width / 2);
        var rightWidth = Math.min(border.right(), width / 2);
        var topHeight = Math.min(border.top(), height / 2);
        var bottomHeight = Math.min(border.bottom(), height / 2);
        var innerWidth = nineSliceWidth - border.left() - border.right();
        var innerHeight = nineSliceHeight - border.top() - border.bottom();

        // The U/V values for the cuts through the nine-slice we'll use
        var leftU = u0 + leftWidth / nineSliceWidth * (u1 - u0);
        var rightU = u1 - rightWidth / nineSliceWidth * (u1 - u0);
        var topV = v0 + topHeight / nineSliceHeight * (v1 - v0);
        var bottomV = v1 - bottomHeight / nineSliceHeight * (v1 - v0);

        // Destination pixel values of the inner rectangle
        var dstInnerLeft = x + leftWidth;
        var dstInnerTop = y + topHeight;
        var dstInnerRight = x + width - rightWidth;
        var dstInnerBottom = y + height - bottomHeight;
        var dstInnerWidth = dstInnerRight - dstInnerLeft;
        var dstInnerHeight = dstInnerBottom - dstInnerTop;

        // Corners are always untiled, but may be cropped
        addQuad(x, y, z, leftWidth, topHeight, color, u0, leftU, v0, topV); // Top left
        addQuad(dstInnerRight, y, z, rightWidth, topHeight, color, rightU, u1, v0, topV); // Top right
        addQuad(dstInnerRight, dstInnerBottom, z, rightWidth, bottomHeight, color, rightU, u1, bottomV, v1); // Bottom
        // right
        addQuad(x, dstInnerBottom, z, leftWidth, bottomHeight, color, u0, leftU, bottomV, v1); // Bottom left

        // The edges are tiled
        fillTiled(dstInnerLeft, y, z, dstInnerWidth, topHeight, color, innerWidth, border.top(), leftU, rightU, v0,
                topV); // Top Edge
        fillTiled(dstInnerLeft, dstInnerBottom, z, dstInnerWidth, bottomHeight, color, innerWidth, border.bottom(),
                leftU, rightU, bottomV, v1); // Bottom Edge
        fillTiled(x, dstInnerTop, z, leftWidth, dstInnerHeight, color, border.left(), innerHeight, u0, leftU, topV,
                bottomV); // Left Edge
        fillTiled(dstInnerRight, dstInnerTop, z, rightWidth, dstInnerHeight, color, border.right(), innerHeight, rightU,
                u1, topV, bottomV); // Right Edge

        // The center is tiled too
        fillTiled(dstInnerLeft, dstInnerTop, z, dstInnerWidth, dstInnerHeight, color, innerWidth, innerHeight, leftU,
                rightU, topV, bottomV);
    }

    private void fillTiled(float x, float y, float z, float width, float height, int color, float destTileWidth,
            float destTileHeight, float u0, float u1, float v0, float v1) {
        fillTiled(x, y, z, width, height, color, destTileWidth, destTileHeight, u0, u1, v0, v1,
                SpriteFillDirection.TOP_TO_BOTTOM);
    }

    private void fillTiled(float x, float y, float z, float width, float height, int color, float destTileWidth,
            float destTileHeight, float u0, float u1, float v0, float v1, SpriteFillDirection fillDirection) {
        if (destTileWidth <= 0 || destTileHeight <= 0) {
            return;
        }

        var right = x + width;
        var bottom = y + height;

        if (fillDirection == SpriteFillDirection.BOTTOM_TO_TOP) {
            for (var cy = bottom; cy >= y; cy -= destTileHeight) {
                // This handles not stretching the potentially partial last column
                var tileHeight = Math.min(cy - y, destTileHeight);
                var tileV0 = v1 - (v1 - v0) * tileHeight / destTileHeight;

                for (var cx = x; cx < right; cx += destTileWidth) {
                    // This handles not stretching the potentially partial last row
                    var tileWidth = Math.min(right - cx, destTileWidth);
                    var tileU1 = u0 + (u1 - u0) * tileWidth / destTileWidth;

                    addQuad(cx, cy - tileHeight, z, tileWidth, tileHeight, color, u0, tileU1, tileV0, v1);
                }
            }
        } else {
            for (var cy = y; cy < bottom; cy += destTileHeight) {
                // This handles not stretching the potentially partial last column
                var tileHeight = Math.min(bottom - cy, destTileHeight);
                var tileV1 = v0 + (v1 - v0) * tileHeight / destTileHeight;

                for (var cx = x; cx < right; cx += destTileWidth) {
                    // This handles not stretching the potentially partial last row
                    var tileWidth = Math.min(right - cx, destTileWidth);
                    var tileU1 = u0 + (u1 - u0) * tileWidth / destTileWidth;

                    addQuad(cx, cy, z, tileWidth, tileHeight, color, u0, tileU1, v0, tileV1);
                }
            }
        }
    }

    public void addQuad(float x, float y, float z, float width, float height, int color, float minU, float maxU,
            float minV, float maxV) {
        if (width < 0 || height < 0) {
            return;
        }

        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int alpha = ARGB.alpha(color);

        builder.pos(x, y, z).tex(minU, minV).color(red, green, blue, alpha).endVertex();
        builder.pos(x, y + height, z).tex(minU, maxV).color(red, green, blue, alpha).endVertex();
        builder.pos(x + width, y + height, z).tex(maxU, maxV).color(red, green, blue, alpha).endVertex();
        builder.pos(x + width, y, z).tex(maxU, minV).color(red, green, blue, alpha).endVertex();
    }

    public void render(int x, int y, int z) {
        if (builder == null) {
            throw new IllegalStateException("Already rendered.");
        }

        builder = null;
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        Minecraft.getMinecraft().getTextureManager().bindTexture(atlasLocation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        tess.draw();
        GlStateManager.popMatrix();
    }
}
