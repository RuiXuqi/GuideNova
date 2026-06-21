package guideme.internal.hooks.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import guideme.internal.atlas.GuiAtlas;
import guideme.internal.atlas.GuiAtlasSprite;
import guideme.internal.atlas.GuiStitcher;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureMap.class)
public class TextureMapMixin {
    @Redirect(method = "loadTextureAtlas", at = @At(value = "NEW", target = "(IIII)Lnet/minecraft/client/renderer/texture/Stitcher;"))
    private Stitcher redirectStitcher(int maxWidth, int maxHeight, int maxTileDimension, int mipmapLevel) {
        return (Object) this instanceof GuiAtlas ? new GuiStitcher(maxWidth, maxHeight, maxTileDimension, mipmapLevel)
                : new Stitcher(maxWidth, maxHeight, maxTileDimension, mipmapLevel);
    }

    @WrapOperation(method = "registerSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;makeAtlasSprite(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite redirectSprite(ResourceLocation spriteId, Operation<TextureAtlasSprite> original) {
        return (Object) this instanceof GuiAtlas ? GuiAtlasSprite.makeAtlasSprite(spriteId) :
        // The method is protected, so use wrap to avoid new ATs
                original.call(spriteId);
    }

    @Inject(method = "getResourceLocation", at = @At("HEAD"), cancellable = true)
    private void redirectTexture(TextureAtlasSprite sprite, CallbackInfoReturnable<ResourceLocation> cir) {
        if (sprite instanceof GuiAtlasSprite guiSprite) {
            cir.setReturnValue(guiSprite.getTextureLocation());
        }
    }
}
