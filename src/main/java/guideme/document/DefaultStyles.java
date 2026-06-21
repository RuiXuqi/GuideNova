package guideme.document;

import guideme.color.ConstantColor;
import guideme.color.SymbolicColor;
import guideme.style.ResolvedTextStyle;
import guideme.style.TextAlignment;
import guideme.style.TextStyle;
import guideme.style.WhiteSpaceMode;

public final class DefaultStyles {
    private DefaultStyles() {
    }

    /**
     * The base style everything else is based on.
     */
    public static final ResolvedTextStyle BASE_STYLE = new ResolvedTextStyle(
            1,
            false,
            false,
            false,
            false,
            false,
            true,
            SymbolicColor.BODY_TEXT,
            WhiteSpaceMode.NORMAL,
            TextAlignment.LEFT,
            false);

    public static final TextStyle BODY_TEXT = TextStyle.builder()
            .unicode(true)
            .color(SymbolicColor.BODY_TEXT)
            .build();

    public static final TextStyle CRAFTING_RECIPE_TYPE = TextStyle.builder()
            .unicode(true)
            .color(SymbolicColor.CRAFTING_RECIPE_TYPE)
            .build();

    public static final TextStyle HEADING1 = TextStyle.builder()
            .fontScale(1.3f)
            .bold(true).unicode(false)
            .color(ConstantColor.WHITE)
            .build();
    public static final TextStyle HEADING2 = TextStyle.builder()
            .fontScale(1.1f)
            .unicode(false)
            .build();
    public static final TextStyle HEADING3 = TextStyle.builder()
            .fontScale(1f)
            .unicode(false)
            .build();
    public static final TextStyle HEADING4 = TextStyle.builder()
            .fontScale(1.1f)
            .bold(true)
            .unicode(true)
            .build();
    public static final TextStyle HEADING5 = TextStyle.builder()
            .fontScale(1f)
            .bold(true)
            .unicode(true)
            .build();
    public static final TextStyle HEADING6 = TextStyle.builder()
            .fontScale(1f)
            .unicode(true)
            .build();

    public static final TextStyle SEARCH_RESULT_HIGHLIGHT = TextStyle.builder()
            .bold(true)
            .underlined(true)
            .build();

}
