package guideme.render;

import guideme.document.LytSize;
import guideme.internal.GuideME;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A texture that is used in the context of a single guide page and is automatically cleared from texture memory when
 * the guide page it was last used on is closed.
 */
public class GuidePageTexture {

    public static GuidePageTexture missing() {
        return new GuidePageTexture(GuideME.makeId("missing"), null);
    }

    private static final Logger LOG = LoggerFactory.getLogger(GuidePageTexture.class);

    // Textures in use by the current page
    private static final Map<GuidePageTexture, DynamicTexture> usedTextures = new IdentityHashMap<>();

    private final ResourceLocation id;

    private final ByteBuffer imageContent;

    private final LytSize size;

    private GuidePageTexture(ResourceLocation id, byte @Nullable [] imageContent) {
        this.id = Objects.requireNonNull(id, "id");
        if (imageContent == null) {
            this.imageContent = null;
            this.size = new LytSize(32, 32);
        } else {
            this.imageContent = ByteBuffer.allocateDirect(imageContent.length);
            this.imageContent.put(imageContent).flip();

            var xOut = new int[1];
            var yOut = new int[1];
            var compOut = new int[1];
            if (!STBImage.stbi_info_from_memory(this.imageContent, xOut, yOut, compOut)) {
                throw new IllegalArgumentException(
                        "Couldn't determine size of image " + id + ": " + STBImage.stbi_failure_reason());
            }

            // Try to determine the size
            this.size = new LytSize(xOut[0], yOut[0]);
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    public LytSize getSize() {
        return size;
    }

    public static GuidePageTexture load(ResourceLocation id, byte[] imageContent) {
        try {
            return new GuidePageTexture(id, imageContent);
        } catch (Exception e) {
            LOG.error("Failed to get image {}: {}", id, e.toString());
            return missing();
        }
    }

    public AbstractTexture use() {
        return usedTextures.computeIfAbsent(this, guidePageTexture -> {
            if (guidePageTexture.imageContent == null) {
                return TextureUtil.MISSING_TEXTURE;
            }

            var xOut = new int[1];
            var yOut = new int[1];
            var compOut = new int[1];
            ByteBuffer decoded = STBImage.stbi_load_from_memory(guidePageTexture.imageContent.asReadOnlyBuffer(),
                    xOut, yOut, compOut, 4);
            if (decoded == null) {
                LOG.error("Failed to read image {}: {}", guidePageTexture.id, STBImage.stbi_failure_reason());
                return TextureUtil.MISSING_TEXTURE;
            }

            try {
                int width = xOut[0];
                int height = yOut[0];
                var dynamicTexture = new DynamicTexture(width, height);
                var pixels = dynamicTexture.getTextureData();

                for (int i = 0; i < width * height; i++) {
                    int r = decoded.get(i * 4) & 0xFF;
                    int g = decoded.get(i * 4 + 1) & 0xFF;
                    int b = decoded.get(i * 4 + 2) & 0xFF;
                    int a = decoded.get(i * 4 + 3) & 0xFF;
                    pixels[i] = a << 24 | r << 16 | g << 8 | b;
                }

                dynamicTexture.updateDynamicTexture();
                return dynamicTexture;
            } finally {
                STBImage.stbi_image_free(decoded);
            }
        });
    }

    public static void releaseUsedTextures() {
        for (DynamicTexture texture : usedTextures.values()) {
            if (texture != TextureUtil.MISSING_TEXTURE) {
                texture.deleteGlTexture();
            }
        }
        usedTextures.clear();
    }
}
