package guideme.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import org.junit.jupiter.api.Test;

class SNBTUtilTest {
    @Test
    void packsStructurePaletteAndData() throws NBTException {
        var tag = JsonToNBT.getTagFromJson("""
                {
                  blocks: [
                    {pos: [1, 0, 0], state: 1},
                    {pos: [0, 0, 0], state: 0}
                  ],
                  palette: [
                    {Name: "minecraft:stone"},
                    {Name: "minecraft:oak_stairs", Properties: {half: "bottom", facing: "north"}}
                  ],
                  entities: [],
                  size: [2, 1, 1],
                  author: "test"
                }
                """);

        assertEquals("""
                {
                    author: "test",
                    size: [2, 1, 1],
                    data: [
                        {pos: [0, 0, 0], state: "minecraft:stone"},
                        {pos: [1, 0, 0], state: "minecraft:oak_stairs{facing:north,half:bottom}"}
                    ],
                    entities: [],
                    palette: [
                        "minecraft:stone",
                        "minecraft:oak_stairs{facing:north,half:bottom}"
                    ]
                }""", SNBTUtil.structureToSnbt(tag));
    }

    @Test
    void unpacksPackedStructurePaletteAndData() throws NBTException {
        var tag = SNBTUtil.snbtToStructure("""
                {
                  palette: [
                    "minecraft:stone",
                    "minecraft:oak_stairs{facing:north,half:bottom}"
                  ],
                  data: [
                    {pos: [0, 0, 0], state: "minecraft:stone"},
                    {pos: [1, 0, 0], state: "minecraft:oak_stairs{facing:north,half:bottom}"}
                  ],
                  entities: [],
                  size: [2, 1, 1],
                  author: "test"
                }
                """);

        assertFalse(tag.hasKey("data"));

        var palette = tag.getTagList("palette", 10);
        assertEquals(2, palette.tagCount());
        assertEquals("minecraft:stone", palette.getCompoundTagAt(0).getString("Name"));

        var stair = palette.getCompoundTagAt(1);
        assertEquals("minecraft:oak_stairs", stair.getString("Name"));
        assertEquals("north", stair.getCompoundTag("Properties").getString("facing"));
        assertEquals("bottom", stair.getCompoundTag("Properties").getString("half"));

        var blocks = tag.getTagList("blocks", 10);
        assertEquals(2, blocks.tagCount());
        assertEquals(0, blocks.getCompoundTagAt(0).getInteger("state"));
        assertEquals(1, blocks.getCompoundTagAt(1).getInteger("state"));
    }
}
