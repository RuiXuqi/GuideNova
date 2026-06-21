package guideme.internal.hooks.mixins;

import guideme.internal.atlas.GuiAtlas;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class ForgeHooksClientMixin {
    // These events are designed vanilla-only, and our customized one should not call them.
    @Inject(method = "onTextureStitchedPre", at = @At("HEAD"), cancellable = true)
    private static void cancelAtlasPre(TextureMap map, CallbackInfo ci) {
        if (map instanceof GuiAtlas)
            ci.cancel();
    }

    @Inject(method = "onTextureStitchedPost", at = @At("HEAD"), cancellable = true)
    private static void cancelAtlasPost(TextureMap map, CallbackInfo ci) {
        if (map instanceof GuiAtlas)
            ci.cancel();
    }
}
