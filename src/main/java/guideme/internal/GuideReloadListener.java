package guideme.internal;

import guideme.Guide;
import guideme.color.SymbolicColorResolver;
import guideme.compiler.IdUtils;
import guideme.compiler.PageCompiler;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.datadriven.DataDrivenGuideLoader;
import guideme.internal.util.LangUtil;
import guideme.internal.util.ResourceUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuideReloadListener implements ISelectiveResourceReloadListener {
    private static final Logger LOG = LoggerFactory.getLogger(GuideReloadListener.class);

    private static final String MARKDOWN_SUFFIX = ".md";

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (!resourcePredicate.test(VanillaResourceType.LANGUAGES))
            return;
        var guidePages = new HashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();

        String language = LangUtil.getCurrentLanguage();
        if (GuideMEClient.isIgnoreTranslatedGuides()) {
            language = null;
        }

        // Load available languages to know which can be ignored
        var languages = getAllLanguages();
        // Only scan huge once to improve performance
        var scannedResources = ResourceUtil.scanAllResources(path ->
        // Data driven guide jsons. There will be tons of jsons so we need a filter
        DataDrivenGuideLoader.isDefinitionPath(path)
                // Guide mds
                || path.endsWith(MARKDOWN_SUFFIX));

        // Discover data driven guides now
        var dataDrivenGuides = loadDataDrivenGuides(scannedResources);

        // Reload pages for data-driven guides first
        for (var guide : dataDrivenGuides.values()) {
            guidePages.put(guide.getId(), loadPages(scannedResources, guide.getContentRootFolder(),
                    guide.getDefaultLanguage(), language, languages));
        }
        for (var guide : GuideRegistry.getStaticGuides()) {
            if (!guidePages.containsKey(guide.getId())) {
                guidePages.put(guide.getId(), loadPages(scannedResources, guide.getContentRootFolder(),
                        guide.getDefaultLanguage(), language, languages));
            }
        }

        // Apply resources
        LOG.info("Data driven guides: {}", dataDrivenGuides.keySet());

        GuideRegistry.setDataDriven(dataDrivenGuides);

        for (var guide : GuideRegistry.getAll()) {
            var pagesForGuide = guidePages.getOrDefault(guide.getId(), Map.of());
            guide.setPages(pagesForGuide);
        }
    }

    /**
     * This code is copied from the MC language manager to retrieve the list of all supported languages. Cannot use
     * {@link net.minecraft.client.resources.LanguageManager#getLanguages()} here because it is updated after resource
     * manager listeners have already run.
     *
     * @see net.minecraft.client.resources.LanguageManager#parseLanguageMetadata(List)
     */
    private static Set<String> getAllLanguages() {
        var result = new HashSet<String>();
        var serializer = Minecraft.getMinecraft().metadataSerializer;
        for (IResourcePack pack : ResourceUtil.getActiveResourcePacks()) {
            try {
                LanguageMetadataSection section = pack.getPackMetadata(serializer, "language");
                if (section != null) {
                    for (Language language : section.getLanguages()) {
                        result.add(language.getLanguageCode());
                    }
                }
            } catch (Exception ignored) {
                // Minecraft itself will already warn about this
            }
        }
        return result;
    }

    private static Map<ResourceLocation, MutableGuide> loadDataDrivenGuides(
            ResourceUtil.ScannedResources scannedResources) {
        var guideSpecs = DataDrivenGuideLoader.load(scannedResources);
        var dataDrivenGuides = new HashMap<ResourceLocation, MutableGuide>();

        for (var entry : guideSpecs.entrySet()) {
            var guideId = entry.getKey();
            var guideSpec = entry.getValue();

            var builder = Guide.builder(guideId)
                    .register(false)
                    .itemSettings(guideSpec.itemSettings())
                    .defaultLanguage(guideSpec.defaultLanguage());

            if (!guideSpec.customColors().isEmpty()) {
                builder.extension(SymbolicColorResolver.EXTENSION_POINT, guideSpec.customColors()::get);
            }

            var guide = (MutableGuide) builder
                    .build();
            dataDrivenGuides.put(guideId, guide);
        }
        return dataDrivenGuides;
    }

    private static Map<ResourceLocation, ParsedGuidePage> loadPages(
            ResourceUtil.ScannedResources scannedResources,
            String contentRoot,
            String defaultLanguage, @Nullable String currentLanguage, Set<String> languages) {
        var pagesForGuide = new HashMap<ResourceLocation, ParsedGuidePage>();

        var resources = scannedResources.scanResources(contentRoot, location -> location.endsWith(MARKDOWN_SUFFIX));

        for (var entry : resources.entrySet()) {
            var resourceId = entry.getKey();
            // Eg: ae2:guides/ae2/main/intro/index.md under contentRoot "guides/ae2/main"
            // becomes the guide-local page id ae2:intro/index.md.
            var pageId = IdUtils.stripPrefix(resourceId, contentRoot + "/");
            if (pageId == null)
                continue;

            var resource = entry.getValue();

            if (LangUtil.getLangFromPageId(pageId, languages) != null) {
                continue; // Skip translated pages.
            }

            // Check for translated versions of this page.
            String language = defaultLanguage;
            if (currentLanguage != null) {
                // Eg: page id ae2:intro/index.md + language zh_cn becomes ae2:intro/index.zh_cn.md,
                // then gets mapped back to the scanned resource id ae2:guides/ae2/main/intro/index.zh_cn.md.
                var translatedResourceId = IdUtils.withPrefix(LangUtil.getTranslatedAsset(pageId, currentLanguage),
                        contentRoot + "/");
                var translatedPage = resources.get(translatedResourceId);
                if (translatedPage != null) {
                    language = currentLanguage;
                    resource = translatedPage;
                }
            }

            String sourcePackId = resource.sourcePackId();
            try (var in = resource.open()) {
                pagesForGuide.put(pageId, PageCompiler.parse(sourcePackId, language, pageId, in));
            } catch (IOException e) {
                LOG.error("Failed to load guidebook page {} from {}", pageId, resource.sourcePackId(), e);
            }
        }

        return pagesForGuide;
    }
}
