package guideme.document.interaction;

import guideme.siteexport.ExportableResourceProvider;
import java.util.List;
import net.minecraft.item.ItemStack;

public interface GuideTooltip extends ExportableResourceProvider {

    default ItemStack getIcon() {
        return ItemStack.EMPTY;
    }

    List<GuideTooltipComponent> getLines();

}
