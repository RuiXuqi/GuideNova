package guideme.internal.datadriven;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import guideme.GuideItemSettings;
import guideme.color.ConstantColor;
import guideme.internal.util.JsonParseUtil;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

/**
 * Format for data driven guide definition files.
 */
public record DataDrivenGuide(GuideItemSettings itemSettings, String defaultLanguage,
        Map<ResourceLocation, ConstantColor> customColors) {
    public static DataDrivenGuide parse(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected guide definition to be an object");
        }
        var object = json.getAsJsonObject();
        return new DataDrivenGuide(
                JsonParseUtil.parseObject(object, "item_settings",
                        GuideItemSettings::parse, GuideItemSettings.DEFAULT),
                JsonUtils.getString(object, "default_language", "en_us"),
                JsonParseUtil.parseObject(object, "custom_colors",
                        DataDrivenGuide::parseCustomColors, Map.of()));
    }

    private static Map<ResourceLocation, ConstantColor> parseCustomColors(JsonObject object) {
        var result = new HashMap<ResourceLocation, ConstantColor>();
        for (var entry : object.entrySet()) {
            var colorId = JsonParseUtil.parseId(entry.getKey());
            var color = entry.getValue().getAsJsonObject();
            var darkMode = parseColor(JsonUtils.getString(color, "dark_mode"));
            var lightMode = parseColor(JsonUtils.getString(color, "light_mode"));
            result.put(colorId, new ConstantColor(lightMode, darkMode));
        }
        return result;
    }

    private static int parseColor(String value) {
        if (!value.startsWith("#")) {
            throw new JsonParseException("Not a color code: " + value);
        }

        try {
            return (int) Long.parseLong(value.substring(1), 16);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Exception parsing color code: " + e.getMessage(), e);
        }
    }
}
