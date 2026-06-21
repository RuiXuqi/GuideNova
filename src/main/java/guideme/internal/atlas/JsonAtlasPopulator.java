package guideme.internal.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import guideme.compiler.IdUtils;
import guideme.internal.util.JsonParseUtil;
import guideme.internal.util.ResourceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SideOnly(Side.CLIENT)
public class JsonAtlasPopulator implements IResourceManagerAwarePopulator {
    private static final Logger LOG = LoggerFactory.getLogger(JsonAtlasPopulator.class);
    private static final String TEXTURES_PREFIX = "textures/";
    private static final String PNG_SUFFIX = ".png";

    private final ResourceLocation atlasInfoLocation;
    private IResourceManager resourceManager;

    public JsonAtlasPopulator(ResourceLocation atlasInfoLocation) {
        this.atlasInfoLocation = atlasInfoLocation;
    }

    @Override
    public void setResourceManager(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public void registerSprites(TextureMap textureMap) {
        Objects.requireNonNull(resourceManager,
                "Resource manager must be set before registering JSON atlas sprites");

        var sprites = new LinkedHashMap<ResourceLocation, ResourceLocation>();
        loadAtlas(resourceManager, sprites);

        for (var entry : sprites.entrySet()) {
            textureMap.setTextureEntry(new GuiAtlasSprite(entry.getKey(), entry.getValue()));
        }

        resourceManager = null;
    }

    private void loadAtlas(IResourceManager resourceManager, Map<ResourceLocation, ResourceLocation> sprites) {
        var atlasFile = IdUtils.withPrefixAndSuffix(atlasInfoLocation, "atlases/", ".json");
        var resources = getAtlasResources(resourceManager, atlasFile);
        if (resources == null)
            return;

        var context = new LoadContext(resourceManager);
        for (IResource resource : resources) {
            try (resource;
                    var reader = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                JsonObject root = JsonUtils.getJsonObject(json, "atlas definition");
                JsonArray sources = JsonUtils.getJsonArray(root, "sources");

                for (JsonElement sourceElement : sources) {
                    runSource(context, JsonUtils.getJsonObject(sourceElement, "atlas source"), sprites, resource);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse atlas definition {} from {}", resource.getResourceLocation(),
                        resource.getResourcePackName(), e);
            }
        }
    }

    private static List<IResource> getAtlasResources(IResourceManager resourceManager, ResourceLocation atlasFile) {
        try {
            return resourceManager.getAllResources(atlasFile);
        } catch (IOException e) {
            LOG.warn("Missing atlas definition {}", atlasFile);
            return null;
        }
    }

    private static void runSource(
            LoadContext context, JsonObject source,
            Map<ResourceLocation, ResourceLocation> sprites, IResource atlasResource) {
        var type = JsonUtils.getString(source, "type");
        switch (type) {
            case "single" -> runSingle(context.resourceManager(), source, sprites);
            case "directory" -> runDirectory(context, source, sprites);
            case "filter" -> runFilter(source, sprites);
            default -> LOG.warn("Unsupported atlas source type '{}' in {} from {}",
                    type, atlasResource.getResourceLocation(), atlasResource.getResourcePackName());
        }
    }

    private static void runSingle(
            IResourceManager resourceManager, JsonObject source,
            Map<ResourceLocation, ResourceLocation> sprites) {
        var resourceId = parseId(source, "resource");
        var spriteId = source.has("sprite") ? parseId(source, "sprite") : resourceId;
        var textureLocation = GuiAtlasSprite.textureLocation(resourceId);

        if (!ResourceUtil.resourceExists(resourceManager, textureLocation)) {
            LOG.warn("Missing sprite: {}", textureLocation);
            return;
        }

        sprites.put(spriteId, resourceId);
    }

    private static void runDirectory(
            LoadContext context, JsonObject source,
            Map<ResourceLocation, ResourceLocation> sprites) {
        var sourcePath = JsonUtils.getString(source, "source");
        var idPrefix = JsonUtils.getString(source, "prefix");
        var textureRoot = sourcePath.isEmpty() ? "textures" : TEXTURES_PREFIX + sourcePath;

        var resources = context.textureResources().scanResources(textureRoot, path -> path.endsWith(PNG_SUFFIX));
        for (var textureLocation : resources.keySet()) {
            var spriteResource = IdUtils.stripPrefixAndSuffix(textureLocation, TEXTURES_PREFIX, PNG_SUFFIX);
            if (spriteResource == null)
                continue;

            var spriteIdPath = sourcePath.isEmpty() ? spriteResource
                    : IdUtils.stripPrefix(spriteResource, sourcePath + "/");
            if (spriteIdPath == null)
                continue;

            var spriteId = IdUtils.withPath(spriteIdPath, idPrefix + spriteIdPath.getPath());
            sprites.put(spriteId, spriteResource);
        }
    }

    private static void runFilter(JsonObject source, Map<ResourceLocation, ResourceLocation> sprites) {
        var pattern = JsonUtils.getJsonObject(source, "pattern");
        var namespacePattern = getPattern(pattern, "namespace");
        var pathPattern = getPattern(pattern, "path");

        sprites.keySet().removeIf(
                spriteId -> (namespacePattern == null || namespacePattern.matcher(spriteId.getNamespace()).find())
                        && (pathPattern == null || pathPattern.matcher(spriteId.getPath()).find()));
    }

    private static ResourceLocation parseId(JsonObject json, String memberName) {
        return JsonParseUtil.parseId(JsonUtils.getString(json, memberName));
    }

    private static Pattern getPattern(JsonObject json, String memberName) {
        if (!json.has(memberName))
            return null;

        try {
            return Pattern.compile(JsonUtils.getString(json, memberName));
        } catch (PatternSyntaxException e) {
            throw new JsonParseException("Invalid atlas filter pattern '" + memberName + "'", e);
        }
    }

    private static class LoadContext {
        private final IResourceManager resourceManager;
        private ResourceUtil.ScannedResources textureResources;

        private LoadContext(IResourceManager resourceManager) {
            this.resourceManager = resourceManager;
        }

        private IResourceManager resourceManager() {
            return resourceManager;
        }

        private ResourceUtil.ScannedResources textureResources() {
            if (textureResources == null) {
                textureResources = ResourceUtil
                        .scanAllResources(path -> path.startsWith(TEXTURES_PREFIX) && path.endsWith(PNG_SUFFIX));
            }
            return textureResources;
        }
    }
}
