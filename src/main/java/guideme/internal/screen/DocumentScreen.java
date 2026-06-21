package guideme.internal.screen;

import guideme.color.ColorValue;
import guideme.color.ConstantColor;
import guideme.document.DefaultStyles;
import guideme.document.LytPoint;
import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.document.block.LytDocument;
import guideme.document.block.LytNode;
import guideme.document.flow.LytFlowContainer;
import guideme.document.interaction.GuideTooltip;
import guideme.document.interaction.InteractiveElement;
import guideme.internal.GuideMEClient;
import guideme.internal.util.DashPattern;
import guideme.internal.util.DashedRectangle;
import guideme.internal.util.TooltipRenderUtil;
import guideme.layout.LayoutContext;
import guideme.layout.MinecraftFontMetrics;
import guideme.render.RenderContext;
import guideme.style.ResolvedTextStyle;
import guideme.style.TextStyle;
import guideme.ui.GuideUiHost;
import guideme.ui.UiPoint;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentScreen extends IndepentScaleScreen implements GuideUiHost {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentScreen.class);

    private static final DashPattern DEBUG_NODE_OUTLINE = new DashPattern(1f, 4, 3, 0xFFFFFFFF, 500);
    private static final DashPattern DEBUG_CONTENT_OUTLINE = new DashPattern(0.5f, 2, 1, 0x7FFFFFFF, 500);
    private static final ColorValue DEBUG_HOVER_OUTLINE_COLOR = new ConstantColor(0x7FFFFF00);

    // 20 virtual px margin around the document
    private static final int FULL_SCREEN_MARGIN = 20;

    @Nullable
    private InteractiveElement mouseCaptureTarget;

    private LytDocument lastDocument;
    private boolean documentLayoutInvalid = true;

    private final GuideScrollbar scrollbar;

    protected LytRect screenRect = LytRect.empty();

    private LytRect documentRect = LytRect.empty();

    public DocumentScreen() {
        this.scrollbar = new GuideScrollbar();
    }

    @Override
    public void initGui() {
        super.initGui();

        if (GuideMEClient.isFullWidthLayout() || width < getMaxWidth()) {
            screenRect = new LytRect(0, 0, width, height);
        } else {
            var maxWidth = getMaxWidth();
            var screenWidth = Math.min(maxWidth, width);
            var left = (width - screenWidth) / 2;
            screenRect = new LytRect(left, 0, screenWidth, height);
        }

        addButton(scrollbar);
        updateDocumentLayout();
    }

    protected int getMaxWidth() {
        return 420 + 150;
    }

    @Override
    protected float calculateEffectiveScale() {
        if (!GuideMEClient.isAdaptiveScalingEnabled()) {
            return 1f;
        }

        // The unifont is already scaled down by half at gui scale 1
        // and at scale 3 it is scaled to 150%, both look bad
        // For GUI scales 1 and 3 scale up the entire screen by 1
        var mc = Minecraft.getMinecraft();
        var sr = new ScaledResolution(mc);
        var currentScale = sr.getScaleFactor();
        var effectiveScale = currentScale;
        if (currentScale == 1) {
            effectiveScale = 2;
        } else if (currentScale == 3) {
            effectiveScale = 4;
        }

        // Validate that when we scale up, we still are above the base width/height
        var virtualWidth = mc.displayWidth / effectiveScale;
        var virtualHeight = mc.displayHeight / effectiveScale;
        if (virtualWidth < 320 || virtualHeight < 240) {
            var reducedEffectiveScale = Math.max(2, currentScale - 1);
            LOG.debug("Not enough screen space ({}x{}) to increase GUI scale from {} to {}. Decreasing to {} instead.",
                    virtualWidth, virtualHeight, currentScale, effectiveScale, reducedEffectiveScale);
            effectiveScale = reducedEffectiveScale;
        }

        return (float) effectiveScale / currentScale;
    }

    protected final void ensureDocumentLayout() {
        if (!documentLayoutInvalid) {
            return;
        }

        documentLayoutInvalid = false;

        var docViewport = getDocumentViewport();
        var context = new LayoutContext(new MinecraftFontMetrics());

        // Build layout if needed
        var document = getDocument();
        if (document != null) {
            document.updateLayout(context, docViewport.width());
            scrollbar.setContentHeight(document.getContentHeight());
        }
    }

    protected final void updateDocumentLayout() {
        documentLayoutInvalid = true;
    }

    @Nullable
    protected abstract LytDocument getDocument();

    @Override
    public LytRect getDocumentRect() {
        return documentRect;
    }

    public void setDocumentRect(LytRect documentRect) {
        this.documentRect = documentRect.withWidth(documentRect.width() - scrollbar.width);
        scrollbar.move(this.documentRect.right(), this.documentRect.y(), this.documentRect.height());
    }

    @Override
    public final LytRect getDocumentViewport() {
        var documentRect = getDocumentRect();
        return new LytRect(0, scrollbar.getScrollAmount(), documentRect.width(), documentRect.height());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // Tick all controls on the page
        var document = getDocumentWithLayout();
        if (document != null) {
            tickNode(document);
        }
    }

    private static void tickNode(LytNode node) {
        node.tick();

        for (var child : node.getChildren()) {
            tickNode(child);
        }
    }

    protected final void setDocumentScrollY(int scrollY) {
        scrollbar.setScrollAmount(scrollY);
    }

    protected final void renderDocument(RenderContext context) {

        // Set scissor rectangle to rect that we show the document in
        var documentRect = getDocumentRect();

        var document = getDocumentWithLayout();
        if (document == null) {
            return;
        }

        // Move rendering to anchor @ 0,0 in the document rect
        var documentViewport = getDocumentViewport();

        // guiGraphics.enableScissor(documentRect.x(), documentRect.y(), documentRect.right(), documentRect.bottom());
        context.pushScissor(documentRect);
        context.push();
        context.translate(documentRect.x() - documentViewport.x(), documentRect.y() - documentViewport.y(), 0);

        document.render(context);

        // Clear depth after rendering the document since some elements in it may render with ludicrous z-values
        // Examples: scaled up item images have to scale their depth as well due to non-uniform scaling issues
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        context.popScissor();

        if (GuideMEClient.isShowDebugGuiOverlays()) {
            renderHoverOutline(document, context);
        }

        context.pop();

    }

    private static void renderHoverOutline(LytDocument document, RenderContext context) {
        var hoveredElement = document.getHoveredElement();

        if (hoveredElement == null) {
            return;
        }

        context.push();
        context.translate(0, 0, 1000);

        GlStateManager.colorLogicOp(GlStateManager.LogicOp.XOR);
        GlStateManager.enableColorLogic();

        // Fill a rectangle highlighting margins
        if (hoveredElement.node() instanceof LytBlock block) {
            var bounds = block.getBounds();
            if (block.getMarginTop() > 0) {
                context.fillRect(
                        bounds.withHeight(block.getMarginTop()).move(0, -block.getMarginTop()),
                        DEBUG_HOVER_OUTLINE_COLOR);
            }
            if (block.getMarginBottom() > 0) {
                context.fillRect(
                        bounds.withHeight(block.getMarginBottom()).move(0, bounds.height()),
                        DEBUG_HOVER_OUTLINE_COLOR);
            }
            if (block.getMarginLeft() > 0) {
                context.fillRect(
                        bounds.withWidth(block.getMarginLeft()).move(-block.getMarginLeft(), 0),
                        DEBUG_HOVER_OUTLINE_COLOR);
            }
            if (block.getMarginRight() > 0) {
                context.fillRect(
                        bounds.withWidth(block.getMarginRight()).move(bounds.width(), 0),
                        DEBUG_HOVER_OUTLINE_COLOR);
            }
        }

        // Fill the content rectangle
        DashedRectangle.render(hoveredElement.node().getBounds(), DEBUG_NODE_OUTLINE, 0);

        // Also outline any inline-elements in the block
        if (hoveredElement.content() != null) {
            if (hoveredElement.node() instanceof LytFlowContainer flowContainer) {
                flowContainer.enumerateContentBounds(hoveredElement.content())
                        .forEach(bound -> {
                            DashedRectangle.render(bound, DEBUG_CONTENT_OUTLINE, 0);
                        });
            }
        }

        GlStateManager.colorLogicOp(GlStateManager.LogicOp.COPY);
        GlStateManager.disableColorLogic();

        // Render the class-name of the hovered node to make it easier to identify
        var bounds = hoveredElement.node().getBounds();
        ResolvedTextStyle debugFontStyle = TextStyle.builder()
                .color(ConstantColor.WHITE)
                .build().mergeWith(DefaultStyles.BASE_STYLE);
        context.fillRect(
                bounds.x(),
                bounds.bottom(),
                (int) context.getWidth(hoveredElement.node().getClass().getName(), debugFontStyle),
                10,
                ConstantColor.BLACK);
        context.renderText(
                hoveredElement.node().getClass().getName(),
                debugFontStyle,
                bounds.x(),
                bounds.bottom());

        context.pop();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);

        if (mouseCaptureTarget != null) {
            var docPointUnclamped = getDocumentPointUnclamped(mouseX, mouseY);
            mouseCaptureTarget.mouseMoved(this, docPointUnclamped.x(), docPointUnclamped.y());
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseMoved(this, docPoint.x(), docPoint.y());
            });
        }
    }

    @Override
    protected boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            if (documentClicked(docPoint, button)) {
                return true;
            }

            return dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseClicked(this, docPoint.x(), docPoint.y(), button);
            });
        } else {
            return false;
        }
    }

    protected boolean documentClicked(UiPoint documentPoint, int button) {
        return false;
    }

    @Override
    protected boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseCaptureTarget != null) {
            var currentTarget = mouseCaptureTarget;

            var docPointUnclamped = getDocumentPointUnclamped(mouseX, mouseY);
            boolean handled = currentTarget.mouseReleased(this, docPointUnclamped.x(), docPointUnclamped.y(), button);

            releaseMouseCapture(currentTarget);
            if (handled) {
                return true;
            }
        }

        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            return dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseReleased(this, docPoint.x(), docPoint.y(), button);
            });
        } else {
            return false;
        }
    }

    @FunctionalInterface
    interface EventInvoker {
        boolean invoke(InteractiveElement el);
    }

    private boolean dispatchEvent(int x, int y, EventInvoker invoker) {
        return dispatchInteraction(x, y, el -> {
            if (invoker.invoke(el)) {
                return Optional.of(true);
            } else {
                return Optional.empty();
            }
        }).orElse(false);
    }

    private <T> Optional<T> dispatchInteraction(int x, int y, Function<InteractiveElement, Optional<T>> invoker) {
        var document = getDocumentWithLayout();
        if (document != null) {
            var underCursor = document.pick(x, y);
            if (underCursor != null) {
                return dispatchInteraction(underCursor, invoker);
            }
        }

        return Optional.empty();
    }

    private LytDocument getDocumentWithLayout() {
        var document = getDocument();
        if (lastDocument != document) {
            releaseMouseCapture();
            updateDocumentLayout();
            lastDocument = document;
        }
        if (document != null) {
            ensureDocumentLayout();
        }
        return document;
    }

    private static <T> Optional<T> dispatchInteraction(LytDocument.HitTestResult receiver,
            Function<InteractiveElement, Optional<T>> invoker) {
        // Iterate through content ancestors
        for (var el = receiver.content(); el != null; el = el.getFlowParent()) {
            if (el instanceof InteractiveElement interactiveEl) {
                var result = invoker.apply(interactiveEl);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        // Iterate through node ancestors
        for (var node = receiver.node(); node != null; node = node.getParent()) {
            if (node instanceof InteractiveElement interactiveEl) {
                var result = invoker.apply(interactiveEl);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }

    @Override
    protected void afterMouseMove(double mouseX, double mouseY) {
        super.afterMouseMove(mouseX, mouseY);

        var document = getDocumentWithLayout();
        if (document != null) {
            // If there's a widget under the cursor, ignore document hit-testing
            if (getChildAt(mouseX, mouseY) != null) {
                document.setHoveredElement(null);
                return;
            }

            var docPoint = this.getDocumentPoint(mouseX, mouseY);
            if (docPoint != null) {
                var hoveredEl = document.pick(docPoint.x(), docPoint.y());
                document.setHoveredElement(hoveredEl);
            } else {
                document.setHoveredElement(null);
            }
        }
    }

    @Override
    public @Nullable UiPoint getDocumentPoint(double screenX, double screenY) {
        var documentRect = getDocumentRect();

        if (screenX >= documentRect.x() && screenX < documentRect.right()
                && screenY >= documentRect.y() && screenY < documentRect.bottom()) {
            return getDocumentPointUnclamped(screenX, screenY);
        }

        return null; // Outside the document
    }

    @Override
    public UiPoint getDocumentPointUnclamped(double screenX, double screenY) {
        var documentRect = getDocumentRect();
        var docX = (int) Math.round(screenX - documentRect.x());
        var docY = (int) Math.round(screenY + scrollbar.getScrollAmount() - documentRect.y());
        return new UiPoint(docX, docY);
    }

    /**
     * Translate a point from within the document into the screen coordinate system.
     */
    @Override
    public LytPoint getScreenPoint(LytPoint documentPoint) {
        var documentRect = getDocumentRect();
        var documentViewport = getDocumentViewport();
        var x = documentPoint.x() - documentViewport.x();
        var y = documentPoint.y() - documentViewport.y();
        return new LytPoint(
                documentRect.x() + x,
                documentRect.y() + y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!super.mouseScrolled(mouseX, mouseY, delta)) {
            return scrollbar.mouseScrolled(mouseX, mouseY, delta);
        }
        return true;
    }

    protected final void renderDocumentTooltip(RenderContext context, int mouseX, int mouseY, float partialTick) {
        var document = getDocumentWithLayout();
        // Render tooltip
        if (document != null && document.getHoveredElement() != null) {
            renderTooltip(context, mouseX, mouseY);
        }
    }

    private void renderTooltip(RenderContext context, int x, int y) {
        var docPos = getDocumentPoint(x, y);
        if (docPos == null) {
            return;
        }
        var document = getDocumentWithLayout();
        if (document == null) {
            return;
        }
        var hoveredElement = document.getHoveredElement();
        if (hoveredElement != null) {
            dispatchInteraction(
                    hoveredElement,
                    el -> el.getTooltip(docPos.x(), docPos.y()))
                    .ifPresent(tooltip -> renderTooltip(context, tooltip, x, y));
        }
    }

    private void renderTooltip(RenderContext context, GuideTooltip tooltip, int mouseX, int mouseY) {
        var lines = tooltip.getLines();

        if (lines.isEmpty()) {
            return;
        }

        int frameWidth = 0;
        int frameHeight = lines.size() == 1 ? -2 : 0;

        for (var line : lines) {
            frameWidth = Math.max(frameWidth, line.getWidth(context));
            frameHeight += line.getHeight(context);
        }

        if (!tooltip.getIcon().isEmpty()) {
            frameWidth += 18;
            frameHeight = Math.max(frameHeight, 18);
        }

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + frameWidth > this.width) {
            x -= 28 + frameWidth;
        }

        if (y + frameHeight + 6 > this.height) {
            y = this.height - frameHeight - 6;
        }

        int zOffset = 400;

        TooltipRenderUtil.renderTooltipBackground(x, y, frameWidth, frameHeight, zOffset);

        if (!tooltip.getIcon().isEmpty()) {
            x += 18;
        }

        context.push();
        context.translate(0, 0, zOffset);
        int currentY = y;

        // Render tooltip text and image first
        for (int i = 0; i < lines.size(); ++i) {
            var line = lines.get(i);
            line.render(context, x, currentY);
            currentY += line.getHeight(context) + (i == 0 ? 2 : 0);
        }

        // Then render tooltip decorations, items, etc.
        if (!tooltip.getIcon().isEmpty()) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();

            float screenZ = this.zLevel;
            float itemRenderZ = this.itemRender.zLevel;
            this.zLevel = zOffset;
            this.itemRender.zLevel = zOffset;

            this.itemRender.renderItemAndEffectIntoGUI(tooltip.getIcon(), x - 18, y);

            this.itemRender.zLevel = itemRenderZ;
            this.zLevel = screenZ;

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        context.pop();
    }

    @Override
    public @Nullable InteractiveElement getMouseCaptureTarget() {
        return mouseCaptureTarget;
    }

    @Override
    public void captureMouse(InteractiveElement element) {
        if (mouseCaptureTarget != element) {
            if (mouseCaptureTarget != null) {
                releaseMouseCapture(mouseCaptureTarget);
            }
            mouseCaptureTarget = element;
        }
    }

    @Override
    public void releaseMouseCapture(InteractiveElement element) {
        if (mouseCaptureTarget == element) {
            mouseCaptureTarget = null;
            element.mouseCaptureLost();
            if (mouseCaptureTarget != null) {
                throw new IllegalStateException("Element " + element + " recaptured the mouse in its release event");
            }
        }
    }

    private void releaseMouseCapture() {
        if (mouseCaptureTarget != null) {
            releaseMouseCapture(mouseCaptureTarget);
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        releaseMouseCapture();
    }

    protected int getMarginBottom() {
        return hasFooter() ? FULL_SCREEN_MARGIN : 0;
    }

    protected boolean hasFooter() {
        return false;
    }
}
