package guideme.internal.hotkey;

import com.google.common.base.Strings;
import guideme.Guide;
import guideme.PageAnchor;
import guideme.indices.ItemIndex;
import guideme.internal.GuideMEClient;
import guideme.internal.GuideRegistry;
import guideme.internal.GuidebookText;
import guideme.internal.Reference;
import guideme.internal.screen.GuideScreen;
import guideme.ui.GuideUiHost;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

/**
 * Adds a "Hold X to show guide" tooltip
 */
public final class OpenGuideHotkey {
    private static final KeyBinding OPEN_GUIDE_MAPPING = new KeyBinding(
            "key.guideme.guide", KeyConflictContext.GUI, Keyboard.KEY_G,
            "key.guideme.category");

    private static final int TICKS_TO_OPEN = 10;

    private static boolean newTick = true;

    // The previous item the tooltip was being shown for
    private static ResourceLocation previousItemId;
    private static final List<FoundPage> guidebookPages = new ArrayList<>();
    // Full ticks since the button was held (reduces slowly when not held)
    private static int ticksKeyHeld;
    // Is the key to open currently held
    private static boolean holding;

    private OpenGuideHotkey() {
    }

    private record FoundPage(Guide guide, PageAnchor page) {
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
    private static class OpenGuideHotkeyEvents {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent evt) {
            // Ignore events fired for anything but the current local player,
            // for example while building the search tree for the creative menu
            if (evt.getEntity() != Minecraft.getMinecraft().player) {
                return;
            }
            handleTooltip(evt.getItemStack(), evt.getFlags(), evt.getToolTip());
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent evt) {
            if (evt.phase == TickEvent.Phase.END) {
                newTick = true;
            }
        }
    }

    private static void handleTooltip(ItemStack itemStack, ITooltipFlag tooltipFlag, List<String> lines) {
        // Player didn't bind the key
        if (!isKeyBound()) {
            holding = false;
            ticksKeyHeld = 0;
            return;
        }

        // This should only update once per client-tick
        if (newTick) {
            newTick = false;
            update(itemStack);
        }

        if (guidebookPages.isEmpty()) {
            return;
        }

        var guide = guidebookPages.getFirst().guide();
        var pageAnchor = guidebookPages.getFirst().page();

        // Don't do anything if we're already on the target page
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuideScreen guideScreen
                && guideScreen.getGuide() == guide
                && guideScreen.getCurrentPageId().equals(pageAnchor.pageId())) {
            return;
        }

        // Compute the progress value between [0,1]
        float progress = ticksKeyHeld;
        if (holding) {
            progress += minecraft.getTickLength();
        } else {
            progress -= minecraft.getTickLength();
        }
        progress /= (float) TICKS_TO_OPEN;
        var component = makeProgressBar(MathHelper.clamp(progress, 0, 1));
        // It may happen that we're the only line
        if (lines.isEmpty()) {
            lines.add(component);
        } else {
            lines.add(1, component);
        }
    }

    private static String makeProgressBar(float progress) {
        var minecraft = Minecraft.getMinecraft();

        var holdW = GuidebookText.HoldToShow
                .text(TextFormatting.GRAY + getHotkey().getDisplayName())
                .setStyle(new Style().setColor(TextFormatting.DARK_GRAY)).getFormattedText();

        var fontRenderer = minecraft.fontRenderer;
        var charWidth = fontRenderer.getStringWidth("|");
        var tipWidth = fontRenderer.getStringWidth(holdW);

        var total = tipWidth / charWidth;
        var current = (int) (progress * total);

        if (progress > 0) {
            var result = TextFormatting.GRAY + Strings.repeat("|", current);
            if (progress < 1)
                result += TextFormatting.DARK_GRAY + Strings.repeat("|", total - current);
            return result;
        }

        return holdW;
    }

    private static void update(ItemStack itemStack) {
        var itemId = itemStack.getItem().getRegistryName();

        if (!Objects.equals(itemId, previousItemId)) {
            previousItemId = itemId;
            guidebookPages.clear();
            ticksKeyHeld = 0;

            if (itemId == null) {
                return;
            }

            for (var guide : GuideRegistry.getAll()) {
                if (!guide.isAvailableToOpenHotkey()) {
                    continue;
                }

                var itemIndex = guide.getIndex(ItemIndex.class);
                var page = itemIndex.get(itemId);
                if (page != null) {
                    guidebookPages.add(new FoundPage(guide, page));
                }
            }
        }

        // Bump the ticks the key was held
        holding = isKeyHeld();
        if (holding) {
            if (ticksKeyHeld < TICKS_TO_OPEN && ++ticksKeyHeld == TICKS_TO_OPEN) {
                if (!guidebookPages.isEmpty()) {
                    var foundPage = guidebookPages.getFirst();
                    var guide = foundPage.guide();

                    if (Minecraft.getMinecraft().currentScreen instanceof GuideUiHost uiHost
                            && uiHost.getGuide() == guide) {
                        uiHost.navigateTo(foundPage.page());
                    } else {
                        GuideMEClient.openGuideAtAnchor(guide, foundPage.page());
                    }
                    // Reset the ticks held immediately to avoid reopening another page if
                    // our cursors lands on an item
                    ticksKeyHeld = 0;
                    holding = false;
                }
            } else if (ticksKeyHeld > TICKS_TO_OPEN) {
                ticksKeyHeld = TICKS_TO_OPEN;
            }
        } else {
            ticksKeyHeld = Math.max(0, ticksKeyHeld - 2);
        }
    }

    /**
     * This circumvents any current UI key handling.
     */
    private static boolean isKeyHeld() {
        int keyCode = getHotkey().getKeyCode();

        return Keyboard.isKeyDown(keyCode);
    }

    private static boolean isKeyBound() {
        return OPEN_GUIDE_MAPPING.getKeyCode() != Keyboard.KEY_NONE;
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUIDE_MAPPING;
    }
}
