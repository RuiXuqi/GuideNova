package guideme.internal;

import guideme.Guide;
import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.command.GuideClientCommand;
import guideme.internal.hotkey.OpenGuideHotkey;
import guideme.internal.item.GuideItemDispatchModelLoader;
import guideme.internal.screen.BaseScreen;
import guideme.render.GuiAssets;
import guideme.render.GuiSpriteScaling;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class GuideMEClientProxy extends GuideMEServerProxy {
    @Override
    public void addGuideTooltip(ResourceLocation guideId, List<String> lines, ITooltipFlag tooltipFlag) {
        var guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            lines.add(TextFormatting.RED + GuidebookText.ItemInvalidGuideId.str());
            return;
        }

        for (ITextComponent component : guide.getItemSettings().tooltipLines()) {
            lines.add(component.getFormattedText());
        }
    }

    @Override
    public @Nullable String getGuideDisplayName(ResourceLocation guideId) {
        var guide = GuideRegistry.getById(guideId);
        if (guide != null) {
            var name = guide.getItemSettings().displayName();
            if (name.isPresent())
                return name.get().getFormattedText();
        }

        return null;
    }

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation id) {
        if (player == Minecraft.getMinecraft().player) {
            var guide = Guides.getById(id);
            if (guide == null) {
                player.sendMessage(GuidebookText.ItemInvalidGuideId.text(id.toString()));
                return false;
            } else {
                return GuideMEClient.openGuideAtPreviousPage(guide, guide.getStartPage());
            }
        }

        return super.openGuide(player, id);
    }

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation id, PageAnchor anchor) {
        if (player == Minecraft.getMinecraft().player) {
            var guide = Guides.getById(id);
            if (guide == null) {
                player.sendMessage(GuidebookText.ItemInvalidGuideId.text(id.toString()));
                return false;
            } else {
                if (anchor == null) {
                    return GuideMEClient.openGuideAtPreviousPage(guide, guide.getStartPage());
                }
                return GuideMEClient.openGuideAtAnchor(guide, anchor);
            }
        }

        return super.openGuide(player, id, anchor);
    }

    @Override
    public Stream<ResourceLocation> getAvailableGuides() {
        return Guides.getAll().stream().map(Guide::getId);
    }

    @Override
    public Stream<ResourceLocation> getAvailablePages(ResourceLocation guideId) {
        var guide = Guides.getById(guideId);
        if (guide == null) {
            return Stream.empty();
        }

        return guide.getPages().stream().map(ParsedGuidePage::getId);
    }

    @Override
    public @Nullable EntityPlayer getLocalPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        Minecraft.getMinecraft().metadataSerializer.registerMetadataSectionType(
                GuiSpriteScaling.SERIALIZER, GuiSpriteScaling.class);
        ModelLoaderRegistry.registerLoader(new GuideItemDispatchModelLoader());
        ClientRegistry.registerKeyBinding(OpenGuideHotkey.getHotkey());
        GuideOnStartup.init();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        ClientCommandHandler.instance.registerCommand(new GuideClientCommand());
        Minecraft.getMinecraft().getTextureManager().loadTickableTexture(
                GuiAssets.GUI_SPRITE_ATLAS, GuideMEClient.GUI_ATLAS);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(new GuideReloadListener());
        BaseScreen.installMouseCallbacks();
    }
}
