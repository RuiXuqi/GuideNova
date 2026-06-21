package guideme.compiler.tags;

import guideme.extensions.Extension;
import guideme.extensions.ExtensionPoint;

/**
 * Allows mods to register mappings between recipe type and their custom recipe blocks for use in {@code <RecipeFor/>}
 * and similar tags.
 * <p/>
 * **NOTE:** In addition to being an extension point, implementations of this interface are also retrieved through the
 * Java Service-Loader to enable use of mod recipes cross-guide. Specific instances registered through
 * {@link guideme.GuideBuilder#extension} will have higher priority than instances discovered through service-loader.
 */
public interface RecipeTypeMappingSupplier extends Extension {
    ExtensionPoint<RecipeTypeMappingSupplier> EXTENSION_POINT = new ExtensionPoint<>(RecipeTypeMappingSupplier.class);

    void collect(RecipeTypeMappings mappings);

    interface RecipeTypeMappings {
        <T> void add(RecipeDisplayMapping<T> mapping);
    }
}
