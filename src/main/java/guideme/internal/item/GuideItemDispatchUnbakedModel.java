package guideme.internal.item;

import guideme.internal.GuideRegistry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullableProblems")
public class GuideItemDispatchUnbakedModel implements IModel {
    private final Set<ResourceLocation> dependencies;

    public GuideItemDispatchUnbakedModel(Set<ResourceLocation> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return dependencies;
    }

    @Override
    public IBakedModel bake(
            IModelState modelState, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> sprites) {
        var baseModel = bakeModel(GuideItem.BASE_MODEL_ID, modelState, format, sprites);

        var guideModels = new HashMap<ResourceLocation, IBakedModel>();
        for (var modelId : dependencies) {
            if (!GuideItem.BASE_MODEL_ID.equals(modelId)) {
                guideModels.put(modelId, bakeModel(modelId, modelState, format, sprites));
            }
        }

        var overrides = new ItemOverrideList(List.of()) {
            @Override
            public IBakedModel handleItemState(
                    IBakedModel originalModel, ItemStack stack,
                    @Nullable World world, @Nullable EntityLivingBase entity) {
                var guideId = GuideItem.getGuideId(stack);
                if (guideId != null) {
                    var guide = GuideRegistry.getById(guideId);
                    if (guide != null && guide.getItemSettings().itemModel().isPresent()) {
                        var model = guideModels.get(guide.getItemSettings().itemModel().get());
                        if (model != null) {
                            return model;
                        }
                    }
                }

                return baseModel;
            }
        };

        return new GuideItemDispatchModel(baseModel, overrides);
    }

    private static IBakedModel bakeModel(
            ResourceLocation modelId, IModelState modelState,
            VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> sprites) {
        return ModelLoaderRegistry.getModelOrLogError(
                modelId,
                "Failed to load GuideME guide item model " + modelId + ".")
                .bake(modelState, format, sprites);
    }
}
