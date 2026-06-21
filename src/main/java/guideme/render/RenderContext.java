package guideme.render;

import guideme.color.ColorValue;
import guideme.color.ConstantColor;
import guideme.color.LightDarkMode;
import guideme.color.MutableColor;
import guideme.document.LytRect;
import guideme.internal.util.FluidBlitter;
import guideme.layout.MinecraftFontMetrics;
import guideme.style.ResolvedTextStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public interface RenderContext {

    LightDarkMode lightDarkMode();

    default boolean isDarkMode() {
        return lightDarkMode() == LightDarkMode.DARK_MODE;
    }

    Matrix4f pose();

    void push();

    void pop();

    default void translate(float x, float y, float z) {
        pose().translate(x, y, z);
        GlStateManager.translate(x, y, z);
    }

    default void scale(float x, float y, float z) {
        pose().scale(x, y, z);
        GlStateManager.scale(x, y, z);
    }

    LytRect viewport();

    /**
     * Checks if the given rectangle intersects with the current viewport, after applying the active pose.
     */
    default boolean intersectsViewport(LytRect bounds) {
        return bounds.intersects(viewport());
    }

    int resolveColor(ColorValue ref);

    void fillRect(LytRect rect, ColorValue topLeft, ColorValue topRight, ColorValue bottomRight, ColorValue bottomLeft);

    default void drawIcon(int x, int y, GuiSprite guiSprite) {
        drawIcon(x, y, guiSprite, ConstantColor.WHITE);
    }

    default void drawIcon(int x, int y, GuiSprite guiSprite, ColorValue color) {
        drawIcon(x, y, guiSprite.atlasSprite(lightDarkMode()), color);
    }

    default void drawIcon(int x, int y, TextureAtlasSprite sprite, ColorValue color) {
        fillIcon(x, y, sprite.getIconWidth(), sprite.getIconHeight(), sprite, color);
    }

    default void fillIcon(LytRect bounds, GuiSprite guiSprite) {
        fillIcon(bounds.x(), bounds.y(), bounds.width(), bounds.height(), guiSprite);
    }

    default void fillIcon(LytRect bounds, GuiSprite guiSprite, ColorValue color) {
        fillIcon(bounds.x(), bounds.y(), bounds.width(), bounds.height(), guiSprite, color);
    }

    default void fillIcon(int x, int y, int width, int height, GuiSprite guiSprite) {
        fillIcon(x, y, width, height, guiSprite, ConstantColor.WHITE);
    }

    default void fillIcon(int x, int y, int width, int height, GuiSprite guiSprite, ColorValue color) {
        fillIcon(x, y, width, height, guiSprite.atlasSprite(lightDarkMode()), color);
    }

    default void fillIcon(int x, int y, int width, int height, TextureAtlasSprite sprite, ColorValue color) {
        var spriteLayer = new SpriteLayer();
        spriteLayer.fillSprite(new ResourceLocation(sprite.getIconName()), 0, 0, 0, width, height, resolveColor(color));
        spriteLayer.render(x, y, 0);
    }

    default ITextureObject getOrLoadTexture(ResourceLocation textureId) {
        var manager = Minecraft.getMinecraft().getTextureManager();
        var cache = manager.getTexture(textureId);
        if (cache != null)
            return cache;
        var texture = new SimpleTexture(textureId);
        return manager.loadTexture(textureId, texture) ? texture : TextureUtil.MISSING_TEXTURE;
    }

    default void fillTexturedRect(LytRect rect, ITextureObject texture,
            ColorValue topLeft, ColorValue topRight, ColorValue bottomRight, ColorValue bottomLeft) {
        // Just use the entire texture by default
        fillTexturedRect(rect, texture, topLeft, topRight, bottomRight, bottomLeft, 0, 0, 1, 1);
    }

    void fillTexturedRect(LytRect rect, ITextureObject texture,
            ColorValue topLeft, ColorValue topRight, ColorValue bottomRight, ColorValue bottomLeft,
            float u0, float v0, float u1, float v1);

    default void fillTexturedRect(LytRect rect, GuidePageTexture texture) {
        fillTexturedRect(rect, texture.use(), ConstantColor.WHITE);
    }

    default void fillTexturedRect(LytRect rect, ITextureObject texture) {
        fillTexturedRect(rect, texture, ConstantColor.WHITE);
    }

    default void fillTexturedRect(LytRect rect, ITextureObject texture, ColorValue color) {
        fillTexturedRect(rect, texture, color, color, color, color);
    }

    default void fillTexturedRect(LytRect rect, GuidePageTexture texture, ColorValue color) {
        fillTexturedRect(rect, texture.use(), color, color, color, color);
    }

    default void fillTexturedRect(LytRect rect, TextureAtlasSprite sprite, ColorValue color) {
        var texture = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        fillTexturedRect(rect, texture, color, color, color, color,
                sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV());
    }

    default void fillTexturedRect(LytRect rect, ResourceLocation textureId) {
        fillTexturedRect(rect, textureId, ConstantColor.WHITE);
    }

    default void fillTexturedRect(LytRect rect, ResourceLocation textureId, ColorValue color) {
        fillTexturedRect(rect, getOrLoadTexture(textureId), color);
    }

    void fillTriangle(Vector2f p1, Vector2f p2, Vector2f p3, ColorValue color);

    default FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    default float getWidth(String text, ResolvedTextStyle style) {
        return font().getStringWidth(style.bold() ? TextFormatting.BOLD + text : text);
    }

    default void renderTextCenteredIn(String text, ResolvedTextStyle style, LytRect rect) {
        var fontMetrics = new MinecraftFontMetrics(font());

        var splitLines = font().listFormattedStringToWidth(text, (int) ((rect.width() - 10) / style.fontScale()));
        var lineHeight = fontMetrics.getLineHeight(style);
        var overallHeight = splitLines.size() * lineHeight;
        var overallWidth = (int) (splitLines.stream().mapToDouble(font()::getStringWidth).max().orElse(0f)
                * style.fontScale());
        var textRect = new LytRect(0, 0, overallWidth, overallHeight);
        textRect = textRect.centerIn(rect);

        var y = textRect.y();
        for (var line : splitLines) {
            var x = textRect.x() + (textRect.width() - font().getStringWidth(line) * style.fontScale()) / 2;
            renderText(line, style, x, y);
            y += lineHeight;
        }
    }

    default void renderText(String text, ResolvedTextStyle style, float x, float y) {
        var effectiveStyle = new Style()
                .setBold(style.bold())
                .setItalic(style.italic())
                .setUnderlined(style.underlined())
                .setStrikethrough(style.strikethrough());

        float fontScale = style.fontScale();
        boolean fontScaled = fontScale != 1;
        if (fontScaled) {
            push();
            translate(x, y, 0.0F);
            scale(fontScale, fontScale, 1.0F);
            x = 0;
            y = 0;
        }

        boolean wasUnicode = font().getUnicodeFlag();
        font().setUnicodeFlag(style.unicode());
        font().drawString(new TextComponentString(text).setStyle(effectiveStyle).getFormattedText(), x, y,
                resolveColor(style.color()), style.dropShadow());
        font().setUnicodeFlag(wasUnicode);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (fontScaled)
            pop();
    }

    default void fillRect(int x, int y, int width, int height, ColorValue color) {
        fillRect(new LytRect(x, y, width, height), color);
    }

    default void fillRect(LytRect rect, ColorValue color) {
        fillRect(rect, color, color, color, color);
    }

    default void fillGradientVertical(LytRect rect, ColorValue top, ColorValue bottom) {
        fillRect(rect, top, top, bottom, bottom);
    }

    default void fillGradientVertical(int x, int y, int width, int height, ColorValue top, ColorValue bottom) {
        fillGradientVertical(new LytRect(x, y, width, height), top, bottom);
    }

    default void fillGradientHorizontal(LytRect rect, ColorValue left, ColorValue right) {
        fillRect(rect, left, right, right, left);
    }

    default void fillGradientHorizontal(int x, int y, int width, int height, ColorValue left, ColorValue right) {
        fillGradientHorizontal(new LytRect(x, y, width, height), left, right);
    }

    default void renderItem(ItemStack stack, int x, int y, float width, float height) {
        renderItem(stack, x, y, 0, width, height);
    }

    default void renderFluid(Fluid fluid, int x, int y, int z, int width, int height) {
        FluidBlitter.create(new FluidStack(fluid, 1))
                .dest(x, y, width, height)
                .zOffset(z)
                .blit();
    }

    default void renderFluid(FluidStack stack, int x, int y, int z, int width, int height) {
        FluidBlitter.create(stack)
                .dest(x, y, width, height)
                .blit();
    }

    void renderItem(ItemStack stack, int x, int y, int z, float width, float height);

    default void renderPanel(LytRect bounds) {
        var panelBlitter = new PanelBlitter(lightDarkMode());
        panelBlitter.addBounds(0, 0, bounds.width(), bounds.height());
        panelBlitter.blit(bounds.x(), bounds.y());
    }

    void pushScissor(LytRect bounds);

    void popScissor();

    default MutableColor mutableColor(ColorValue symbolicColor) {
        return MutableColor.of(symbolicColor, lightDarkMode());
    }
}
