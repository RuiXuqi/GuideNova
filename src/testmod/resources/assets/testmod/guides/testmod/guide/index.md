---
item_ids:
  - minecraft:carrot
---

# Start Page

[Japanese](./japanese.md)

[Markdown](./markdown.md)

<Recipe id="missingrecipe" fallbackText="The recipe for special item is disabled." />

Welcome to the world of <ItemImage id="minecraft:stone" />, <PlayerName />!

Keybinding Test: <KeyBind id="key.jump" />. Unbound key: <KeyBind id="key.spectatorOutlines" />.

You may ~~need~~ a <Color color="#ff0000">door</Color> <Color id="test_color">door</Color>!

<CommandLink command="/tp @s 0 90 0" title="Tooltip" close={true}>Teleport!</CommandLink>

<ItemLink id="minecraft:stick" tag="{display:{Name:&quot;Custom name&quot;}}" />

<GameScene zoom={4} interactive={true}>
    <Entity id="minecraft:sheep" data="{Color: 2}" />
    <Block id="minecraft:water" />
</GameScene>

<GameScene zoom={4}>
    <ImportStructure src="redstone_test.nbt" />
    <RemoveBlocks id="minecraft:stone" />
    <ImportStructure src="redstone_test.nbt" pos="1 0 1" />
</GameScene>

<RecipeFor id="minecraft:wooden_door" />
<RecipeFor id="minecraft:iron_ingot" type="minecraft:smelting" />

<GameScene zoom={2}>
  <ImportStructure src="test.nbt" />

  <BlockAnnotationTemplate id="minecraft:log" p:variant="spruce" p:axis="x">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      This will be shown in the tooltip! <ItemImage id="minecraft:stone" />
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>

<GameScene zoom="8">
  <ImportStructure src="end_portal.nbt" />
</GameScene>

## Recipes

<Row>
    <RecipeFor id="minecraft:planks" meta="0" />
    <RecipeFor id="minecraft:bed" meta="14" />
    <RecipeFor id="minecraft:stick" />
    <RecipesFor id="minecraft:bed" meta="13" />
</Row>

***

<Row>
  <BlockImage id="minecraft:log" p:variant="oak" scale="4" />
  <BlockImage id="minecraft:log" p:variant="spruce" scale="4" />
  <BlockImage id="minecraft:log2" p:variant="acacia" scale="4" />
  <BlockImage id="minecraft:log" p:variant="birch" scale="4" />
  <BlockImage id="minecraft:log" p:variant="jungle" scale="4" />
  <BlockImage id="minecraft:log2" p:variant="dark_oak" scale="4" />
</Row>
