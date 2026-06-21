package guideme.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

record SNBTPrinter(String indentation, int depth, List<String> path) {

    private static final String INDENTATION = "    ";
    private static final String NAME_VALUE_SEPARATOR = ":";
    private static final String ELEMENT_SEPARATOR = ",";
    private static final String LIST_OPEN = "[";
    private static final String LIST_CLOSE = "]";
    private static final String LIST_TYPE_SEPARATOR = ";";
    private static final String ELEMENT_SPACING = " ";
    private static final String STRUCT_OPEN = "{";
    private static final String STRUCT_CLOSE = "}";
    private static final String NEWLINE = "\n";
    private static final String EMPTY = "";
    private static final String LIST_PATH = LIST_OPEN + LIST_CLOSE;
    private static final String STRUCT_PATH = STRUCT_OPEN + STRUCT_CLOSE;

    private static final Map<String, List<String>> KEY_ORDER = new HashMap<>();
    private static final Set<String> NO_INDENTATION = new HashSet<>(Arrays.asList(
            STRUCT_PATH + ".size." + LIST_PATH,
            STRUCT_PATH + ".data." + LIST_PATH + "." + STRUCT_PATH,
            STRUCT_PATH + ".palette." + LIST_PATH + "." + STRUCT_PATH,
            STRUCT_PATH + ".entities." + LIST_PATH + "." + STRUCT_PATH));

    static {
        KEY_ORDER.put(STRUCT_PATH,
                Arrays.asList("DataVersion", "author", "size", "data", "entities", "palette", "palettes"));
        KEY_ORDER.put(STRUCT_PATH + ".data." + LIST_PATH + "." + STRUCT_PATH, Arrays.asList("pos", "state", "nbt"));
        KEY_ORDER.put(STRUCT_PATH + ".entities." + LIST_PATH + "." + STRUCT_PATH, Arrays.asList("blockPos", "pos"));
    }

    SNBTPrinter() {
        this(INDENTATION, 0, new ArrayList<>());
    }

    public String print(NBTBase tag) {
        return switch (tag.getId()) {
            case Constants.NBT.TAG_END -> EMPTY;
            case Constants.NBT.TAG_LIST -> printList((NBTTagList) tag);
            case Constants.NBT.TAG_COMPOUND -> printCompound((NBTTagCompound) tag);
            case Constants.NBT.TAG_BYTE_ARRAY, Constants.NBT.TAG_INT_ARRAY, Constants.NBT.TAG_LONG_ARRAY ->
                printArray(tag);
            default -> tag.toString();
        };
    }

    private String printList(NBTTagList tag) {
        if (tag.isEmpty())
            return LIST_PATH;

        var builder = new StringBuilder(LIST_OPEN);
        pushPath(LIST_PATH);
        String childIndentation = NO_INDENTATION.contains(pathString()) ? EMPTY : indentation;
        if (!childIndentation.isEmpty()) {
            builder.append(NEWLINE);
        }

        for (int i = 0; i < tag.tagCount(); i++) {
            builder.repeat(childIndentation, depth + 1);
            builder.append(new SNBTPrinter(childIndentation, depth + 1, path).print(tag.get(i)));
            if (i != tag.tagCount() - 1) {
                builder.append(ELEMENT_SEPARATOR).append(childIndentation.isEmpty() ? ELEMENT_SPACING : NEWLINE);
            }
        }

        if (!childIndentation.isEmpty()) {
            builder.append(NEWLINE).repeat(childIndentation, depth);
        }

        builder.append(LIST_CLOSE);
        popPath();
        return builder.toString();
    }

    private String printCompound(NBTTagCompound tag) {
        if (tag.isEmpty())
            return STRUCT_PATH;

        var builder = new StringBuilder(STRUCT_OPEN);
        pushPath(STRUCT_PATH);
        String childIndentation = NO_INDENTATION.contains(pathString()) ? EMPTY : indentation;
        if (!childIndentation.isEmpty()) {
            builder.append(NEWLINE);
        }

        var keys = getKeys(tag);
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            pushPath(key);
            builder.repeat(childIndentation, depth + 1)
                    .append(NBTTagCompound.handleEscape(key))
                    .append(NAME_VALUE_SEPARATOR)
                    .append(ELEMENT_SPACING)
                    .append(new SNBTPrinter(childIndentation, depth + 1, path).print(tag.getTag(key)));
            popPath();

            if (i != keys.size() - 1) {
                builder.append(ELEMENT_SEPARATOR).append(childIndentation.isEmpty() ? ELEMENT_SPACING : NEWLINE);
            }
        }

        if (!childIndentation.isEmpty()) {
            builder.append(NEWLINE).repeat(childIndentation, depth);
        }

        builder.append(STRUCT_CLOSE);
        popPath();
        return builder.toString();
    }

    private static String printArray(NBTBase tag) {
        String text = tag.toString();
        int typeSeparator = text.indexOf(LIST_TYPE_SEPARATOR);
        if (typeSeparator < 0 || typeSeparator + 1 >= text.length()
                || text.startsWith(LIST_CLOSE, typeSeparator + 1)) {
            return text;
        }
        return text.substring(0, typeSeparator + 1)
                + ELEMENT_SPACING
                + text.substring(typeSeparator + 1).replace(ELEMENT_SEPARATOR, ELEMENT_SEPARATOR + ELEMENT_SPACING);
    }

    private void popPath() {
        path.removeLast();
    }

    private void pushPath(String key) {
        path.add(key);
    }

    private List<String> getKeys(NBTTagCompound tag) {
        var remainingKeys = new HashSet<>(tag.getKeySet());
        var result = new ArrayList<String>();
        var orderedKeys = KEY_ORDER.get(pathString());
        if (orderedKeys != null) {
            for (String key : orderedKeys) {
                if (remainingKeys.remove(key)) {
                    result.add(key);
                }
            }

            if (!remainingKeys.isEmpty()) {
                var sortedKeys = new ArrayList<>(remainingKeys);
                Collections.sort(sortedKeys);
                result.addAll(sortedKeys);
            }
        } else {
            result.addAll(remainingKeys);
            Collections.sort(result);
        }

        return result;
    }

    private String pathString() {
        return String.join(".", path);
    }
}
