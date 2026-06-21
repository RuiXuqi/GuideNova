package guideme.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import guideme.internal.util.JsonParseUtil;
import java.lang.reflect.Type;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GuiSpriteScaling extends IMetadataSection {
    Logger LOG = LoggerFactory.getLogger(GuiSpriteScaling.class);

    String SECTION_NAME = "gui";

    IMetadataSectionSerializer<GuiSpriteScaling> SERIALIZER = new IMetadataSectionSerializer<>() {
        @Override
        public String getSectionName() {
            return SECTION_NAME;
        }

        @Override
        public GuiSpriteScaling deserialize(JsonElement json, Type rType, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject root = json.getAsJsonObject();
            JsonObject scaling = JsonUtils.getJsonObject(root, "scaling");

            String type = JsonUtils.getString(scaling, "type");
            return switch (type) {
                case "stretch" -> new Stretch();
                case "tile" -> new Tile(
                        JsonParseUtil.getPositiveInt(scaling, "width"),
                        JsonParseUtil.getPositiveInt(scaling, "height"));
                case "nine_slice" -> {
                    int width = JsonParseUtil.getPositiveInt(scaling, "width");
                    int height = JsonParseUtil.getPositiveInt(scaling, "height");

                    if (!scaling.has("border"))
                        throw new JsonSyntaxException("Missing scaling/border");
                    JsonElement borderO = scaling.get("border");
                    NineSlice.Border border;
                    if (JsonUtils.isNumber(borderO)) {
                        int value = JsonParseUtil.getPositiveInt(borderO, "border");
                        border = new NineSlice.Border(value, value, value, value);
                    } else {
                        JsonObject object = JsonUtils.getJsonObject(borderO, "border");
                        border = new NineSlice.Border(
                                JsonParseUtil.getNonNegativeInt(object, "left"),
                                JsonParseUtil.getNonNegativeInt(object, "top"),
                                JsonParseUtil.getNonNegativeInt(object, "right"),
                                JsonParseUtil.getNonNegativeInt(object, "bottom"));
                    }

                    if (border.left() + border.right() >= width) {
                        throw new JsonSyntaxException("Nine-sliced texture has no horizontal center slice: "
                                + border.left() + " + " + border.right() + " >= " + width);
                    }

                    if (border.top() + border.bottom() >= height) {
                        throw new JsonSyntaxException("Nine-sliced texture has no vertical center slice: "
                                + border.top() + " + " + border.bottom() + " >= " + height);
                    }

                    yield new NineSlice(width, height, border);
                }
                default -> throw new JsonSyntaxException("Unknown gui scaling type: " + type);
            };
        }
    };

    GuiSpriteScaling DEFAULT = new Stretch();

    record NineSlice(int width, int height, Border border) implements GuiSpriteScaling {
        public record Border(int left, int top, int right, int bottom) {
        }
    }

    record Stretch() implements GuiSpriteScaling {
    }

    record Tile(int width, int height) implements GuiSpriteScaling {
    }
}
