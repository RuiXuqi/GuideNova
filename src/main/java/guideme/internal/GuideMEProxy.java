package guideme.internal;

import guideme.PageAnchor;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.jetbrains.annotations.Nullable;

public interface GuideMEProxy {
    static GuideMEProxy instance() {
        return GuideME.PROXY;
    }

    default void addGuideTooltip(ResourceLocation guideId, List<String> lines, ITooltipFlag tooltipFlag) {
    }

    @Nullable
    default String getGuideDisplayName(ResourceLocation guideId) {
        return null;
    }

    boolean openGuide(EntityPlayer player, ResourceLocation guideId);

    boolean openGuide(EntityPlayer player, ResourceLocation guideId, PageAnchor anchor);

    Stream<ResourceLocation> getAvailableGuides();

    Stream<ResourceLocation> getAvailablePages(ResourceLocation guideId);

    @Nullable
    default EntityPlayer getLocalPlayer() {
        return null;
    }

    default void preInit(FMLPreInitializationEvent event) {
    }

    default void init(FMLInitializationEvent event) {
    }

    default void postInit(FMLPostInitializationEvent event) {
    }

    default void serverStarting(FMLServerStartingEvent event) {
    }
}
