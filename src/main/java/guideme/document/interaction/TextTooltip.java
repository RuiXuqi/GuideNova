package guideme.document.interaction;

import guideme.siteexport.ResourceExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TextTooltip implements GuideTooltip {
    private final List<GuideTooltipComponent> lines;

    public TextTooltip(List<String> lines) {
        this.lines = lines.stream().map(TextTooltipComponent::new).collect(Collectors.toList());
    }

    public TextTooltip(String text) {
        this(List.of(text));
    }

    public TextTooltip(String firstLine, String... additionalLines) {
        this(makeLineList(firstLine, additionalLines));
    }

    private static List<String> makeLineList(String firstLine, String[] additionalLines) {
        var lines = new ArrayList<String>(1 + additionalLines.length);
        lines.add(firstLine);
        Collections.addAll(lines, additionalLines);
        return lines;
    }

    @Override
    public List<GuideTooltipComponent> getLines() {
        return lines;
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
    }
}
