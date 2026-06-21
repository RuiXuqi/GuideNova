package guideme.internal.item;

import guideme.compiler.IdUtils;
import guideme.internal.GuideRegistry;
import guideme.internal.datadriven.DataDrivenGuideLoader;
import guideme.internal.util.ResourceUtil;
import java.util.LinkedHashSet;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

@SuppressWarnings("NullableProblems")
public class GuideItemDispatchModelLoader implements ICustomModelLoader {
    static final ResourceLocation MODEL_ID = IdUtils.withPrefix(GuideItem.ID, "item/");
    private static final ResourceLocation ACTUAL_MODEL_ID = IdUtils.withPrefix(MODEL_ID, "models/");
    private static boolean modelLoaded;

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        // guideme:models/item/guide
        return ACTUAL_MODEL_ID.equals(modelLocation);
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) {
        modelLoaded = true;
        // Collect guide models
        var dependencies = new LinkedHashSet<ResourceLocation>();
        dependencies.add(GuideItem.BASE_MODEL_ID);

        // Static guides
        for (var guide : GuideRegistry.getStaticGuides()) {
            guide.getItemSettings().itemModel().ifPresent(model -> {
                if (!MODEL_ID.equals(model)) {
                    dependencies.add(model);
                }
            });
        }

        // Data driven guides
        var scannedResources = ResourceUtil.scanAllResources(DataDrivenGuideLoader::isDefinitionPath);
        dependencies.addAll(DataDrivenGuideLoader.collectModels(scannedResources));

        return new GuideItemDispatchUnbakedModel(dependencies);
    }

    public static boolean isModelLoaded() {
        return modelLoaded;
    }
}
