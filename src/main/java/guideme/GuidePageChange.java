package guideme;

import guideme.compiler.ParsedGuidePage;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record GuidePageChange(
        @Nullable String language,
        ResourceLocation pageId,
        @Nullable ParsedGuidePage oldPage,
        @Nullable ParsedGuidePage newPage) {
}
