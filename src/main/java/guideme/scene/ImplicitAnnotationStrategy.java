package guideme.scene;

import guideme.extensions.Extension;
import guideme.extensions.ExtensionPoint;
import guideme.scene.annotation.SceneAnnotation;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a way to generate a {@link SceneAnnotation} on the fly if no explicit annotation could be found under the
 * mouse.
 */
public interface ImplicitAnnotationStrategy extends Extension {
    ExtensionPoint<ImplicitAnnotationStrategy> EXTENSION_POINT = new ExtensionPoint<>(ImplicitAnnotationStrategy.class);

    @Nullable
    SceneAnnotation getAnnotation(GuidebookLevel level, IBlockState blockState, RayTraceResult hitResult);
}
