package guideme.internal.network;

import guideme.PageAnchor;
import guideme.internal.GuideMEProxy;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OpenGuideRequest implements IExtendedBufferMessage {
    private ResourceLocation guideId;
    private Optional<PageAnchor> pageAnchor;

    public OpenGuideRequest() {
    }

    public OpenGuideRequest(ResourceLocation guideId, Optional<PageAnchor> pageAnchor) {
        this.guideId = guideId;
        this.pageAnchor = pageAnchor;
    }

    public OpenGuideRequest(ResourceLocation guideId) {
        this(guideId, Optional.empty());
    }

    @Override
    public void toBytes(ExtendedBuffer buffer) {
        buffer.writeResourceLocation(guideId);
        buffer.writeOptional(pageAnchor, PageAnchor::write);
    }

    @Override
    public void fromBytes(ExtendedBuffer buffer) {
        this.guideId = buffer.readResourceLocation();
        this.pageAnchor = buffer.readOptional(PageAnchor::read);
    }

    public static class Handler implements IMessageHandler<OpenGuideRequest, IMessage> {
        @Nullable
        @Override
        public IMessage onMessage(OpenGuideRequest packet, MessageContext contextSource) {
            handleClient(packet);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void handleClient(OpenGuideRequest packet) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                var player = GuideMEProxy.instance().getLocalPlayer();
                var anchor = packet.pageAnchor.orElse(null);
                if (anchor != null) {
                    GuideMEProxy.instance().openGuide(player, packet.guideId, anchor);
                } else {
                    GuideMEProxy.instance().openGuide(player, packet.guideId);
                }
            });
        }
    }
}
