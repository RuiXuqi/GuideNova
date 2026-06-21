package guideme.document.interaction;

import guideme.internal.screen.IndepentScaleScreen;
import guideme.siteexport.ResourceExporter;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ItemTooltip implements GuideTooltip {
    private final ItemStack stack;
    private final ItemStack icon;

    public ItemTooltip(ItemStack stack) {
        this(stack, stack);
    }

    public ItemTooltip(ItemStack stack, ItemStack icon) {
        this.stack = stack;
        this.icon = icon;
    }

    public ItemStack getItem() {
        return stack;
    }

    @Override
    public ItemStack getIcon() {
        return icon;
    }

    @Override
    public List<GuideTooltipComponent> getLines() {
        var lines = IndepentScaleScreen.getTooltipFromItem(Minecraft.getMinecraft(), stack);
        return lines.stream()
                .map(TextTooltipComponent::new)
                .collect(Collectors.toList());
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        exporter.referenceItem(stack);
    }
}
