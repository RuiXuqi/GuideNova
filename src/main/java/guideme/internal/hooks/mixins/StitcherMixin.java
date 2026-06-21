package guideme.internal.hooks.mixins;

import guideme.internal.atlas.GuiStitcher;
import java.util.Arrays;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Stitcher.class)
public class StitcherMixin {
    @Redirect(method = "doStitch", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;sort([Ljava/lang/Object;)V"))
    private void redirectSort(Object[] array) {
        if ((Object) this instanceof GuiStitcher) {
            Arrays.sort((Stitcher.Holder[]) array, GuiStitcher.HOLDER_COMPARATOR);
        } else {
            Arrays.sort(array);
        }
    }

    @Inject(method = "allocateSlot", at = @At("HEAD"), cancellable = true)
    private void redirectAllocate(Stitcher.Holder holder, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof GuiStitcher guiStitcher) {
            cir.setReturnValue(guiStitcher.addToStorage(holder));
        }
    }
}
