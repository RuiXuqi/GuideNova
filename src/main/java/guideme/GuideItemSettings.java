package guideme;

import com.google.gson.JsonObject;
import guideme.internal.util.JsonParseUtil;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * Configuration settings for the automatically generated guide item.
 */
public record GuideItemSettings(Optional<ITextComponent> displayName,
        List<ITextComponent> tooltipLines, Optional<ResourceLocation> itemModel) {

    public static GuideItemSettings DEFAULT = new GuideItemSettings(Optional.empty(), List.of(), Optional.empty());

    public static GuideItemSettings parse(JsonObject object) {
        return new GuideItemSettings(
                JsonParseUtil.getOptional(object, "display_name")
                        .map(element -> ITextComponent.Serializer.jsonToComponent(element.toString())),
                JsonParseUtil.getList(object, "tooltip_lines",
                        element -> ITextComponent.Serializer.jsonToComponent(element.toString())),
                JsonParseUtil.getOptionalString(object, "model").map(JsonParseUtil::parseId));
    }
}
