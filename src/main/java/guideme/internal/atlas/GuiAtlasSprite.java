package guideme.internal.atlas;

import guideme.compiler.IdUtils;
import java.io.IOException;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiAtlasSprite extends TextureAtlasSprite {
    private final ResourceLocation resourceId;

    protected GuiAtlasSprite(ResourceLocation spriteId, ResourceLocation resourceId) {
        super(spriteId.toString());
        this.resourceId = resourceId;
    }

    private GuiAtlasSprite(ResourceLocation spriteId) {
        this(spriteId, spriteId);
    }

    public static TextureAtlasSprite makeAtlasSprite(ResourceLocation spriteId) {
        return new GuiAtlasSprite(spriteId);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation(resourceId);
    }

    public static ResourceLocation textureLocation(ResourceLocation resourceId) {
        return IdUtils.withPrefixAndSuffix(resourceId, "textures/", ".png");
    }

    @Override
    public void loadSprite(PngSizeInfo sizeInfo, boolean hasAnimation) throws IOException {
        try {
            super.loadSprite(sizeInfo, hasAnimation);
        } catch (RuntimeException ignored) {
            // We only catch "broken" textures
        }
    }

    /// Internal method
    public static int[][] generateRectangularMipmapData(int level, int width, int height, int[][] data) {
        int[][] result = new int[level + 1][];
        result[0] = data[0];

        for (int mip = 1; mip <= level; mip++) {
            if (data.length > mip && data[mip] != null) {
                result[mip] = data[mip];
                continue;
            }

            int[] previous = result[mip - 1];
            int previousWidth = Math.max(1, width >> (mip - 1));
            int previousHeight = Math.max(1, height >> (mip - 1));
            int mipWidth = Math.max(1, width >> mip);
            int mipHeight = Math.max(1, height >> mip);
            int[] current = new int[mipWidth * mipHeight];

            for (int y = 0; y < mipHeight; y++) {
                for (int x = 0; x < mipWidth; x++) {
                    int x0 = Math.min(previousWidth - 1, x * 2);
                    int y0 = Math.min(previousHeight - 1, y * 2);
                    int x1 = Math.min(previousWidth - 1, x0 + 1);
                    int y1 = Math.min(previousHeight - 1, y0 + 1);
                    current[x + y * mipWidth] = blendColors(
                            previous[x0 + y0 * previousWidth], previous[x1 + y0 * previousWidth],
                            previous[x0 + y1 * previousWidth], previous[x1 + y1 * previousWidth]);
                }
            }

            result[mip] = current;
        }

        return result;
    }

    private static int blendColors(int c0, int c1, int c2, int c3) {
        int alpha = averageComponent(c0, c1, c2, c3, 24);
        int red = averageComponent(c0, c1, c2, c3, 16);
        int green = averageComponent(c0, c1, c2, c3, 8);
        int blue = averageComponent(c0, c1, c2, c3, 0);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int averageComponent(int c0, int c1, int c2, int c3, int shift) {
        return ((c0 >> shift & 0xFF)
                + (c1 >> shift & 0xFF)
                + (c2 >> shift & 0xFF)
                + (c3 >> shift & 0xFF)) / 4;
    }
}
