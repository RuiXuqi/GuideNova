package guideme.compiler.tags;

import guideme.document.block.LytBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Describes how GuideME discovers and renders one family of recipes.
 * <p>
 * The recipe object is intentionally generic because Minecraft 1.12 only uses {@code IRecipe} for crafting recipes.
 * Machine recipes and compatibility providers usually live in mod-specific registries.
 */
public interface RecipeDisplayMapping<T> {
    /**
     * Stable id for diagnostics, de-duplication, and the optional {@code type} attribute on recipe tags.
     */
    ResourceLocation id();

    /**
     * All recipes currently known for this mapping.
     */
    Iterable<? extends T> recipes();

    /**
     * Return the stable recipe id when one exists.
     */
    @Nullable
    ResourceLocation getRecipeId(T recipe);

    /**
     * Return true when this recipe can produce the requested item stack.
     */
    boolean outputMatches(T recipe, ItemStack target);

    /**
     * Create the layout block for the recipe.
     */
    LytBlock create(T recipe);

    @Nullable
    default LytBlock createById(ResourceLocation recipeId) {
        for (T recipe : recipes()) {
            if (recipeId.equals(getRecipeId(recipe))) {
                return create(recipe);
            }
        }
        return null;
    }

    @Nullable
    default LytBlock createFirstForOutput(ItemStack target) {
        for (T recipe : recipes()) {
            if (outputMatches(recipe, target)) {
                return create(recipe);
            }
        }
        return null;
    }
}
