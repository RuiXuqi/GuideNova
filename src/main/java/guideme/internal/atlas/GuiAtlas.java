package guideme.internal.atlas;

import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

/**
 * Hack to impl rectangular sprite loading.
 */
@SideOnly(Side.CLIENT)
public class GuiAtlas extends TextureMap {
    public GuiAtlas(String basePath, @Nullable ITextureMapPopulator iconCreator, boolean skipFirst) {
        super(basePath, iconCreator, skipFirst);
    }

    public GuiAtlas(String basePath, @Nullable ITextureMapPopulator iconCreator) {
        this(basePath, iconCreator, false);
    }

    @Override
    public void loadSprites(IResourceManager resourceManager, ITextureMapPopulator iconCreator) {
        if (iconCreator instanceof IResourceManagerAwarePopulator awarePopulator) {
            awarePopulator.setResourceManager(resourceManager);
        }
        super.loadSprites(resourceManager, iconCreator);
    }

    @Override
    public boolean setTextureEntry(TextureAtlasSprite entry) {
        return entry instanceof GuiAtlasSprite && super.setTextureEntry(entry);
    }
}
