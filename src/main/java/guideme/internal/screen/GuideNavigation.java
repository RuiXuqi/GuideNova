package guideme.internal.screen;

import guideme.Guide;
import guideme.PageAnchor;
import guideme.internal.util.PlatformUtil;
import java.net.URI;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the history navigation logic from GuideScreen to allow for jumping between search and guide display
 * seamlessly without duplicating all the nav logic.
 */
public final class GuideNavigation {
    private static final Logger LOG = LoggerFactory.getLogger(GuideNavigation.class);

    private GuideNavigation() {
    }

    public static void navigateTo(Guide guide, PageAnchor anchor) {
        var history = GlobalInMemoryHistory.get(guide);
        var currentScreen = getCurrentGuideMeScreen();
        GuiScreen screenToReturnTo = null;
        if (currentScreen instanceof GuideScreen guideScreen) {
            screenToReturnTo = guideScreen.getReturnToOnClose();
        } else if (currentScreen instanceof GuideSearchScreen searchScreen) {
            screenToReturnTo = searchScreen.getReturnToOnClose();
        } else {
            screenToReturnTo = Minecraft.getMinecraft().currentScreen;
        }

        // Handle built-in pages
        if (GuideSearchScreen.PAGE_ID.equals(anchor.pageId())) {
            var guiScreen = GuideSearchScreen.open(guide, anchor.anchor());
            guiScreen.setReturnToOnClose(screenToReturnTo);
            Minecraft.getMinecraft().displayGuiScreen(guiScreen);
            return;
        }

        // Handle navigation within the same guide
        if (currentScreen instanceof GuideScreen guideScreen && guideScreen.getGuide() == guide) {
            if (Objects.equals(guideScreen.getCurrentPageId(), anchor.pageId())) {
                guideScreen.scrollToAnchor(anchor.anchor());
                if (anchor.anchor() != null) {
                    history.push(anchor);
                }
            } else {
                guideScreen.loadPageAndScrollTo(anchor);
                history.push(anchor);
            }
            return;
        }

        GuideScreen guideScreen = GuideScreen.openNew(guide, anchor, history);
        guideScreen.setReturnToOnClose(screenToReturnTo);
        Minecraft.getMinecraft().displayGuiScreen(guideScreen);
    }

    @Nullable
    private static GuiScreen getCurrentGuideMeScreen() {
        var currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen instanceof GuideScreen || currentScreen instanceof GuideSearchScreen) {
            return currentScreen;
        }
        return null;
    }

    public static void navigateForward(Guide guide) {
        var history = GlobalInMemoryHistory.get(guide);
        history.forward().ifPresent(pageAnchor -> navigateTo(guide, pageAnchor));
    }

    public static void navigateBack(Guide guide) {
        var history = GlobalInMemoryHistory.get(guide);
        history.back().ifPresent(pageAnchor -> navigateTo(guide, pageAnchor));
    }

    public static void openUrl(String href) {
        URI uri;
        try {
            uri = URI.create(href);
        } catch (IllegalArgumentException ignored) {
            LOG.debug("Can't parse '{}' as URL", href);
            return;
        }

        // Treat it as an external URL if it has a scheme
        var minecraft = Minecraft.getMinecraft();
        var previousScreen = minecraft.currentScreen;

        if (uri.getScheme() != null) {
            if (minecraft.gameSettings.chatLinksPrompt) {
                minecraft.displayGuiScreen(new GuiConfirmOpenLink((doOpen, _) -> {
                    if (doOpen)
                        PlatformUtil.openUri(uri);
                    minecraft.displayGuiScreen(previousScreen);
                }, href, -1, false));
            } else {
                PlatformUtil.openUri(uri);
            }
        } else {
            LOG.debug("Can't open relative URL: '{}'", href);
        }
    }
}
