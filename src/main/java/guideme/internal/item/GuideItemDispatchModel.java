package guideme.internal.item;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraftforge.client.model.BakedModelWrapper;

public class GuideItemDispatchModel extends BakedModelWrapper<IBakedModel> {
    private final ItemOverrideList itemOverrides;

    public GuideItemDispatchModel(IBakedModel originalModel, ItemOverrideList itemOverrides) {
        super(originalModel);
        this.itemOverrides = itemOverrides;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.itemOverrides;
    }
}
