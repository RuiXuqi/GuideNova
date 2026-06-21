package guideme.internal.util;

import guideme.compiler.ParsedGuidePage;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NavigationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NavigationUtil.class);

    private NavigationUtil() {
    }

    public static ItemStack createNavigationIcon(ParsedGuidePage page) {
        var navigation = page.getFrontmatter().navigationEntry();

        var icon = ItemStack.EMPTY;
        if (navigation != null && navigation.iconItemId() != null) {
            var iconItem = ForgeRegistries.ITEMS.getValue(navigation.iconItemId());
            if (iconItem != null) {
                if (navigation.iconNbt() != null) {
                    icon = new ItemStack(iconItem);
                    icon.setTagCompound(navigation.iconNbt());
                } else {
                    icon = new ItemStack(iconItem);
                }
            }

            if (icon.isEmpty()) {
                LOG.error("Couldn't find icon {} for icon of page {}", navigation.iconItemId(), page);
            }
        }

        return icon;
    }
}
