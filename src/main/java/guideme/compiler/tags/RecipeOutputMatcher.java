package guideme.compiler.tags;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class RecipeOutputMatcher {
    private RecipeOutputMatcher() {
    }

    public static boolean matches(ItemStack output, ItemStack target) {
        if (output == null || output.isEmpty() || target.isEmpty()) {
            return false;
        }
        if (output.getItem() != target.getItem()) {
            return false;
        }
        if (target.getMetadata() != OreDictionary.WILDCARD_VALUE
                && output.getMetadata() != target.getMetadata()) {
            return false;
        }
        return !target.hasTagCompound() || ItemStack.areItemStackTagsEqual(output, target);
    }
}
