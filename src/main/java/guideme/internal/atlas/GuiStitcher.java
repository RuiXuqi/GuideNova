package guideme.internal.atlas;

import java.util.Comparator;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiStitcher extends Stitcher {
    public static final Comparator<Holder> HOLDER_COMPARATOR = Comparator
            .<Holder>comparingInt(holder -> -holder.getHeight())
            .thenComparingInt(holder -> -holder.getWidth())
            .thenComparing(holder -> new ResourceLocation(holder.getAtlasSprite().getIconName()));

    public GuiStitcher(int maxWidth, int maxHeight, int maxTileDimension, int mipmapLevel) {
        super(maxWidth, maxHeight, maxTileDimension, mipmapLevel);
    }

    @Override
    public void addSprite(TextureAtlasSprite sprite) {
        var holder = new Holder(sprite, this.mipmapLevelStitcher);
        if (holder.isRotated())
            holder.rotate();
        this.setStitchHolders.add(holder);
    }

    /// Internal method for mixin use
    public boolean addToStorage(Holder holder) {
        for (Slot slot : this.stitchSlots) {
            if (slot.addSlot(holder)) {
                return true;
            }
        }

        return this.expand(holder);
    }

    private boolean expand(Holder holder) {
        int oldPowerOfTwoWidth = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth);
        int oldPowerOfTwoHeight = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight);
        int newPowerOfTwoWidth = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth + holder.getWidth());
        int newPowerOfTwoHeight = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight + holder.getHeight());
        boolean canGrowWidth = newPowerOfTwoWidth <= this.maxWidth;
        boolean canGrowHeight = newPowerOfTwoHeight <= this.maxHeight;

        if (!canGrowWidth && !canGrowHeight)
            return false;

        boolean widthWouldGrow = canGrowWidth && oldPowerOfTwoWidth != newPowerOfTwoWidth;
        boolean heightWouldGrow = canGrowHeight && oldPowerOfTwoHeight != newPowerOfTwoHeight;
        boolean growWidth = widthWouldGrow ^ heightWouldGrow ?
        // Forge: Fix stitcher not expanding entire height before growing width, and (potentially) growing larger then
        // the max size.
                !widthWouldGrow && canGrowWidth : canGrowWidth && oldPowerOfTwoWidth <= oldPowerOfTwoHeight;

        Slot slot;
        if (growWidth) {
            if (this.currentHeight == 0)
                this.currentHeight = newPowerOfTwoHeight;
            slot = new Slot(this.currentWidth, 0, newPowerOfTwoWidth - this.currentWidth, this.currentHeight);
            this.currentWidth = newPowerOfTwoWidth;
        } else {
            slot = new Slot(0, this.currentHeight, this.currentWidth, newPowerOfTwoHeight - this.currentHeight);
            this.currentHeight = newPowerOfTwoHeight;
        }

        slot.addSlot(holder);
        this.stitchSlots.add(slot);
        return true;
    }
}
