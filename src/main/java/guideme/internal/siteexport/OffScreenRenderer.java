package guideme.internal.siteexport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

// TODO
public final class OffScreenRenderer implements AutoCloseable {
    private final int width;
    private final int height;
    private final Framebuffer framebuffer;

    public OffScreenRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        this.framebuffer = new Framebuffer(width, height, true);
        this.framebuffer.setFramebufferColor(0, 0, 0, 0);
    }

    public byte[] captureAsPng(Runnable render) {
        var minecraft = Minecraft.getMinecraft();

        framebuffer.bindFramebuffer(true);
        framebuffer.framebufferClear();
        GlStateManager.viewport(0, 0, width, height);

        render.run();

        var buffer = GLAllocation.createDirectIntBuffer(width * height);
        GlStateManager.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        var pixels = new int[width * height];
        buffer.get(pixels);

        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (var y = 0; y < height; y++) {
            var sourceY = height - y - 1;
            for (var x = 0; x < width; x++) {
                var pixel = pixels[sourceY * width + x];
                var r = pixel & 0xFF;
                var g = pixel >> 8 & 0xFF;
                var b = pixel >> 16 & 0xFF;
                var a = pixel >> 24 & 0xFF;
                image.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
            }
        }

        minecraft.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode scene PNG", e);
        }
    }

    @Override
    public void close() {
        framebuffer.deleteFramebuffer();
    }
}
