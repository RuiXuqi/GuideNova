package guideme.internal;

import guideme.PageAnchor;
import guideme.internal.command.GuideCommand;
import guideme.internal.command.StructureCommands;
import guideme.internal.network.OpenGuideRequest;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.jetbrains.annotations.Nullable;

public class GuideMEServerProxy implements GuideMEProxy {
    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation id) {
        if (player instanceof EntityPlayerMP serverPlayer) {
            GuideME.instance().sendPacket(serverPlayer, new OpenGuideRequest(id));
            return true;
        }

        return false;
    }

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation guideId, @Nullable PageAnchor anchor) {
        if (player instanceof EntityPlayerMP serverPlayer) {
            GuideME.instance().sendPacket(serverPlayer, new OpenGuideRequest(guideId, Optional.ofNullable(anchor)));
            return true;
        }

        return false;
    }

    @Override
    public Stream<ResourceLocation> getAvailableGuides() {
        return Stream.empty();
    }

    @Override
    public Stream<ResourceLocation> getAvailablePages(ResourceLocation guideId) {
        return Stream.empty();
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        GuideMEConfig.init(event.getSuggestedConfigurationFile(), event.getSide());
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        var command = new GuideCommand();
        event.registerServerCommand(command);
        if (event.getSide().isClient())
            StructureCommands.register(command);
    }
}
