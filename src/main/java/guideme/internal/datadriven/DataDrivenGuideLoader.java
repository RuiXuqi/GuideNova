package guideme.internal.datadriven;

import com.google.gson.JsonElement;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataDrivenGuideLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DataDrivenGuideLoader.class);

    public static final String ROOT = "guideme_guides";
    public static final String JSON_SUFFIX = ".json";

    private DataDrivenGuideLoader() {
    }

    public static boolean isDefinitionPath(String path) {
        return path.startsWith(ROOT + "/") && path.endsWith(JSON_SUFFIX);
    }

    public static Map<ResourceLocation, DataDrivenGuide> load(ResourceUtil.ScannedResources scannedResources) {
        var guideJsons = scannedResources.scanResources(ROOT, path -> path.endsWith(JSON_SUFFIX));
        var result = new LinkedHashMap<ResourceLocation, DataDrivenGuide>();

        for (var entry : guideJsons.entrySet()) {
            var resourceId = entry.getKey();
            var resource = entry.getValue();

            var guideId = IdUtils.stripPrefixAndSuffix(resourceId, ROOT + "/", JSON_SUFFIX);
            if (guideId == null)
                continue;

            try (var reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                result.put(guideId, DataDrivenGuide.parse(json));
            } catch (JsonParseException e) {
                LOG.error("Failed to load data driven guide {} from {}", guideId, resource.sourcePackId(), e);
            } catch (IOException e) {
                LOG.error("Failed to read data driven guide {} from {}", guideId, resource.sourcePackId(), e);
            }
        }

        return result;
    }

    public static Set<ResourceLocation> collectModels(ResourceUtil.ScannedResources scannedResources) {
        var guideJsons = scannedResources.scanResources(ROOT, path -> path.endsWith(JSON_SUFFIX));
        var result = new LinkedHashSet<ResourceLocation>();

        for (var entry : guideJsons.entrySet()) {
            var resourceId = entry.getKey();
            var resource = entry.getValue();

            var guideId = IdUtils.stripPrefixAndSuffix(resourceId, ROOT + "/", JSON_SUFFIX);
            if (guideId == null)
                continue;

            try (var reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject())
                    throw new JsonParseException("Expected guide definition to be an object");
                JsonParseUtil.getOptionalObject(json.getAsJsonObject(), "item_settings")
                        .flatMap(itemSettings -> JsonParseUtil.getOptionalString(itemSettings, "model"))
                        .map(JsonParseUtil::parseId)
                        .ifPresent(result::add);
            } catch (JsonParseException e) {
                LOG.error("Failed to load guide item model dependency for {} from {}", guideId, resource.sourcePackId(),
                        e);
            } catch (IOException e) {
                LOG.error("Failed to read guide item model dependency for {} from {}", guideId, resource.sourcePackId(),
                        e);
            }
        }

        return result;
    }
}
