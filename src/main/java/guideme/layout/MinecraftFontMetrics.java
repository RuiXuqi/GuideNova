package guideme.layout;

import guideme.style.ResolvedTextStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;

public class MinecraftFontMetrics implements FontMetrics {
    private final FontRenderer font;

    public MinecraftFontMetrics() {
        this(Minecraft.getMinecraft().fontRenderer);
    }

    public MinecraftFontMetrics(FontRenderer font) {
        this.font = font;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        String text = new String(Character.toChars(codePoint));
        if (style.bold())
            text = TextFormatting.BOLD + text;
        boolean wasUnicode = font.getUnicodeFlag();
        font.setUnicodeFlag(style.unicode());
        float width = font.getStringWidth(text);
        font.setUnicodeFlag(wasUnicode);
        return width;
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        return (int) Math.ceil(font.FONT_HEIGHT * style.fontScale());
    }
}
