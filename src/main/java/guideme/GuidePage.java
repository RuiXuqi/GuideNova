package guideme;

import guideme.document.block.LytDocument;
import net.minecraft.util.ResourceLocation;

public record GuidePage(String sourcePack, ResourceLocation id, LytDocument document) {
}
