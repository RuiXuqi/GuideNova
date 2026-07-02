package guideme.internal.hooks.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import guideme.internal.atlas.GuiStitcher;
import java.util.Arrays;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Stitcher.class)
public class StitcherMixin {
    @WrapOperation(method = "doStitch", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;sort([Ljava/lang/Object;)V"))
    private void modifySort(Object[] array, Operation<Void> original) {
        if ((Object) this instanceof GuiStitcher) {
            Arrays.sort((Stitcher.Holder[]) array, GuiStitcher.HOLDER_COMPARATOR);
        } else {
            // Cast to (Object) to prevent Java from unrolling the array into varargs.
            original.call((Object) array);
        }
    }

    @Inject(method = "allocateSlot", at = @At("HEAD"), cancellable = true)
    private void redirectAllocate(Stitcher.Holder holder, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof GuiStitcher guiStitcher) {
            cir.setReturnValue(guiStitcher.addToStorage(holder));
        }
    }
}
