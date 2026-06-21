package guideme.siteexport;

import java.nio.file.Path;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ResourceExporter {
    default void referenceBlock(Block block) {
        referenceItem(new ItemStack(block));
    }

    default void referenceItem(Item item) {
        referenceItem(new ItemStack(item));
    }

    void referenceItem(ItemStack stack);

    void referenceFluid(Fluid fluid);

    String exportTexture(ResourceLocation texture);

    /**
     * @return The new resource id after applying cache busting.
     */
    Path copyResource(ResourceLocation id);

    Path getPathForWriting(ResourceLocation assetId);

    /**
     * Generates a resource location for a page specific resource.
     */
    Path getPageSpecificPathForWriting(String suffix);

    @Nullable
    ResourceLocation getCurrentPageId();

    Path getOutputFolder();

    default String getPathRelativeFromOutputFolder(Path p) {
        return "/" + getOutputFolder().relativize(p).toString().replace('\\', '/');
    }

    /**
     * Generates a resource location for a page specific resource.
     */
    ResourceLocation getPageSpecificResourceLocation(String suffix);

    void referenceRecipe(IRecipe recipe);
}
