package guideme.compiler.tags;

import guideme.color.ARGB;
import guideme.color.ColorValue;
import guideme.color.ConstantColor;
import guideme.compiler.IdUtils;
import guideme.compiler.PageCompiler;
import guideme.document.LytErrorSink;
import guideme.libs.mdast.mdx.model.MdxJsxAttribute;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockStateMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * utilities for dealing with attributes of {@link MdxJsxElementFields}.
 */
public final class MdxAttrs {

    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{2}){3,4}$");

    private MdxAttrs() {
    }

    @Contract("_, _, _, _, !null -> !null")
    public static String getString(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute, String defaultValue) {
        var id = el.getAttribute(attribute);
        if (id == null) {
            return defaultValue;
        }

        if (id.hasStringValue()) {
            return id.getStringValue();
        } else if (id.hasExpressionValue()) {
            errorSink.appendError(compiler, "Expected string for '" + attribute + "' but got an expression.", el);
            return defaultValue;
        } else {
            return defaultValue;
        }
    }

    @Contract("_, _, _, _, !null -> !null")
    public static NBTTagCompound getCompoundTag(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute, NBTTagCompound defaultValue) {
        var nbtString = getString(compiler, errorSink, el, attribute, null);
        if (nbtString == null) {
            return defaultValue;
        }

        try {
            return JsonToNBT.getTagFromJson(nbtString);
        } catch (NBTException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return defaultValue;
        }
    }

    @Nullable
    public static ResourceLocation getRequiredId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute) {
        var id = getString(compiler, errorSink, el, attribute, null);
        if (id == null) {
            errorSink.appendError(compiler, "Missing " + attribute + " attribute.", el);
            return null;
        }

        id = id.trim(); // Trim leading/trailing whitespace for easier use

        try {
            return compiler.resolveId(id);
        } catch (IdUtils.ResourceLocationException e) {
            errorSink.appendError(compiler, "Malformed id " + id + ": " + e.getMessage(), el);
            return null;
        }
    }

    @Nullable
    public static Pair<ResourceLocation, Block> getRequiredBlockAndId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute) {
        var itemId = getRequiredId(compiler, errorSink, el, attribute);

        var resultItem = ForgeRegistries.BLOCKS.getValue(itemId);
        if (resultItem == null) {
            errorSink.appendError(compiler, "Missing block: " + itemId, el);
            return null;
        }
        return Pair.of(itemId, resultItem);
    }

    @Nullable
    public static Pair<ResourceLocation, Item> getRequiredItemAndId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute) {
        var itemId = getRequiredId(compiler, errorSink, el, attribute);

        var resultItem = ForgeRegistries.ITEMS.getValue(itemId);
        if (resultItem == null) {
            errorSink.appendError(compiler, "Missing item: " + itemId, el);
            return null;
        }
        return Pair.of(itemId, resultItem);
    }

    @Nullable
    public static Pair<ResourceLocation, EntityEntry> getRequiredEntityTypeAndId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute) {
        var entityTypeId = getRequiredId(compiler, errorSink, el, attribute);

        var resultType = ForgeRegistries.ENTITIES.getValue(entityTypeId);
        if (resultType == null) {
            errorSink.appendError(compiler, "Missing entity type: " + entityTypeId, el);
            return null;
        }
        return Pair.of(entityTypeId, resultType);
    }

    @Nullable
    public static Item getRequiredItem(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String attribute) {
        var result = getRequiredItemAndId(compiler, errorSink, el, attribute);
        if (result != null) {
            return result.getRight();
        }
        return null;
    }

    @Nullable
    public static ItemStack getRequiredItemStack(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var result = getRequiredItemStackAndId(compiler, errorSink, el);
        return result != null ? result.getValue() : null;
    }

    @Nullable
    public static Pair<ResourceLocation, ItemStack> getRequiredItemStackAndId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        return getRequiredItemStackAndId(compiler, errorSink, el, 0);
    }

    @Nullable
    public static Pair<ResourceLocation, ItemStack> getRequiredItemStackAndId(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, int defaultMetadata) {
        var itemAndId = getRequiredItemAndId(compiler, errorSink, el, "id");
        if (itemAndId == null) {
            return null;
        }

        var tag = MdxAttrs.getCompoundTag(compiler, errorSink, el, "tag", null);
        var metadata = getMetadata(compiler, errorSink, el, defaultMetadata);

        var stack = new ItemStack(itemAndId.getRight(), 1, metadata);
        stack.setTagCompound(tag);
        return Pair.of(itemAndId.getKey(), stack);
    }

    private static int getMetadata(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, int defaultMetadata) {
        var value = getString(compiler, errorSink, el, "meta", null);
        if (value == null) {
            value = getString(compiler, errorSink, el, "damage", null);
        }
        if (value == null) {
            return defaultMetadata;
        }

        value = value.trim();
        if ("*".equals(value)) {
            return OreDictionary.WILDCARD_VALUE;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed item metadata: '" + value + "'", el);
            return defaultMetadata;
        }
    }

    public static float getFloat(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, float defaultValue) {
        // Float attributes support expression syntax of bare style numbers too
        var attr = el.getAttribute(name);
        if (attr == null) {
            return defaultValue;
        }

        String attrValue;
        if (attr.hasExpressionValue()) {
            attrValue = attr.getExpressionValue();
        } else if (attr.hasStringValue()) {
            attrValue = attr.getStringValue();
        } else {
            return defaultValue;
        }

        try {
            return Float.parseFloat(attrValue);
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed floating point value: '" + attrValue + "'", el);
            return defaultValue;
        }
    }

    @Contract("_, _, _, _, !null -> !null")
    @Nullable
    public static Vector3f getVector3(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, @Nullable Vector3fc defaultValue) {
        var attrValue = getString(compiler, errorSink, el, name, null);
        if (attrValue == null) {
            return defaultValue != null ? new Vector3f(defaultValue) : null;
        }

        var parts = attrValue.trim().split("\\s+", 3);
        var result = new Vector3f();
        try {
            for (int i = 0; i < parts.length; i++) {
                float v = Float.parseFloat(parts[i]);
                result.setComponent(i, v);
            }
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed 3D vector: '" + attrValue + "'", el);
            return defaultValue != null ? new Vector3f(defaultValue) : null;
        }

        return result;
    }

    @Contract("_, _, _, _, !null -> !null")
    @Nullable
    public static BlockPos getBlockPos(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, @Nullable BlockPos defaultValue) {
        var attrValue = getString(compiler, errorSink, el, name, null);
        if (attrValue == null) {
            return defaultValue;
        }

        var parts = attrValue.trim().split("\\s+", 3);
        @SuppressWarnings("UnusedAssignment")
        int x = 0;
        int y = 0;
        int z = 0;
        try {
            x = Integer.parseInt(parts[0]);
            if (parts.length >= 2) {
                y = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                z = Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Invalid block position: '" + attrValue + "'", el);
            return defaultValue;
        }

        return new BlockPos.MutableBlockPos(x, y, z);
    }

    @Contract("_, _, _, _, !null -> !null")
    @Nullable
    public static Vector2f getVector2(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, @Nullable Vector2fc defaultValue) {
        var attrValue = getString(compiler, errorSink, el, name, null);
        if (attrValue == null) {
            return defaultValue != null ? new Vector2f(defaultValue) : null;
        }

        var parts = attrValue.trim().split("\\s+", 2);
        var result = new Vector2f();
        try {
            for (int i = 0; i < parts.length; i++) {
                float v = Float.parseFloat(parts[i]);
                result.setComponent(i, v);
            }
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed 2D vector: '" + attrValue + "'", el);
            return defaultValue != null ? new Vector2f(defaultValue) : null;
        }

        return result;
    }

    public static int getInt(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, int defaultValue) {
        var attrValue = getString(compiler, errorSink, el, name, null);
        if (attrValue == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(attrValue);
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed integer value: '" + attrValue + "'", el);
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Enum<T> & IStringSerializable> T getEnum(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, T defaultValue) {
        var stringValue = getString(compiler, errorSink, el, name, defaultValue.getName());

        var clazz = (Class<T>) defaultValue.getClass();
        for (var constant : clazz.getEnumConstants()) {
            if (constant.getName().equals(stringValue)) {
                return constant;
            }
        }

        errorSink.appendError(compiler, "Unrecognized option for attribute " + name + ": " + stringValue, el);
        return null;
    }

    public static IBlockState applyBlockStateProperties(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            IBlockState state) {
        for (var attrNode : el.attributes()) {
            if (!(attrNode instanceof MdxJsxAttribute attr)) {
                continue;
            }
            var attrName = attr.name;
            if (!attrName.startsWith("p:")) {
                continue;
            }
            var statePropertyName = attrName.substring("p:".length());
            var stateDefinition = state.getBlock().getBlockState();
            var property = stateDefinition.getProperty(statePropertyName);
            if (property == null) {
                errorSink.appendError(compiler, "block doesn't have property " + statePropertyName, el);
                continue;
            }
            state = applyProperty(compiler, errorSink, el, state, property, attr.getStringValue());
        }
        return state;
    }

    private static <T extends Comparable<T>> IBlockState applyProperty(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            IBlockState state, IProperty<T> property, String stringValue) {
        var propertyValue = property.parseValue(stringValue);
        if (!propertyValue.isPresent()) {
            errorSink.appendError(compiler, "Invalid value  for property " + property + ": " + stringValue, el);
            return state;
        }

        return state.withProperty(property, propertyValue.get());
    }

    public static BlockPos getPos(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var x = getInt(compiler, errorSink, el, "x", 0);
        var y = getInt(compiler, errorSink, el, "y", 0);
        var z = getInt(compiler, errorSink, el, "z", 0);
        return new BlockPos(x, y, z);
    }

    public static void getFloatPos(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            Vector3f out) {
        out.x = getFloat(compiler, errorSink, el, "x", out.x);
        out.y = getFloat(compiler, errorSink, el, "y", out.y);
        out.z = getFloat(compiler, errorSink, el, "z", out.z);
    }

    public static ColorValue getColor(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, ColorValue defaultColor) {
        var colorStr = getString(compiler, errorSink, el, name, null);
        if (colorStr != null) {
            if ("transparent".equals(colorStr)) {
                return new ConstantColor(0);
            }

            var m = COLOR_PATTERN.matcher(colorStr);
            if (!m.matches()) {
                errorSink.appendError(compiler, "Color must have format #AARRGGBB", el);
                return defaultColor;
            }

            int r, g, b;
            int a = 255;
            if (colorStr.length() == 7) {
                r = Integer.valueOf(colorStr.substring(1, 3), 16);
                g = Integer.valueOf(colorStr.substring(3, 5), 16);
                b = Integer.valueOf(colorStr.substring(5, 7), 16);
            } else {
                a = Integer.valueOf(colorStr.substring(1, 3), 16);
                r = Integer.valueOf(colorStr.substring(3, 5), 16);
                g = Integer.valueOf(colorStr.substring(5, 7), 16);
                b = Integer.valueOf(colorStr.substring(7, 9), 16);
            }
            return new ConstantColor(ARGB.color(a, r, g, b));
        }

        return defaultColor;
    }

    public static boolean getBoolean(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
            String name, boolean defaultValue) {
        var attribute = el.getAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }

        if (attribute.hasExpressionValue()) {
            var expressionValue = attribute.getExpressionValue();

            if (expressionValue.equals("true")) {
                return true;
            } else if (expressionValue.equals("false")) {
                return false;
            }
        }

        errorSink.appendError(compiler, name + " should be {true} or {false}", el);
        return defaultValue;
    }

    /**
     * Reads all attributes of the element starting with {@code p:} and builds a predicate testing a block states
     * properties against these values. Which attribute the block id is read from is configurable.
     */
    @Nullable
    public static Predicate<IBlockState> getRequiredBlockStatePredicate(
            PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String idAttribute) {
        var pair = getRequiredBlockAndId(compiler, errorSink, el, idAttribute);
        if (pair == null) {
            return null;
        }

        var block = pair.getRight();

        var predicate = BlockStateMatcher.forBlock(block);

        for (var attrNode : el.attributes()) {
            if (!(attrNode instanceof MdxJsxAttribute attr)) {
                continue;
            }
            var attrName = attr.name;
            if (!attrName.startsWith("p:")) {
                continue;
            }
            var statePropertyName = attrName.substring("p:".length());
            var stateDefinition = block.getBlockState();
            var property = stateDefinition.getProperty(statePropertyName);
            if (property == null) {
                errorSink.appendError(compiler, "block doesn't have property " + statePropertyName, el);
                continue;
            }

            String stringValue = attr.getStringValue();
            var maybePropertyValue = property.parseValue(stringValue);
            if (!maybePropertyValue.isPresent()) {
                errorSink.appendError(compiler, "Invalid value  for property " + property + ": " + stringValue, el);
                continue;
            }

            var propertyValue = maybePropertyValue.get();
            predicate.where(property, o -> Objects.equals(o, propertyValue));
        }

        return predicate;
    }
}
