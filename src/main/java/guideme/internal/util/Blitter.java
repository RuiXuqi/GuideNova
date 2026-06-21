/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package guideme.internal.util;

import guideme.color.ARGB;
import guideme.internal.GuideME;
import guideme.ui.UiRect;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Utility class for drawing rectangular textures in the UI.
 */
@SideOnly(Side.CLIENT)
public final class Blitter {

    // This assumption is obviously bogus, but currently all textures are this size,
    // and it's impossible to get the texture size from an already loaded texture.
    // The coordinates will still be correct when a resource pack provides bigger textures as long
    // as each texture element is still positioned at the same relative position
    public static final int DEFAULT_TEXTURE_WIDTH = 256;
    public static final int DEFAULT_TEXTURE_HEIGHT = 256;

    private final ResourceLocation texture;
    // This texture size is only used to convert the source rectangle into uv coordinates (which are [0,1] and work
    // with textures of any size at runtime).
    private final int referenceWidth;
    private final int referenceHeight;
    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;
    private UiRect srcRect;
    private boolean hasUv;
    private float minU;
    private float minV;
    private float maxU;
    private float maxV;
    private UiRect destRect = new UiRect(0, 0, 0, 0);
    private boolean blending = true;
    private TextureTransform transform = TextureTransform.NONE;
    private int zOffset;

    Blitter(ResourceLocation texture, int referenceWidth, int referenceHeight) {
        this.texture = texture;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
    }

    /**
     * Creates a blitter where the source rectangle is in relation to a 256x256 pixel texture.
     */
    public static Blitter texture(ResourceLocation file) {
        return texture(file, DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT);
    }

    /**
     * Creates a blitter where the source rectangle is in relation to a 256x256 pixel texture.
     */
    public static Blitter texture(String file) {
        return texture(file, DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT);
    }

    /**
     * Creates a blitter where the source rectangle is in relation to a texture of the given size.
     */
    public static Blitter texture(ResourceLocation file, int referenceWidth, int referenceHeight) {
        return new Blitter(file, referenceWidth, referenceHeight);
    }

    /**
     * Creates a blitter where the source rectangle is in relation to a texture of the given size.
     */
    public static Blitter texture(String file, int referenceWidth, int referenceHeight) {
        return new Blitter(GuideME.makeId("textures/" + file), referenceWidth, referenceHeight);
    }

    /**
     * Creates a blitter from a texture atlas sprite.
     */
    public static Blitter sprite(ResourceLocation atlasLocation, TextureAtlasSprite sprite) {
        return new Blitter(atlasLocation, 1, 1)
                .src(0, 0, sprite.getIconWidth(), sprite.getIconHeight())
                .uv(sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV());
    }

    public Blitter copy() {
        Blitter result = new Blitter(texture, referenceWidth, referenceHeight);
        result.srcRect = srcRect;
        result.hasUv = hasUv;
        result.minU = minU;
        result.minV = minV;
        result.maxU = maxU;
        result.maxV = maxV;
        result.destRect = destRect;
        result.r = r;
        result.g = g;
        result.b = b;
        result.a = a;
        return result;
    }

    /**
     * Use the given rectangle from the texture (in pixels assuming a 256x256 texture size).
     */
    public Blitter src(int x, int y, int w, int h) {
        this.srcRect = new UiRect(x, y, w, h);
        this.hasUv = false;
        return this;
    }

    private Blitter uv(float minU, float minV, float maxU, float maxV) {
        this.hasUv = true;
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
        return this;
    }

    public Blitter srcWidth(int w) {
        this.srcRect = new UiRect(srcRect.getX(), srcRect.getY(), w, srcRect.getHeight());
        return this;
    }

    public Blitter srcHeight(int h) {
        this.srcRect = new UiRect(srcRect.getX(), srcRect.getY(), srcRect.getWidth(), h);
        return this;
    }

    /**
     * Use the given rectangle from the texture (in pixels assuming a 256x256 texture size).
     */
    public Blitter src(UiRect rect) {
        return src(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    /**
     * Draw into the rectangle defined by the given coordinates.
     */
    public Blitter dest(int x, int y, int w, int h) {
        this.destRect = new UiRect(x, y, w, h);
        return this;
    }

    /**
     * Draw at the given x,y coordinate and use the source rectangle size as the destination rectangle size.
     */
    public Blitter dest(int x, int y) {
        return dest(x, y, 0, 0);
    }

    /**
     * Draw into the given rectangle.
     */
    public Blitter dest(UiRect rect) {
        return dest(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public UiRect getDestRect() {
        int x = destRect.getX();
        int y = destRect.getY();
        int w = 0, h = 0;
        if (destRect.getWidth() != 0 && destRect.getHeight() != 0) {
            w = destRect.getWidth();
            h = destRect.getHeight();
        } else if (srcRect != null) {
            w = srcRect.getWidth();
            h = srcRect.getHeight();
        }
        return new UiRect(x, y, w, h);
    }

    public Blitter color(float r, float g, float b) {
        this.r = (int) (MathHelper.clamp(r, 0, 1) * 255);
        this.g = (int) (MathHelper.clamp(g, 0, 1) * 255);
        this.b = (int) (MathHelper.clamp(b, 0, 1) * 255);
        return this;
    }

    public Blitter colorArgb(int packedArgb) {
        this.a = ARGB.alpha(packedArgb);
        this.r = ARGB.red(packedArgb);
        this.g = ARGB.green(packedArgb);
        this.b = ARGB.blue(packedArgb);
        return this;
    }

    public Blitter opacity(float a) {
        this.a = (int) (MathHelper.clamp(a, 0, 1) * 255);
        return this;
    }

    public Blitter color(float r, float g, float b, float a) {
        return color(r, g, b).opacity(a);
    }

    public Blitter transform(TextureTransform transform) {
        this.transform = Objects.requireNonNull(transform);
        return this;
    }

    /**
     * Enables or disables alpha-blending. If disabled, all pixels of the texture will be drawn as opaque, and the alpha
     * value set using {@link #opacity(float)} will be ignored.
     */
    public Blitter blending(boolean enable) {
        this.blending = enable;
        return this;
    }

    /**
     * Sets the color to the R,G,B values encoded in the lower 24-bit of the given integer.
     */
    public Blitter colorRgb(int packedRgb) {
        float r = (packedRgb >> 16 & 255) / 255.0F;
        float g = (packedRgb >> 8 & 255) / 255.0F;
        float b = (packedRgb & 255) / 255.0F;

        return color(r, g, b);
    }

    public Blitter zOffset(int offset) {
        this.zOffset = offset;
        return this;
    }

    public void blit() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(this.texture);

        // With no source rectangle, we'll use the entirety of the texture. This happens rarely though.
        float minU, minV, maxU, maxV;
        if (hasUv) {
            minU = this.minU;
            minV = this.minV;
            maxU = this.maxU;
            maxV = this.maxV;
        } else if (srcRect == null) {
            minU = minV = 0;
            maxU = maxV = 1;
        } else {
            minU = srcRect.getX() / (float) referenceWidth;
            minV = srcRect.getY() / (float) referenceHeight;
            maxU = (srcRect.getX() + srcRect.getWidth()) / (float) referenceWidth;
            maxV = (srcRect.getY() + srcRect.getHeight()) / (float) referenceHeight;
        }

        // Transform the UV
        switch (transform) {
            case MIRROR_H -> {
                var tmp = minU;
                minU = maxU;
                maxU = tmp;
            }
            case MIRROR_V -> {
                var tmp = minV;
                minV = maxV;
                maxV = tmp;
            }
        }

        // It's possible to not set a destination rectangle size, in which case the
        // source rectangle size will be used
        float x1 = destRect.getX();
        float y1 = destRect.getY();
        float x2 = x1, y2 = y1;
        if (destRect.getWidth() != 0 && destRect.getHeight() != 0) {
            x2 += destRect.getWidth();
            y2 += destRect.getHeight();
        } else if (srcRect != null) {
            x2 += srcRect.getWidth();
            y2 += srcRect.getHeight();
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tess.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x1, y2, 0)
                .tex(minU, maxV)
                .color(r, g, b, a)
                .endVertex();
        bufferbuilder.pos(x2, y2, 0)
                .tex(maxU, maxV)
                .color(r, g, b, a)
                .endVertex();
        bufferbuilder.pos(x2, y1, 0)
                .tex(maxU, minV)
                .color(r, g, b, a)
                .endVertex();
        bufferbuilder.pos(x1, y1, 0)
                .tex(minU, minV)
                .color(r, g, b, a)
                .endVertex();

        if (blending) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            GlStateManager.disableBlend();
        }
        tess.draw();
    }

}
