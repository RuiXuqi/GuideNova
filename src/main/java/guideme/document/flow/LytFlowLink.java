package guideme.document.flow;

import guideme.PageAnchor;
import guideme.color.SymbolicColor;
import guideme.internal.GuideMEClient;
import guideme.internal.util.PlatformUtil;
import guideme.ui.GuideUiHost;
import java.net.URI;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.util.SoundEvent;
import org.jetbrains.annotations.Nullable;

public class LytFlowLink extends LytTooltipSpan {
    @Nullable
    private Consumer<GuideUiHost> clickCallback;

    @Nullable
    private SoundEvent clickSound = GuideMEClient.GUIDE_CLICK_EVENT;

    public LytFlowLink() {
        modifyStyle(style -> style.color(SymbolicColor.LINK));
        modifyHoverStyle(style -> style.underlined(true));
    }

    public void setClickCallback(@Nullable Consumer<GuideUiHost> clickCallback) {
        this.clickCallback = clickCallback;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button) {
        if (button == 0 && clickCallback != null) {
            if (clickSound != null) {
                var handler = Minecraft.getMinecraft().getSoundHandler();
                handler.playSound(PositionedSoundRecord.getMasterRecord(clickSound, 1.0F));
            }
            clickCallback.accept(screen);
            return true;
        }
        return false;
    }

    public @Nullable SoundEvent getClickSound() {
        return clickSound;
    }

    public void setClickSound(@Nullable SoundEvent clickSound) {
        this.clickSound = clickSound;
    }

    /**
     * Configures this link to open the given external URL on click.
     */
    public void setExternalUrl(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("External URLs must be absolute: " + uri);
        }

        setClickCallback(screen -> {
            var mc = Minecraft.getMinecraft();
            var previousScreen = mc.currentScreen;
            mc.displayGuiScreen(new GuiConfirmOpenLink((yes, _) -> {
                if (yes)
                    PlatformUtil.openUri(uri);
                mc.displayGuiScreen(previousScreen);
            }, uri.toString(), -1, true));
        });
    }

    /**
     * Configures this link to open the given page on click.
     */
    public void setPageLink(PageAnchor anchor) {
        setClickCallback(screen -> {
            screen.navigateTo(anchor);
        });
    }
}
