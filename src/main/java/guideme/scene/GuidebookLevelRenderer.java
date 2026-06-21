package guideme.scene;

import guideme.color.LightDarkMode;
import guideme.scene.annotation.InWorldAnnotation;
import guideme.scene.annotation.InWorldAnnotationRenderer;
import guideme.scene.level.GuidebookLevel;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

public class GuidebookLevelRenderer {

    private static GuidebookLevelRenderer instance;

    public static GuidebookLevelRenderer getInstance() {
        if (instance == null) {
            instance = new GuidebookLevelRenderer();
        }
        return instance;
    }

    public void render(
            GuidebookLevel level, CameraSettings cameraSettings,
            Collection<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        level.onRenderFrame();

        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        CameraSettings.multiply(cameraSettings.getProjectionMatrix());

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        CameraSettings.multiply(cameraSettings.getViewMatrix());

        renderContent(level);

        InWorldAnnotationRenderer.render(annotations, lightDarkMode);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW); // Reset to default

        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        RenderHelper.disableStandardItemLighting(); // Reset to GUI lighting
    }

    /**
     * Render without camera setup. Used by scene export and tests that already prepared the GL matrices.
     */
    public void renderContent(GuidebookLevel level) {
        var mc = Minecraft.getMinecraft();
        mc.entityRenderer.enableLightmap();
        GlStateManager.enableRescaleNormal();

        renderBlocks(level, BlockRenderLayer.SOLID);
        renderBlocks(level, BlockRenderLayer.CUTOUT_MIPPED);
        renderBlocks(level, BlockRenderLayer.CUTOUT);

        renderBlockEntities(level, level.getPartialTick(), 0);
        renderEntities(level, level.getPartialTick(), 0);

        renderBlocks(level, BlockRenderLayer.TRANSLUCENT);

        GlStateManager.depthMask(false);
        renderBlockEntities(level, level.getPartialTick(), 1);
        renderEntities(level, level.getPartialTick(), 1);
        GlStateManager.depthMask(true);

        ForgeHooksClient.setRenderLayer(null);
        ForgeHooksClient.setRenderPass(-1);

        GlStateManager.disableRescaleNormal();
        mc.entityRenderer.disableLightmap();
    }

    private void renderBlocks(GuidebookLevel level, BlockRenderLayer layer) {
        ForgeHooksClient.setRenderLayer(layer);
        RenderHelper.disableStandardItemLighting();

        var mc = Minecraft.getMinecraft();
        var dispatcher = mc.getBlockRendererDispatcher();
        var tess = Tessellator.getInstance();
        var buffer = tess.getBuffer();

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.depthMask(true);
        }

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(0, 0, 0);

        var it = level.getFilledBlocks().iterator();
        while (it.hasNext()) {
            var pos = it.next();
            var state = level.getBlockState(pos);
            if (state.getBlock().canRenderInLayer(state, layer)) {
                dispatcher.renderBlock(state, pos, level, buffer);
            }
        }

        tess.draw();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        ForgeHooksClient.setRenderLayer(null);
    }

    private void renderBlockEntities(GuidebookLevel level, float partialTick, int pass) {
        ForgeHooksClient.setRenderPass(pass);
        RenderHelper.enableStandardItemLighting();

        var mc = Minecraft.getMinecraft();
        var dispatcher = TileEntityRendererDispatcher.instance;
        dispatcher.prepare(level, mc.getTextureManager(), mc.fontRenderer, mc.getRenderViewEntity(), null, partialTick);
        TileEntityRendererDispatcher.staticPlayerX = 0;
        TileEntityRendererDispatcher.staticPlayerY = 0;
        TileEntityRendererDispatcher.staticPlayerZ = 0;

        dispatcher.preDrawBatch();
        for (var blockEntity : level.getBlockEntities()) {
            if (blockEntity.shouldRenderInPass(pass)) {
                var pos = blockEntity.getPos();
                dispatcher.entityX = pos.getX() + 0.5;
                dispatcher.entityY = pos.getY() + 0.5;
                dispatcher.entityZ = pos.getZ() + 0.5;
                dispatcher.render(blockEntity, partialTick, -1);
            }
        }
        dispatcher.drawBatch(0);

        GlStateManager.enableCull();
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    private void renderEntities(GuidebookLevel level, float partialTick, int pass) {
        ForgeHooksClient.setRenderPass(pass);
        RenderHelper.enableStandardItemLighting();

        var mc = Minecraft.getMinecraft();
        var manager = mc.getRenderManager();
        manager.cacheActiveRenderInfo(level, mc.fontRenderer, mc.getRenderViewEntity(), null, mc.gameSettings,
                partialTick);
        manager.setRenderPosition(0, 0, 0);
        manager.setPlayerViewY(180);

        for (var entity : level.getEntitiesForRendering()) {
            if (entity.shouldRenderInPass(pass)) {
                int light = entity.getBrightnessForRender();
                if (entity.isBurning()) {
                    light = 15728880;
                }
                int lightU = light % 65536;
                int lightV = light / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightU, lightV);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                manager.renderEntity(
                        entity,
                        entity.posX, entity.posY, entity.posZ,
                        entity.rotationYaw,
                        partialTick,
                        false // Show collision box
                );
            }
        }
    }
}
