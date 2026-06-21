package guideme.internal;

import guideme.compiler.IdUtils;
import guideme.internal.item.GuideItem;
import guideme.internal.network.OpenGuideRequest;
import java.util.Objects;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        // org.apache.commons.text is added this version
        dependencies = "required-after:cleanroom@[0.5.13-alpha,)", guiFactory = "guideme.internal.GuideMEConfigGuiFactory", customProperties = {
                @Mod.CustomProperty(k = "license", v = "See GitHub repository for details"),
                @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/RuiXuqi/GuideNova/issues")
        })
public class GuideME {
    @SidedProxy(serverSide = "guideme.internal.GuideMEServerProxy", clientSide = "guideme.internal.GuideMEClientProxy")
    static GuideMEProxy PROXY;

    public static final GuideItem GUIDE_ITEM = new GuideItem();

    private SimpleNetworkWrapper networkChannel;

    @SuppressWarnings("unused")
    @Mod.Instance
    private static GuideME INSTANCE;

    @Mod.EventBusSubscriber
    private static class GuideMEEvents {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            event.getRegistry().register(GUIDE_ITEM);
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PROXY.preInit(event);

        networkChannel = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID + "_channel");
        networkChannel.registerMessage(
                OpenGuideRequest.Handler.class,
                OpenGuideRequest.class,
                0,
                Side.CLIENT);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PROXY.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PROXY.serverStarting(event);
    }

    public void sendPacket(EntityPlayerMP target, OpenGuideRequest request) {
        networkChannel.sendTo(request, target);
    }

    public static ResourceLocation makeId(String path) {
        return IdUtils.build(Reference.MOD_ID, path);
    }

    public static GuideME instance() {
        return Objects.requireNonNull(INSTANCE, "instance");
    }
}
