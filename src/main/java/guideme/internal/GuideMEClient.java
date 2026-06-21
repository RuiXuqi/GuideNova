package guideme.internal;

import guideme.Guide;
import guideme.PageAnchor;
import guideme.color.LightDarkMode;
import guideme.internal.atlas.GuiAtlas;
import guideme.internal.atlas.JsonAtlasPopulator;
import guideme.internal.item.GuideItem;
import guideme.internal.screen.GlobalInMemoryHistory;
import guideme.internal.screen.GuideNavigation;
import guideme.internal.search.GuideSearch;
import guideme.internal.util.config.ConfigBuilder;
import guideme.internal.util.config.ConfigUtil;
import guideme.internal.util.config.MouseWheelSensitivityEntry;
import guideme.render.GuiAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuideMEClient {
    private static final Logger LOG = LoggerFactory.getLogger(GuideMEClient.class);

    public static final ResourceLocation GUIDE_CLICK_ID = GuideME.makeId("guide.click");
    public static final SoundEvent GUIDE_CLICK_EVENT = new SoundEvent(GUIDE_CLICK_ID).setRegistryName(GUIDE_CLICK_ID);

    public static final ResourceLocation NOISE_ID = GuideME.makeId("blocks/noise");

    public static final GuideSearch SEARCH = new GuideSearch();

    public static final GuiAtlas GUI_ATLAS = new GuiAtlas("textures", new JsonAtlasPopulator(GuideME.makeId("gui")));

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
    private static class GuideMEClientEvents {
        @SubscribeEvent
        public static void registerSounds(RegistryEvent.Register<SoundEvent> evt) {
            evt.getRegistry().register(GUIDE_CLICK_EVENT);
        }

        @SubscribeEvent
        public static void onTextureStitchPre(TextureStitchEvent.Pre evt) {
            if (evt.getMap() == Minecraft.getMinecraft().getTextureMapBlocks()) {
                evt.getMap().registerSprite(NOISE_ID);
            }
        }

        @SubscribeEvent
        public static void onTextureStitchPost(TextureStitchEvent.Post evt) {
            if (evt.getMap() == GuideMEClient.GUI_ATLAS) {
                GuiAssets.resetSprites();
            }
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent evt) {
            ModelLoader.setCustomModelResourceLocation(GuideME.GUIDE_ITEM, 0,
                    new ModelResourceLocation(GuideItem.ID, "inventory"));
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent evt) {
            if (evt.phase == TickEvent.Phase.START) {
                SEARCH.processWork();
                processDevWatchers();
            }
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent evt) {
            if (evt.getModID().equals(Reference.MOD_ID)) {
                GuideMEConfig.readFromProp();
            }
        }
    }

    private static void processDevWatchers() {
        for (var guide : GuideRegistry.getAll()) {
            guide.tick();
        }
    }

    public static LightDarkMode currentLightDarkMode() {
        return LightDarkMode.LIGHT_MODE;
    }

    public static boolean isShowDebugGuiOverlays() {
        return ClientConfig.showDebugGuiOverlays;
    }

    public static boolean isAdaptiveScalingEnabled() {
        return ClientConfig.adaptiveScaling;
    }

    public static boolean isIgnoreTranslatedGuides() {
        return ClientConfig.ignoreTranslatedGuides;
    }

    public static boolean isHideMissingRecipeErrors() {
        return ClientConfig.hideMissingRecipeErrors;
    }

    public static boolean isFullWidthLayout() {
        return ClientConfig.fullWidthLayout;
    }

    public static void setFullWidthLayout(boolean fullWidth) {
        if (fullWidth != isFullWidthLayout()) {
            ClientConfig.fullWidthLayout = fullWidth;
            GuideMEConfig.save();
            var minecraft = Minecraft.getMinecraft();
            var screen = minecraft.currentScreen;
            if (screen != null) {
                var sr = new ScaledResolution(minecraft);
                screen.onResize(minecraft, sr.getScaledWidth(), sr.getScaledHeight());
            }
        }
    }

    public static double getScrollSensitivity() {
        return ClientConfig.scrollSensitivity;
    }

    public static boolean isDiscreteScrolling() {
        return ClientConfig.discreteScrolling;
    }

    public static boolean openGuideAtPreviousPage(Guide guide, ResourceLocation initialPage) {
        try {
            var history = GlobalInMemoryHistory.get(guide);
            var historyPage = history.current();
            if (historyPage.isPresent()) {
                GuideNavigation.navigateTo(guide, historyPage.get());
            } else {
                GuideNavigation.navigateTo(guide, PageAnchor.page(initialPage));
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed to open guide.", e);
            return false;
        }
    }

    public static boolean openGuideAtAnchor(Guide guide, PageAnchor anchor) {
        try {
            GuideNavigation.navigateTo(guide, anchor);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to open guide at {}.", anchor, e);
            return false;
        }
    }

    static class ClientConfig {
        static boolean ignoreTranslatedGuides = false;
        static boolean hideMissingRecipeErrors = false;

        static boolean adaptiveScaling = true;
        static boolean fullWidthLayout = true;

        static boolean showDebugGuiOverlays = false;

        static double scrollSensitivity = 1.0D;
        static boolean discreteScrolling = false;

        static void build(ConfigBuilder builder) {
            builder.pushCategory("guides", "Advanced Debugging Settings for Guide development");
            ignoreTranslatedGuides = builder.get(
                    "ignoreTranslatedGuides",
                    ignoreTranslatedGuides,
                    "Never load translated guide pages for your current language.");
            hideMissingRecipeErrors = builder.get(
                    "hideMissingRecipeErrors",
                    hideMissingRecipeErrors,
                    "Never show errors in guides when recipes can't be found (i.e. because they were hidden by a datapack).");
            builder.popCategory();

            builder.pushCategory("gui");
            adaptiveScaling = builder.get(
                    "adaptiveScaling",
                    adaptiveScaling,
                    "Adapt GUI scaling for the Guide screen to fix Minecraft font issues at GUI scale 1 and 3.");
            fullWidthLayout = builder.get(
                    "fullWidthLayout",
                    fullWidthLayout,
                    "Use the full width of the screen for the guide when it is opened.");
            builder.popCategory();

            builder.pushCategory("debug");
            showDebugGuiOverlays = builder.get(
                    "showDebugGuiOverlays",
                    showDebugGuiOverlays,
                    "Show debugging overlays in GUI on mouse-over.");
            builder.popCategory();

            builder.pushCategory("control");
            var scrollSensitivityP = builder.getProp(
                    "scrollSensitivity",
                    scrollSensitivity,
                    "Adjusts how far screens scroll for each mouse wheel step.").setMinValue(0.01D).setMaxValue(10.00D);
            scrollSensitivityP.setConfigEntryClass(MouseWheelSensitivityEntry.class);
            ConfigUtil.validifyRange(scrollSensitivityP);
            ConfigUtil.commentRange(scrollSensitivityP);
            scrollSensitivity = scrollSensitivityP.getDouble();

            discreteScrolling = builder.get(
                    "discreteScrolling",
                    discreteScrolling,
                    "Treats each wheel event as a single step regardless of high-resolution mouse or touchpad scroll distance.");
            builder.popCategory();
        }
    }
}
