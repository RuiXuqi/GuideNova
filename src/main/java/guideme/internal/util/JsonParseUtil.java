package guideme.internal.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import guideme.compiler.IdUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class JsonParseUtil {
    private JsonParseUtil() {
    }

    public static Optional<JsonElement> getOptional(JsonObject json, String memberName) {
        return json.has(memberName) ? Optional.of(json.get(memberName)) : Optional.empty();
    }

    public static Optional<JsonObject> getOptionalObject(JsonObject json, String memberName) {
        return json.has(memberName) ? Optional.of(json.getAsJsonObject(memberName)) : Optional.empty();
    }

    public static Optional<String> getOptionalString(JsonObject json, String memberName) {
        return getOptional(json, memberName).map(JsonElement::getAsString);
    }

    @Nullable
    public static <T> T parse(JsonObject json, String memberName, Function<JsonElement, T> parser) {
        return parse(json, memberName, parser, null);
    }

    public static <T> T parse(JsonObject json, String memberName, Function<JsonElement, T> parser, T defaultValue) {
        return json.has(memberName) ? parser.apply(json.get(memberName)) : defaultValue;
    }

    @Nullable
    public static <T> T parseObject(JsonObject json, String memberName, Function<JsonObject, T> parser) {
        return parseObject(json, memberName, parser, null);
    }

    public static <T> T parseObject(JsonObject json, String memberName, Function<JsonObject, T> parser,
            T defaultValue) {
        return json.has(memberName) ? parser.apply(json.getAsJsonObject(memberName)) : defaultValue;
    }

    public static <T> List<T> getList(JsonObject json, String memberName, Function<JsonElement, T> elementParser) {
        return parse(json, memberName, element -> {
            JsonArray array = element.getAsJsonArray();
            List<T> result = new ArrayList<>(array.size());
            array.forEach(e -> result.add(elementParser.apply(e)));
            return result;
        }, List.of());
    }

    public static int getPositiveInt(JsonObject json, String memberName) {
        return requirePositive(JsonUtils.getInt(json, memberName), memberName);
    }

    public static int getPositiveInt(JsonElement json, String memberName) {
        return requirePositive(JsonUtils.getInt(json, memberName), memberName);
    }

    public static int getNonNegativeInt(JsonObject json, String memberName) {
        return requireNonNegative(JsonUtils.getInt(json, memberName), memberName);
    }

    public static int getNonNegativeInt(JsonElement json, String memberName) {
        return requireNonNegative(JsonUtils.getInt(json, memberName), memberName);
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0)
            throw new JsonSyntaxException(name + " must be positive but received " + value);
        return value;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0)
            throw new JsonSyntaxException(name + " must be non-negative but received " + value);
        return value;
    }

    public static ResourceLocation parseId(String value) {
        try {
            return IdUtils.parse(value);
        } catch (IdUtils.ResourceLocationException e) {
            throw new JsonParseException("Invalid resource location: " + value, e);
        }
    }
}
