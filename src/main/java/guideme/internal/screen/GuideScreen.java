package guideme.internal.screen;

import guideme.Guide;
import guideme.GuidePage;
import guideme.PageAnchor;
import guideme.PageCollection;
import guideme.color.ConstantColor;
import guideme.color.SymbolicColor;
import guideme.compiler.AnchorIndexer;
import guideme.compiler.PageCompiler;
import guideme.compiler.ParsedGuidePage;
import guideme.document.DefaultStyles;
import guideme.document.LytRect;
import guideme.document.block.LytDocument;
import guideme.document.block.LytHeading;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowAnchor;
import guideme.document.flow.LytFlowContent;
import guideme.document.flow.LytFlowSpan;
import guideme.internal.GuidebookText;
import guideme.layout.LayoutContext;
import guideme.layout.MinecraftFontMetrics;
import guideme.render.GuiAssets;
import guideme.render.GuidePageTexture;
import guideme.render.RenderContext;
import guideme.style.TextAlignment;
import guideme.style.TextStyle;
import guideme.ui.GuideUiHost;
import guideme.ui.UiPoint;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuideScreen extends DocumentScreen implements GuideUiHost {
    private static final Logger LOG = LoggerFactory.getLogger(GuideScreen.class);

    private final Guide guide;

    private GuidePage currentPage;
    private final LytParagraph pageTitle;

    private final NavigationToolbar toolbar;

    @Nullable
    private GuiScreen returnToOnClose;

    /**
     * When the guidebook is initially opened, it does not do a proper layout due to missing width/height info. When we
     * should scroll to a point in the page, we have to "stash" that and do it after the initial proper layout has been
     * done.
     */
    @Nullable
    private String pendingScrollToAnchor;

    private final GuideNavBar navbar;

    private GuideScreen(Guide guide, PageAnchor anchor) {
        this.guide = guide;

        this.pageTitle = new LytParagraph();
        this.pageTitle.setStyle(DefaultStyles.HEADING1);

        toolbar = new NavigationToolbar(guide);
        toolbar.setCloseCallback(this::onClose);
        toolbar.setCanSearch(true);

        navbar = new GuideNavBar(this);

        loadPageAndScrollTo(anchor);
    }

    /**
     * Opens and resets history. Uses per-guide history by default.
     */
    public static GuideScreen openNew(Guide guide, PageAnchor anchor) {
        return openNew(guide, anchor, GlobalInMemoryHistory.get(guide));
    }

    /**
     * Opens and resets history.
     */
    public static GuideScreen openNew(Guide guide, PageAnchor anchor, GuideScreenHistory history) {
        history.push(anchor);

        return new GuideScreen(guide, anchor);
    }

    @Override
    public void initGui() {
        super.initGui();

        addButton(navbar);
        toolbar.addToScreen(this::addButton);

        updateScreenLayout();
    }

    private void updateScreenLayout() {
        if (screenRect.isEmpty()) {
            return; // On first call there's no point to layout
        }

        // If there is enough space, always expand the navbar
        navbar.setPinned(hasSpaceForSidebar());
        navbar.x = screenRect.x();

        var left = screenRect.x();
        if (!navbar.isPinned() && left < GuideNavBar.WIDTH_CLOSED) {
            left = GuideNavBar.WIDTH_CLOSED;
        }

        var availableWidth = screenRect.right() - left - toolbar.getWidth() - 5;
        updateTitleLayout(left, availableWidth);

        var marginTop = pageTitle.getBounds().bottom() + 4;

        var toolbarTop = (marginTop - toolbar.getHeight()) / 2;
        toolbar.move(screenRect.right() - toolbar.getWidth(), toolbarTop);

        if (navbar.isPinned()) {
            left = screenRect.x() + navbar.width;
        }

        setDocumentRect(new LytRect(
                left,
                screenRect.y() + marginTop,
                screenRect.right() - left,
                screenRect.height() - getMarginBottom() - marginTop));

        if (navbar.isPinned()) {
            // Move the navbar to below the title
            navbar.y = getDocumentRect().y();
            navbar.height = getDocumentRect().height();
        } else {
            navbar.y = screenRect.y();
            navbar.height = screenRect.height();
        }

        updateDocumentLayout();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        toolbar.update();

        processPendingScrollTo();
    }

    @Override
    protected boolean documentClicked(UiPoint documentPoint, int button) {
        if (button == 3) {
            GuideNavigation.navigateBack(guide);
            return true;
        } else if (button == 4) {
            GuideNavigation.navigateForward(guide);
            return true;
        }

        return false;
    }

    /**
     * If a scroll-to command is queued, this processes that.
     */
    private void processPendingScrollTo() {
        if (pendingScrollToAnchor == null) {
            return;
        }

        var anchor = pendingScrollToAnchor;
        pendingScrollToAnchor = null;

        var indexer = new AnchorIndexer(currentPage.document());

        var targetAnchor = indexer.get(anchor);
        if (targetAnchor == null) {
            LOG.warn("Failed to find anchor {} in page {}", anchor, currentPage);
            return;
        }

        if (targetAnchor.flowContent() instanceof LytFlowAnchor flowAnchor && flowAnchor.getLayoutY().isPresent()) {
            setDocumentScrollY(flowAnchor.getLayoutY().getAsInt());
        } else {
            var bounds = targetAnchor.blockNode().getBounds();
            setDocumentScrollY(bounds.y());
        }
    }

    @Override
    protected void scaledRender(RenderContext context, int mouseX, int mouseY, float partialTick) {
        context.fillIcon(screenRect, GuiAssets.GUIDE_BACKGROUND, SymbolicColor.GUIDE_SCREEN_BACKGROUND);

        var documentRect = getDocumentRect();
        context.fillRect(documentRect, new ConstantColor(0x40333333));

        renderDocument(context);

        context.push();
        context.translate(0, 0, 200);

        renderTitle(documentRect, context);

        if (hasFooter()) {
            renderFooter(documentRect, context);
        }

        navbar.renderCtx(context, mouseX, mouseY, partialTick);
        super.scaledRender(context, mouseX, mouseY, partialTick);

        context.pop();

        renderDocumentTooltip(context, mouseX, mouseY, partialTick);
    }

    private void renderFooter(LytRect documentRect, RenderContext context) {
        // Render the source of the content
        var externalSource = getExternalSourceName();
        if (externalSource != null) {
            var paragraph = new LytParagraph();
            paragraph.appendText(GuidebookText.ContentFrom.text().getFormattedText() + " ");
            var sourceSpan = new LytFlowSpan();

            sourceSpan.appendText(externalSource);
            sourceSpan.setStyle(TextStyle.builder().italic(true).build());
            paragraph.append(sourceSpan);
            paragraph.setStyle(TextStyle.builder().alignment(TextAlignment.RIGHT).build());
            var layoutContext = new LayoutContext(new MinecraftFontMetrics());
            paragraph.layout(layoutContext, documentRect.x(), documentRect.bottom(), documentRect.width());
            paragraph.render(context);
        }
    }

    @Override
    protected boolean hasFooter() {
        return getExternalSourceName() != null;
    }

    /**
     * Gets a readable name for the source of the page (i.e. resource pack name, mod name) if the page has been
     * contributed externally.
     */
    @Nullable
    private String getExternalSourceName() {
        if (!currentPage.sourcePack().startsWith("mod:")) {
            for (var entry : Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries()) {
                if (entry.getResourcePackName().equals(currentPage.sourcePack())) {
                    return entry.getTexturePackDescription();
                }
            }
        }

        var pageNamespace = currentPage.id().getNamespace();
        // If the page had an ID under another mod's namespace, we have to use the mod-list to resolve its name
        if (!guide.getDefaultNamespace().equals(pageNamespace)) {
            var mod = Loader.instance().getIndexedModList().get(pageNamespace);
            return mod != null ? mod.getName() : null;
        }

        return null;
    }

    @Override
    public void drawWorldBackground(int tint) {
        // Stub this out otherwise vanilla renders a background on top of our content
    }

    private void renderTitle(LytRect documentRect, RenderContext context) {
        pageTitle.render(context);
        var separatorRect = new LytRect(
                screenRect.x(),
                documentRect.y() - 1,
                screenRect.width(),
                1);
        separatorRect = separatorRect.withWidth(screenRect.width());
        context.fillRect(separatorRect, SymbolicColor.HEADER1_SEPARATOR);
    }

    @Override
    public void navigateTo(ResourceLocation pageId) {
        navigateTo(PageAnchor.page(pageId));
    }

    @Override
    public void navigateTo(PageAnchor anchor) {
        GuideNavigation.navigateTo(guide, anchor);
    }

    void loadPageAndScrollTo(PageAnchor anchor) {
        loadPage(anchor.pageId());

        setDocumentScrollY(0);
        updateDocumentLayout();

        pendingScrollToAnchor = anchor.anchor();
    }

    @Override
    public void reloadPage() {
        loadPage(currentPage.id());
        updateDocumentLayout();
    }

    private void loadPage(ResourceLocation pageId) {
        GuidePageTexture.releaseUsedTextures();
        var page = guide.getParsedPage(pageId);

        if (page == null) {
            // Build a "not found" page dynamically
            page = buildNotFoundPage(pageId);
        }

        currentPage = PageCompiler.compile(guide, guide.getExtensions(), page);

        // Find and pull out the first heading
        pageTitle.clearContent();
        for (var flowContent : extractPageTitle(currentPage)) {
            pageTitle.append(flowContent);
        }

        updateScreenLayout();
    }

    private Iterable<LytFlowContent> extractPageTitle(GuidePage page) {
        for (var block : page.document().getBlocks()) {
            if (block instanceof LytHeading heading) {
                if (heading.getDepth() == 1) {
                    page.document().removeChild(heading);
                    return heading.getContent();
                } else {
                    break; // Any heading other than depth 1 cancels this algo
                }
            }
        }
        return List.of();
    }

    private ParsedGuidePage buildNotFoundPage(ResourceLocation pageId) {
        String pageSource = "# Page not Found\n" +
                "\n" +
                "Page \"" + pageId + "\" could not be found.";

        return PageCompiler.parse(
                pageId.getNamespace(),
                "en_us",
                pageId,
                pageSource);
    }

    /**
     * Sets a screen to return to when closing this guide.
     */
    public void setReturnToOnClose(@Nullable GuiScreen screen) {
        this.returnToOnClose = screen;
    }

    public @Nullable GuiScreen getReturnToOnClose() {
        return returnToOnClose;
    }

    @Override
    protected LytDocument getDocument() {
        return currentPage.document();
    }

    private void updateTitleLayout(int left, int availableWidth) {
        var context = new LayoutContext(new MinecraftFontMetrics());
        // Compute the fake layout to find out how high it would be
        // Account for the navigation buttons on the right
        if (availableWidth < 0) {
            availableWidth = 0;
        }

        var minTitleHeight = 20;
        pageTitle.layout(context, 0, 0, availableWidth);
        var titleY = Math.max(4, minTitleHeight - pageTitle.getBounds().height()) / 2;

        pageTitle.layout(context, left + 5, titleY, availableWidth);
    }

    public void scrollToAnchor(@Nullable String anchor) {
        pendingScrollToAnchor = anchor;
        if (anchor == null) {
            setDocumentScrollY(0);
        }
    }

    @Override
    public PageCollection getGuide() {
        return guide;
    }

    public ResourceLocation getCurrentPageId() {
        return currentPage.id();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        GuidePageTexture.releaseUsedTextures();
    }

    @Override
    protected void onClose() {
        if (mc != null && mc.currentScreen == this && this.returnToOnClose != null) {
            mc.displayGuiScreen(this.returnToOnClose);
            this.returnToOnClose = null;
            return;
        }
        super.onClose();
    }

    private boolean hasSpaceForSidebar() {
        return width >= super.getMaxWidth();
    }
}
