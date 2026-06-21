package guideme.internal.screen;

import guideme.Guide;
import guideme.Guides;
import guideme.PageAnchor;
import guideme.PageCollection;
import guideme.color.ConstantColor;
import guideme.color.SymbolicColor;
import guideme.compiler.ParsedGuidePage;
import guideme.document.DefaultStyles;
import guideme.document.LytRect;
import guideme.document.block.AlignItems;
import guideme.document.block.LytDocument;
import guideme.document.block.LytHBox;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowBreak;
import guideme.document.flow.LytFlowLink;
import guideme.internal.GuideME;
import guideme.internal.GuideMEClient;
import guideme.internal.GuidebookText;
import guideme.internal.search.GuideSearch;
import guideme.internal.util.Blitter;
import guideme.internal.util.NavigationUtil;
import guideme.render.GuiAssets;
import guideme.render.RenderContext;
import guideme.scene.LytItemImage;
import guideme.style.BorderStyle;
import guideme.ui.GuideUiHost;
import guideme.ui.UiPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

public class GuideSearchScreen extends DocumentScreen {
    /**
     * This ID refers to this screen as a built-in page.
     */
    public static final ResourceLocation PAGE_ID = GuideME.makeId("search");

    private static final int MIN_TITLE_HEIGHT = 16;

    private final GuideEditBox searchField;

    private final Guide guide;

    private final NavigationToolbar toolbar;

    @Nullable
    private GuiScreen returnToOnClose;

    private final LytDocument searchResultsDoc = new LytDocument();

    private final List<GuideSearch.SearchResult> searchResults = new ArrayList<>();

    GuideSearchScreen(Guide guide) {
        this.guide = guide;
        this.toolbar = new NavigationToolbar(guide);
        this.toolbar.setCloseCallback(this::onClose);

        // Trigger indexing of this guide
        GuideMEClient.SEARCH.index(guide);

        searchField = new GuideEditBox(
                Minecraft.getMinecraft().fontRenderer,
                16,
                6,
                0,
                14);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setHint(GuidebookText.Search.text().setStyle(new Style()
                .setColor(TextFormatting.DARK_GRAY).setItalic(true)).getFormattedText());
        searchField.setResponder(this::search);
        searchField.setFocused(true);
    }

    public static GuideSearchScreen open(Guide guide, @Nullable String anchor) {
        var history = GlobalInMemoryHistory.get(guide);
        history.push(new PageAnchor(PAGE_ID, anchor));

        var screen = new GuideSearchScreen(guide);
        if (anchor != null) {
            screen.searchField.setText(anchor);
        }
        return screen;
    }

    @Override
    protected LytDocument getDocument() {
        return searchResultsDoc;
    }

    @Override
    public void navigateTo(ResourceLocation pageId) {
    }

    @Override
    public void navigateTo(PageAnchor anchor) {
    }

    @Override
    public void initGui() {
        super.initGui();

        toolbar.addToScreen(this::addButton);
        toolbar.update();

        searchField.x = screenRect.x() + 16;
        searchField.width = screenRect.right() - searchField.x - toolbar.getWidth();
        searchField.setCursorPosition(searchField.getCursorPosition());

        if (screenRect.isEmpty()) {
            return; // On first call there's no point to layout
        }

        var left = screenRect.x();

        int documentTop = searchField.y + searchField.height;
        var toolbarTop = (documentTop - toolbar.getHeight()) / 2;
        toolbar.move(screenRect.right() - toolbar.getWidth(), toolbarTop);

        setDocumentRect(new LytRect(
                left,
                documentTop,
                screenRect.right() - left,
                screenRect.height() - getMarginBottom()));

        updateDocumentLayout();
    }

    private void search(String query) {
        // Update history such that forward/backwards will remember the current search query
        GlobalInMemoryHistory.get(guide).push(makeSearchAnchor());

        searchResults.clear();
        searchResults.addAll(GuideMEClient.SEARCH.searchGuide(query, guide));

        searchResultsDoc.clearContent();

        for (var searchResult : searchResults) {
            var searchResultItem = new LytHBox();
            searchResultItem.setFullWidth(true);
            searchResultItem.setGap(5);
            searchResultItem.setAlignItems(AlignItems.CENTER);

            var guide = Guides.getById(searchResult.guideId());
            if (guide == null) {
                continue;
            }

            var page = guide.getParsedPage(searchResult.pageId());
            if (page == null) {
                continue;
            }
            var icon = NavigationUtil.createNavigationIcon(page);

            var image = new LytItemImage();
            if (!icon.isEmpty()) {
                image.setItem(icon);
            }
            searchResultItem.append(image);

            var summary = new LytParagraph();
            var documentLink = buildLinkToSearchResult(searchResult, guide, page);
            summary.append(documentLink);
            summary.append(new LytFlowBreak());
            summary.append(searchResult.text());
            summary.setPaddingTop(2);
            summary.setPaddingBottom(2);
            searchResultItem.append(summary);

            searchResultItem.setBorderBottom(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

            searchResultsDoc.append(searchResultItem);
        }

        updateDocumentLayout();
    }

    private LytFlowLink buildLinkToSearchResult(GuideSearch.SearchResult searchResult, Guide guide,
            ParsedGuidePage page) {
        var documentLink = new LytFlowLink();
        documentLink.appendText(searchResult.pageTitle());
        documentLink.setClickCallback(ignored -> {
            // Append this search page to the guides history
            var history = GlobalInMemoryHistory.get(guide);
            history.push(makeSearchAnchor());

            // Reuse the same guide screen if it's within the same guide
            if (returnToOnClose instanceof GuideUiHost guideHost && guideHost.getGuide() == guide) {
                this.onClose();
                guideHost.navigateTo(page.getId());
            } else {
                returnToOnClose = GuideScreen.openNew(guide, PageAnchor.page(page.getId()));
                this.onClose();
            }
        });
        return documentLink;
    }

    @Override
    protected void scaledRender(RenderContext context, int mouseX, int mouseY, float partialTick) {
        context.fillIcon(screenRect, GuiAssets.GUIDE_BACKGROUND, SymbolicColor.GUIDE_SCREEN_BACKGROUND);

        Blitter.texture(GuideME.makeId("textures/guide/buttons.png"), 64, 64)
                .src(GuideIconButton.Role.SEARCH.iconSrcX, GuideIconButton.Role.SEARCH.iconSrcY, 16, 16)
                .dest(screenRect.x(), 2, 16, 16)
                .colorArgb(context.resolveColor(SymbolicColor.ICON_BUTTON_NORMAL))
                .blit();

        var documentRect = getDocumentRect();
        context.fillRect(documentRect, new ConstantColor(0x80333333));

        if (searchField.getText().isEmpty()) {
            context.renderTextCenteredIn(
                    GuidebookText.SearchNoQuery.text().getFormattedText(),
                    DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE),
                    documentRect);
        } else if (searchResults.isEmpty()) {
            context.renderTextCenteredIn(
                    GuidebookText.SearchNoResults.text().getFormattedText(),
                    DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE),
                    documentRect);
        } else {
            renderDocument(context);
        }

        context.push();
        context.translate(0, 0, 200);

        renderTitle(documentRect, context);

        searchField.drawTextBox();
        super.scaledRender(context, mouseX, mouseY, partialTick);

        context.pop();

        renderDocumentTooltip(context, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!searchField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchField.updateCursorCounter();
    }

    @Override
    public void drawWorldBackground(int tint) {
        // Stub this out otherwise vanilla renders a background on top of our content
    }

    private void renderTitle(LytRect documentRect, RenderContext context) {
        var separatorRect = new LytRect(
                documentRect.x(),
                documentRect.y() - 1,
                documentRect.width(),
                1);
        separatorRect = separatorRect.withWidth(screenRect.width());
        context.fillRect(separatorRect, SymbolicColor.HEADER1_SEPARATOR);
    }

    private PageAnchor makeSearchAnchor() {
        if (searchField.getText().isBlank()) {
            return PageAnchor.page(PAGE_ID);
        } else {
            return new PageAnchor(PAGE_ID, searchField.getText());
        }
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

    @Override
    protected void onClose() {
        if (mc != null && mc.currentScreen == this && this.returnToOnClose != null) {
            mc.displayGuiScreen(this.returnToOnClose);
            this.returnToOnClose = null;
            return;
        }
        super.onClose();
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
    public PageCollection getGuide() {
        return guide;
    }

    @Override
    public void reloadPage() {
    }
}
