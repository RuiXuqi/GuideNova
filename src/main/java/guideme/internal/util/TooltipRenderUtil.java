package guideme.internal.util;

import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@SideOnly(Side.CLIENT)
public final class TooltipRenderUtil {
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR_TOP = 0x505000FF;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007F;

    private TooltipRenderUtil() {
    }

    // Pos

    public static Vector2ic positionMenuTooltip(
            FontRenderer font, List<String> tooltip, int screenWidth, int screenHeight,
            int mouseX, int mouseY, int widgetY, int widgetHeight) {
        return positionMenuTooltip(screenWidth, screenHeight, mouseX, mouseY,
                getTooltipWidth(font, tooltip), getTooltipHeight(tooltip), widgetY, widgetHeight);
    }

    private static Vector2ic positionMenuTooltip(
            int screenWidth, int screenHeight, int mouseX, int mouseY,
            int tooltipWidth, int tooltipHeight, int widgetY, int widgetHeight) {
        // Logic from MenuTooltipPositioner
        Vector2i tooltipPos = new Vector2i(mouseX + 12, mouseY);
        if (tooltipPos.x + tooltipWidth > screenWidth - 5) {
            tooltipPos.x = Math.max(mouseX - 12 - tooltipWidth, 9);
        }

        tooltipPos.y += 3;
        int framedTooltipHeight = tooltipHeight + 3 + 3;
        int preferredBelowY = widgetY + widgetHeight + 3 + getOffset(0, 0, widgetHeight);
        int bottomLimit = screenHeight - 5;
        if (preferredBelowY + framedTooltipHeight <= bottomLimit) {
            tooltipPos.y += getOffset(tooltipPos.y, widgetY, widgetHeight);
        } else {
            tooltipPos.y -= framedTooltipHeight + getOffset(tooltipPos.y, widgetY + widgetHeight, widgetHeight);
        }

        return tooltipPos;
    }

    public static Vector2ic applyDrawingOffset(Vector2ic tooltipPos) {
        return new Vector2i(tooltipPos.x() - 12, tooltipPos.y() + 12);
    }

    private static int getOffset(int mouseY, int widgetY, int widgetHeight) {
        int distance = Math.min(Math.abs(mouseY - widgetY), widgetHeight);
        return Math.round(lerp((float) distance / (float) widgetHeight, widgetHeight - 3, 5.0F));
    }

    private static int getTooltipWidth(FontRenderer font, List<String> tooltip) {
        int tooltipWidth = 0;
        for (String line : tooltip) {
            tooltipWidth = Math.max(tooltipWidth, font.getStringWidth(line));
        }
        return tooltipWidth;
    }

    private static int getTooltipHeight(List<String> tooltips) {
        return getTooltipHeight(tooltips.size());
    }

    private static int getTooltipHeight(int lineCount) {
        if (lineCount <= 1)
            return 8;
        return 8 + (lineCount - 1) * 10 + 2;
    }

    private static float lerp(float amount, float start, float end) {
        return start + amount * (end - start);
    }

    // Render

    public static void renderTooltipBackground(int x, int y, int width, int height, int z) {
        renderTooltipBackground(x, y, width, height, z,
                BACKGROUND_COLOR, BACKGROUND_COLOR, BORDER_COLOR_TOP, BORDER_COLOR_BOTTOM);
    }

    public static void renderTooltipBackground(int x, int y, int width, int height, int z,
            int backgroundTop, int backgroundBottom, int borderTop, int borderBottom) {
        int left = x - 3;
        int top = y - 3;
        int right = width + 3 + 3;
        int bottom = height + 3 + 3;
        renderHorizontalLine(left, top - 1, right, z, backgroundTop);
        renderHorizontalLine(left, top + bottom, right, z, backgroundBottom);
        renderRectangle(left, top, right, bottom, z, backgroundTop, backgroundBottom);
        renderVerticalLineGradient(left - 1, top, bottom, z, backgroundTop, backgroundBottom);
        renderVerticalLineGradient(left + right, top, bottom, z, backgroundTop, backgroundBottom);
        renderFrameGradient(left, top + 1, right, bottom, z, borderTop, borderBottom);
    }

    private static void renderFrameGradient(int x, int y, int width, int height, int z, int topColor, int bottomColor) {
        renderVerticalLineGradient(x, y, height - 2, z, topColor, bottomColor);
        renderVerticalLineGradient(x + width - 1, y, height - 2, z, topColor, bottomColor);
        renderHorizontalLine(x, y - 1, width, z, topColor);
        renderHorizontalLine(x, y - 1 + height - 1, width, z, bottomColor);
    }

    private static void renderVerticalLineGradient(int x, int y, int length, int z, int topColor, int bottomColor) {
        GuiUtils.drawGradientRect(z, x, y, x + 1, y + length, topColor, bottomColor);
    }

    private static void renderHorizontalLine(int x, int y, int length, int z, int color) {
        GuiUtils.drawGradientRect(z, x, y, x + length, y + 1, color, color);
    }

    private static void renderRectangle(int x, int y, int width, int height, int z, int color, int colorTo) {
        GuiUtils.drawGradientRect(z, x, y, x + width, y + height, color, colorTo);
    }
}
