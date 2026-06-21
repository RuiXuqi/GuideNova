package guideme.document.interaction;

import guideme.document.block.LytBlock;
import guideme.siteexport.ExportableResourceProvider;
import guideme.siteexport.ResourceExporter;
import java.util.List;

/**
 * A {@link GuideTooltip} that renders a {@link LytBlock} as the tooltip content.
 */
public class ContentTooltip implements GuideTooltip {
    private final List<GuideTooltipComponent> lines;

    private final LytBlock content;

    public ContentTooltip(LytBlock content) {
        this.content = content;
        this.lines = List.of(new ContentTooltipComponent(content));
    }

    @Override
    public List<GuideTooltipComponent> getLines() {
        return lines;
    }

    public LytBlock getContent() {
        return content;
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        ExportableResourceProvider.visit(content, exporter);
    }
}
