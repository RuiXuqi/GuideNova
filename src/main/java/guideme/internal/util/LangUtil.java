package guideme.internal.util;

import guideme.compiler.IdUtils;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class LangUtil {
    private LangUtil() {
    }

    public static Set<String> getValidLanguages() {
        var client = Minecraft.getMinecraft();
        if (client != null) {
            return client.getLanguageManager().getLanguages().stream()
                    .map(Language::getLanguageCode)
                    .collect(Collectors.toSet());
        }
        return Set.of("en_us");
    }

    public static String getCurrentLanguage() {
        var client = Minecraft.getMinecraft();
        if (client != null) {
            // Sometimes inexplicably, the language code is actually "en_US" instead of the minecraft default (en_us).
            // ResourceLocations crash for non-lowercase path components, so we ensure we're not crashing later
            // by forcing lowercase here.
            return client.getLanguageManager().getCurrentLanguage().getLanguageCode().toLowerCase(Locale.ROOT);
        }
        return "en_us";
    }

    public static ResourceLocation getTranslatedAsset(ResourceLocation assetId, String language) {
        return IdUtils.withPrefix(assetId, "_" + language + "/");
    }

    public static ResourceLocation stripLangFromPageId(ResourceLocation pageId, Set<String> supportedLanguages) {
        String path = pageId.getPath();

        int firstSep = path.indexOf("/");
        if (firstSep == -1) {
            return pageId; // No directory, bare filename
        }

        if (path.charAt(0) != '_') {
            return pageId; // First folder doesn't start with "_"
        }

        // There has to be content after the slash since empty paths are not allowed
        if (firstSep + 1 >= path.length()) {
            return pageId;
        }

        var potentialLanguage = path.substring(1, firstSep);
        if (supportedLanguages.contains(potentialLanguage)) {
            return IdUtils.withPath(pageId, path.substring(firstSep + 1));
        }

        return pageId;
    }

    @Nullable
    public static String getLangFromPageId(ResourceLocation pageId, Set<String> supportedLanguages) {
        String path = pageId.getPath();

        int firstSep = path.indexOf("/");
        if (firstSep == -1) {
            return null; // No directory, bare filename
        }

        if (path.charAt(0) != '_') {
            return null; // First folder doesn't start with "_"
        }

        // There has to be content after the slash since empty paths are not allowed
        if (firstSep + 1 >= path.length()) {
            return null;
        }

        var potentialLanguage = path.substring(1, firstSep);
        if (supportedLanguages.contains(potentialLanguage)) {
            return potentialLanguage;
        }

        return null;
    }

}
