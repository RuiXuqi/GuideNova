package guideme.internal.hooks.mixins;

import guideme.internal.atlas.GuiAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Redirect(method = "generateMipmaps", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureUtil;generateMipmapData(II[[I)[[I"))
    private int[][] redirectMipmap(int level, int width, int[][] data) {
        return (Object) this instanceof GuiAtlasSprite
                ? GuiAtlasSprite.generateRectangularMipmapData(level, this.width, this.height, data)
                : TextureUtil.generateMipmapData(level, width, data);
    }
}
