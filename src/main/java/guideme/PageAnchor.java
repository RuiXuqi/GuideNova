package guideme;

import guideme.compiler.IdUtils;
import guideme.internal.network.ExtendedBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Points to a guidebook page with an optional anchor within that page.
 *
 * @param pageId
 * @param anchor ID of an anchor in the page.
 */
public record PageAnchor(ResourceLocation pageId, @Nullable String anchor) {
    public static PageAnchor page(ResourceLocation pageId) {
        return new PageAnchor(pageId, null);
    }

    public static PageAnchor parse(String anchor) {
        int sep = anchor.indexOf('#');
        ResourceLocation pageId = null;
        String fragment = null;
        if (sep != -1) {
            pageId = IdUtils.parse(anchor.substring(0, sep));
            fragment = anchor.substring(sep + 1);
        } else {
            pageId = IdUtils.parse(anchor);
        }
        return new PageAnchor(pageId, fragment);
    }

    @Override
    public String toString() {
        if (anchor != null) {
            return pageId.toString() + "#" + anchor;
        } else {
            return pageId.toString();
        }
    }

    public static PageAnchor read(ExtendedBuffer buffer) {
        var pageId = buffer.readResourceLocation();
        var anchor = buffer.readNullable(ExtendedBuffer::readString);
        return new PageAnchor(pageId, anchor);
    }

    public static void write(ExtendedBuffer buffer, PageAnchor anchor) {
        buffer.writeResourceLocation(anchor.pageId);
        buffer.writeNullable(anchor.anchor, ExtendedBuffer::writeString);
    }
}
