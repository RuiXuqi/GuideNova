package guideme.internal.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SNBTUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SNBTUtil.class);
    private static final Comparator<NBTTagList> YXZ_LISTTAG_INT_COMPARATOR = Comparator
            .comparingInt((NBTTagList tag) -> tag.getIntAt(1))
            .thenComparingInt(tag -> tag.getIntAt(0))
            .thenComparingInt(tag -> tag.getIntAt(2));
    private static final Comparator<NBTTagList> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator
            .comparingDouble((NBTTagList tag) -> tag.getDoubleAt(1))
            .thenComparingDouble(tag -> tag.getDoubleAt(0))
            .thenComparingDouble(tag -> tag.getDoubleAt(2));
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final String KEY_VALUE_SEPARATOR = ":";

    private SNBTUtil() {
    }

    public static NBTTagCompound snbtToStructure(String text) throws NBTException {
        return unpackStructureTemplate(JsonToNBT.getTagFromJson(text));
    }

    private static NBTTagCompound unpackStructureTemplate(NBTTagCompound compound) {
        var palette = compound.getTagList("palette", Constants.NBT.TAG_STRING);
        var palette2NameMap = new LinkedHashMap<String, NBTTagCompound>();
        for (int i = 0; i < palette.tagCount(); i++) {
            String packedStateName = palette.getStringTagAt(i);
            palette2NameMap.put(packedStateName, unpackBlockState(packedStateName));
        }

        if (compound.hasKey("palettes", Constants.NBT.TAG_LIST)) {
            var packedPalettes = compound.getTagList("palettes", Constants.NBT.TAG_COMPOUND);
            var unpackedPalettes = new NBTTagList();
            for (int i = 0; i < packedPalettes.tagCount(); i++) {
                var packedPaletteMap = packedPalettes.getCompoundTagAt(i);
                var unpackedPalette = new NBTTagList();
                for (String packedStateName : palette2NameMap.keySet()) {
                    unpackedPalette.appendTag(unpackBlockState(packedPaletteMap.getString(packedStateName)));
                }
                unpackedPalettes.appendTag(unpackedPalette);
            }
            compound.setTag("palettes", unpackedPalettes);
            compound.removeTag("palette");
        } else {
            var unpackedPalette = new NBTTagList();
            for (var blockState : palette2NameMap.values()) {
                unpackedPalette.appendTag(blockState);
            }
            compound.setTag("palette", unpackedPalette);
        }

        if (compound.hasKey("data", Constants.NBT.TAG_LIST)) {
            var paletteName2IndexMap = new Object2IntOpenHashMap<String>();
            paletteName2IndexMap.defaultReturnValue(-1);
            for (int i = 0; i < palette.tagCount(); i++) {
                paletteName2IndexMap.put(palette.getStringTagAt(i), i);
            }

            var blocks = compound.getTagList("data", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < blocks.tagCount(); i++) {
                var blockTag = blocks.getCompoundTagAt(i);
                String packedStateName = blockTag.getString("state");
                int stateIndex = paletteName2IndexMap.getInt(packedStateName);
                if (stateIndex == -1)
                    throw new IllegalStateException("Entry " + packedStateName + " missing from palette");

                blockTag.setInteger("state", stateIndex);
            }

            compound.setTag("blocks", blocks);
            compound.removeTag("data");
        }

        return compound;
    }

    private static NBTTagCompound unpackBlockState(String blockStateStr) {
        var blockState = new NBTTagCompound();
        int start = blockStateStr.indexOf(PROPERTIES_START);
        String blockName;
        if (start >= 0) {
            blockName = blockStateStr.substring(0, start);
            var properties = new NBTTagCompound();
            if (start + 2 <= blockStateStr.length()) {
                String propertiesStr = blockStateStr.substring(start + 1,
                        blockStateStr.indexOf(PROPERTIES_END, start));
                for (String property : propertiesStr.split(ELEMENT_SEPARATOR)) {
                    var parts = property.split(KEY_VALUE_SEPARATOR, 2);
                    if (parts.length == 2)
                        properties.setString(parts[0], parts[1]);
                    else
                        LOG.error("Something went wrong parsing: '{}' -- incorrect gamedata!", blockStateStr);
                }
                blockState.setTag("Properties", properties);
            }
        } else {
            blockName = blockStateStr;
        }

        blockState.setString("Name", blockName);
        return blockState;
    }

    public static String structureToSnbt(NBTTagCompound compound) {
        return new SNBTPrinter().print(packStructureTemplate(compound));
    }

    private static NBTTagCompound packStructureTemplate(NBTTagCompound compound) {
        boolean hasPalettes = compound.hasKey("palettes", Constants.NBT.TAG_LIST);
        var palette = hasPalettes ? getListAt(compound.getTagList("palettes", Constants.NBT.TAG_LIST), 0)
                : compound.getTagList("palette", Constants.NBT.TAG_COMPOUND);

        var packedPalette = new NBTTagList();
        for (int i = 0; i < palette.tagCount(); i++) {
            packedPalette.appendTag(new NBTTagString(packBlockState(palette.getCompoundTagAt(i))));
        }
        compound.setTag("palette", packedPalette);

        if (hasPalettes) {
            var packedPalettes = new NBTTagList();
            var palettes = compound.getTagList("palettes", Constants.NBT.TAG_LIST);
            for (int i = 0; i < palettes.tagCount(); i++) {
                var paletteVariants = getListAt(palettes, i);
                var packedPaletteMap = new NBTTagCompound();

                for (int j = 0; j < paletteVariants.tagCount(); ++j) {
                    packedPaletteMap.setString(
                            packedPalette.getStringTagAt(j),
                            packBlockState(paletteVariants.getCompoundTagAt(j)));
                }

                packedPalettes.appendTag(packedPaletteMap);
            }
            compound.setTag("palettes", packedPalettes);
        }

        if (compound.hasKey("entities", Constants.NBT.TAG_LIST)) {
            var entities = compound.getTagList("entities", Constants.NBT.TAG_COMPOUND);
            var sortedEntities = new ArrayList<NBTTagCompound>();
            for (int i = 0; i < entities.tagCount(); i++) {
                sortedEntities.add(entities.getCompoundTagAt(i));
            }
            sortedEntities.sort(Comparator.comparing(
                    entity -> entity.getTagList("pos", Constants.NBT.TAG_DOUBLE),
                    YXZ_LISTTAG_DOUBLE_COMPARATOR));

            var sortedEntityTags = new NBTTagList();
            for (var entity : sortedEntities) {
                sortedEntityTags.appendTag(entity);
            }
            compound.setTag("entities", sortedEntityTags);
        }

        var blocks = compound.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
        var sortedBlocks = new ArrayList<NBTTagCompound>();
        for (int i = 0; i < blocks.tagCount(); i++) {
            sortedBlocks.add(blocks.getCompoundTagAt(i));
        }
        sortedBlocks.sort(Comparator.comparing(
                block -> block.getTagList("pos", Constants.NBT.TAG_INT),
                YXZ_LISTTAG_INT_COMPARATOR));

        var data = new NBTTagList();
        for (var block : sortedBlocks) {
            block.setString("state", packedPalette.getStringTagAt(block.getInteger("state")));
            data.appendTag(block);
        }
        compound.setTag("data", data);
        compound.removeTag("blocks");
        return compound;
    }

    private static NBTTagList getListAt(NBTTagList tag, int index) {
        var value = tag.get(index);
        return value.getId() == Constants.NBT.TAG_LIST ? (NBTTagList) value : new NBTTagList();
    }

    private static String packBlockState(NBTTagCompound tag) {
        var builder = new StringBuilder(tag.getString("Name"));
        if (tag.hasKey("Properties", Constants.NBT.TAG_COMPOUND)) {
            var properties = tag.getCompoundTag("Properties");
            var keys = new ArrayList<>(properties.getKeySet());
            Collections.sort(keys);
            builder.append(PROPERTIES_START);

            for (int i = 0; i < keys.size(); i++) {
                if (i != 0)
                    builder.append(ELEMENT_SEPARATOR);
                String key = keys.get(i);
                builder.append(key).append(KEY_VALUE_SEPARATOR).append(properties.getString(key));
            }

            builder.append(PROPERTIES_END);
        }

        return builder.toString();
    }
}
