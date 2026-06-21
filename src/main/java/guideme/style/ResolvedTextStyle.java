package guideme.style;

import guideme.color.ColorValue;

/**
 * Represents the styling of text for rendering.
 */
public record ResolvedTextStyle(
        float fontScale,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated,
        boolean unicode,
        ColorValue color,
        WhiteSpaceMode whiteSpace,
        TextAlignment alignment,
        boolean dropShadow) {
}
