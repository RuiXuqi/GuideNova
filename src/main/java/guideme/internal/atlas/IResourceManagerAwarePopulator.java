package guideme.internal.atlas;

import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
interface IResourceManagerAwarePopulator extends ITextureMapPopulator {
    void setResourceManager(IResourceManager resourceManager);
}
