package guideme.navigation;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record NavigationNode(
        @Nullable ResourceLocation pageId,
        String title,
        ItemStack icon,
        List<NavigationNode> children,
        int position,
        boolean hasPage) {
}
