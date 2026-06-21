package guideme.document.flow;

import guideme.compiler.PageCompiler;
import guideme.document.LytErrorSink;
import guideme.libs.unist.UnistNode;
import net.minecraft.util.text.ITextComponent;

public interface LytFlowParent extends LytErrorSink {
    void append(LytFlowContent child);

    default LytFlowText appendText(String text) {
        var node = new LytFlowText();
        node.setText(text);
        append(node);
        return node;
    }

    /**
     * Converts formatted Minecraft text into our flow content.
     */
    default void appendComponent(ITextComponent formattedText) {
        for (ITextComponent node : formattedText) {
            String text = node.getUnformattedComponentText();
            if (text.isEmpty()) {
                appendText(text);
            } else {
                var span = new LytFlowSpan();
                // TODO: Convert style
                span.appendText(text);
                append(span);
            }
        }
    }

    default void appendBreak() {
        var br = new LytFlowBreak();
        append(br);
    }

    @Override
    default void appendError(PageCompiler compiler, String text, UnistNode node) {
        append(compiler.createErrorFlowContent(text, node));
    }
}
