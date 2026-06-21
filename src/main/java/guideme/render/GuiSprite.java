package guideme.render;

import guideme.color.LightDarkMode;
import guideme.compiler.IdUtils;
import guideme.internal.GuideMEClient;
import guideme.internal.atlas.GuiAtlasSprite;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuiSprite {

    private static final Logger LOG = LoggerFactory.getLogger(GuiSprite.class);

    private final ResourceLocation id;
    private volatile CachedState cachedState;

    public GuiSprite(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation get(LightDarkMode mode) {
        var state = getOrCreateCachedState();
        return mode == LightDarkMode.DARK_MODE ? state.darkId : id;
    }

    public GuiSpriteScaling spriteScaling() {
        var state = getOrCreateCachedState();
        return state.spriteScaling;
    }

    public TextureAtlasSprite atlasSprite(LightDarkMode mode) {
        var state = getOrCreateCachedState();
        return mode == LightDarkMode.LIGHT_MODE ? state.sprite : state.darkSprite;
    }

    private CachedState getOrCreateCachedState() {
        // Double-checked locking
        var result = cachedState;
        if (result != null) {
            return result;
        }

        synchronized (this) {
            var guiSprites = GuideMEClient.GUI_ATLAS;

            var sprite = guiSprites.getAtlasSprite(id.toString());
            var spriteScaling = getSpriteScaling(id, sprite);
            var darkId = IdUtils.withSuffix(id, "_darkmode");
            var darkSprite = guiSprites.getAtlasSprite(darkId.toString());

            if (darkSprite.getIconName().equals(guiSprites.getMissingSprite().getIconName())) {
                // Use the light sprite as the dark sprite
                darkId = id;
                darkSprite = sprite;
            } else {
                // Ensure people avoid the foot-gun of using different scaling
                var darkScaling = getSpriteScaling(darkId, sprite);
                if (!darkScaling.equals(spriteScaling)) {
                    LOG.warn(
                            "Dark-mode sprite {} uses different sprite-scaling from the light-mode version. Please ensure the same .mcmeta file content is used.",
                            darkId);
                }
            }

            cachedState = new CachedState(
                    sprite,
                    darkId,
                    darkSprite,
                    spriteScaling);
            return cachedState;
        }
    }

    private static GuiSpriteScaling getSpriteScaling(ResourceLocation id, TextureAtlasSprite sprite) {
        var location = sprite instanceof GuiAtlasSprite atlasSprite ? atlasSprite.getTextureLocation()
                : IdUtils.withPrefixAndSuffix(id, "textures/gui/sprites/", ".png");

        try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
            GuiSpriteScaling scaling = resource.getMetadata(GuiSpriteScaling.SECTION_NAME);
            return scaling != null ? scaling : GuiSpriteScaling.DEFAULT;
        } catch (IOException e) {
            LOG.error("Failed to load metadata for {}", id, e);
            return GuiSpriteScaling.DEFAULT;
        } catch (Exception e) {
            LOG.error("Failed to read sprite scaling for {}", id, e);
            return GuiSpriteScaling.DEFAULT;
        }
    }

    private record CachedState(
            TextureAtlasSprite sprite,
            ResourceLocation darkId,
            TextureAtlasSprite darkSprite,
            // We always use the same sprite scaling for light and dark mode
            GuiSpriteScaling spriteScaling) {
    }

    void reset() {
        cachedState = null;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
