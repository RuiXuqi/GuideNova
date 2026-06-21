package guideme.internal.extensions;

import guideme.compiler.tags.RecipeDisplayMapping;
import guideme.compiler.tags.RecipeOutputMatcher;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import guideme.internal.GuidebookText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import org.jetbrains.annotations.Nullable;

final class VanillaRecipes {
    private static final ResourceLocation CRAFTING_ID = new ResourceLocation("minecraft", "crafting");
    private static final ResourceLocation SMELTING_ID = new ResourceLocation("minecraft", "smelting");

    private VanillaRecipes() {
    }

    static RecipeDisplayMapping<IRecipe> crafting() {
        return new RecipeDisplayMapping<>() {
            @Override
            public ResourceLocation id() {
                return CRAFTING_ID;
            }

            @Override
            public Iterable<? extends IRecipe> recipes() {
                return CraftingManager.REGISTRY;
            }

            @Override
            public @Nullable ResourceLocation getRecipeId(IRecipe recipe) {
                return recipe.getRegistryName();
            }

            @Override
            public boolean outputMatches(IRecipe recipe, ItemStack target) {
                return RecipeOutputMatcher.matches(recipe.getRecipeOutput(), target);
            }

            @Override
            public LytStandardRecipeBox<IRecipe> create(IRecipe recipe) {
                return createCrafting(recipe);
            }
        };
    }

    static RecipeDisplayMapping<SmeltingDisplay> smelting() {
        return new RecipeDisplayMapping<>() {
            @Override
            public ResourceLocation id() {
                return SMELTING_ID;
            }

            @Override
            public Iterable<? extends SmeltingDisplay> recipes() {
                return getSmeltingRecipes();
            }

            @Override
            public @Nullable ResourceLocation getRecipeId(SmeltingDisplay recipe) {
                return null;
            }

            @Override
            public boolean outputMatches(SmeltingDisplay recipe, ItemStack target) {
                return RecipeOutputMatcher.matches(recipe.output(), target);
            }

            @Override
            public LytStandardRecipeBox<SmeltingDisplay> create(SmeltingDisplay recipe) {
                return createSmelting(recipe);
            }
        };
    }

    public static LytStandardRecipeBox<IRecipe> createCrafting(IRecipe recipe) {
        LytSlotGrid grid;
        var ingredients = recipe.getIngredients();
        if (recipe instanceof IShapedRecipe shapedRecipe) {
            var recipeWidth = Math.max(1, shapedRecipe.getRecipeWidth());
            var recipeHeight = Math.max(1, shapedRecipe.getRecipeHeight());
            grid = new LytSlotGrid(recipeWidth, recipeHeight);

            for (var x = 0; x < recipeWidth; x++) {
                for (var y = 0; y < recipeHeight; y++) {
                    var index = y * recipeWidth + x;
                    if (index < ingredients.size()) {
                        var ingredient = ingredients.get(index);
                        if (!isIngredientEmpty(ingredient)) {
                            grid.setIngredient(x, y, ingredient);
                        }
                    }
                }
            }
        } else {
            var visibleIngredients = ingredients.stream()
                    .filter(ingredient -> !isIngredientEmpty(ingredient))
                    .toList();
            var ingredientCount = visibleIngredients.size();
            grid = new LytSlotGrid(
                    Math.max(1, Math.min(3, ingredientCount)),
                    Math.max(1, (ingredientCount + 2) / 3));
            for (int i = 0; i < visibleIngredients.size(); i++) {
                var col = i % 3;
                var row = i / 3;
                grid.setIngredient(col, row, visibleIngredients.get(i));
            }
        }

        var title = (recipe instanceof IShapedRecipe)
                ? GuidebookText.Crafting.str()
                : GuidebookText.ShapelessCrafting.str();

        return LytStandardRecipeBox.builder()
                .title(title)
                .icon(Blocks.CRAFTING_TABLE)
                .input(grid)
                .outputFromResultOf(recipe)
                .build(recipe);
    }

    public static LytStandardRecipeBox<SmeltingDisplay> createSmelting(SmeltingDisplay recipe) {
        return LytStandardRecipeBox.builder()
                .title(GuidebookText.Smelting.str())
                .icon(Blocks.FURNACE)
                .input(recipe.input())
                .output(recipe.output())
                .build(recipe);
    }

    private static List<SmeltingDisplay> getSmeltingRecipes() {
        var result = new ArrayList<SmeltingDisplay>();
        for (var entry : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            var input = entry.getKey();
            var output = entry.getValue();
            if (!input.isEmpty() && !output.isEmpty()) {
                result.add(new SmeltingDisplay(Ingredient.fromStacks(input.copy()), output.copy()));
            }
        }
        result.sort(Comparator
                .comparing((SmeltingDisplay recipe) -> stackSortKey(recipe.output()))
                .thenComparing(recipe -> stackSortKey(recipe.firstInput())));
        return result;
    }

    private static String stackSortKey(ItemStack stack) {
        var registryName = stack.getItem().getRegistryName();
        return (registryName != null ? registryName.toString() : stack.getItem().getClass().getName())
                + "#" + stack.getMetadata();
    }

    private static boolean isIngredientEmpty(Ingredient ingredient) {
        for (var stack : ingredient.getMatchingStacks()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public record SmeltingDisplay(Ingredient input, ItemStack output) {
        ItemStack firstInput() {
            var stacks = input.getMatchingStacks();
            return stacks.length == 0 ? ItemStack.EMPTY : stacks[0];
        }
    }
}
