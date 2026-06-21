package guideme.internal;

import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.IdUtils;
import guideme.internal.screen.GuideScreen;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for facilitating the use of the Guide without entering the game.
 */
public final class GuideOnStartup {
    private static final Logger LOG = LoggerFactory.getLogger(GuideOnStartup.class);

    private GuideOnStartup() {
    }

    public static void init() {
        var guidesToValidate = getGuideIdsToValidate();
        var showOnStartup = getShowOnStartup();

        if (!guidesToValidate.isEmpty() || showOnStartup != null) {
            var guideOpenedOnce = new MutableBoolean(false);
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onGuiOpen(GuiOpenEvent e) {
                    if (e.getGui() instanceof GuiMainMenu && !guideOpenedOnce.booleanValue()) {
                        guideOpenedOnce.setTrue();

                        for (var guideId : guidesToValidate) {
                            var guide = GuideRegistry.getById(guideId);
                            if (guide == null) {
                                LOG.error("Cannot validate guide '{}' since it does not exist.", guideId);
                            } else {
                                guide.validateAll();
                            }
                        }

                        if (showOnStartup != null) {
                            var guide = Guides.getById(showOnStartup.guideId);
                            if (guide == null) {
                                LOG.error("Cannot show guide '{}' since it does not exist.", showOnStartup.guideId);
                            } else {
                                try {
                                    var anchor = showOnStartup.anchor;
                                    if (anchor == null) {
                                        anchor = PageAnchor.page(guide.getStartPage());
                                    }
                                    var screen = GuideScreen.openNew(guide, anchor);
                                    screen.setReturnToOnClose(e.getGui());
                                    e.setGui(screen);
                                } catch (Exception ex) {
                                    LOG.error("Failed to open {}", showOnStartup, ex);
                                    System.exit(1);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private record ShowOnStartup(ResourceLocation guideId, @Nullable PageAnchor anchor) {
    }

    private static ShowOnStartup getShowOnStartup() {
        var showOnStartup = System.getProperty("guideme.showOnStartup");
        if (showOnStartup == null) {
            return null;
        }

        var parts = showOnStartup.split("!", 2);
        var guideId = IdUtils.parse(parts[0]);
        PageAnchor page = null;
        if (parts.length > 1) {
            page = PageAnchor.parse(parts[1]);
        }
        return new ShowOnStartup(guideId, page);
    }

    private static Set<ResourceLocation> getGuideIdsToValidate() {
        Set<ResourceLocation> guidesToValidate = new LinkedHashSet<>();
        var validateGuideIds = System.getProperty("guideme.validateAtStartup");
        if (validateGuideIds != null) {
            var guideIds = validateGuideIds.split(",");
            for (String guideId : guideIds) {
                guidesToValidate.add(IdUtils.parse(guideId));
            }
        }
        return guidesToValidate;
    }

}
