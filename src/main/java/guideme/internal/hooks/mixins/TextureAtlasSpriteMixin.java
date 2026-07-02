package guideme.internal.hooks.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import guideme.internal.atlas.GuiAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @WrapOperation(method = "generateMipmaps", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureUtil;generateMipmapData(II[[I)[[I"))
    private int[][] modifyMipmap(int level, int width, int[][] data, Operation<int[][]> original) {
        return (Object) this instanceof GuiAtlasSprite
                ? GuiAtlasSprite.generateRectangularMipmapData(level, this.width, this.height, data)
                : original.call(level, width, data);
    }
}
