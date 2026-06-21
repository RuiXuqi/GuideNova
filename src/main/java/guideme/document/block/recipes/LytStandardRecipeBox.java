package guideme.document.block.recipes;

import guideme.document.DefaultStyles;
import guideme.document.LytSize;
import guideme.document.block.AlignItems;
import guideme.document.block.LytBlock;
import guideme.document.block.LytGuiSprite;
import guideme.document.block.LytHBox;
import guideme.document.block.LytParagraph;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.LytVBox;
import guideme.render.GuiAssets;
import guideme.render.RenderContext;
import guideme.scene.LytItemImage;
import guideme.siteexport.ExportableResourceProvider;
import guideme.siteexport.ResourceExporter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Provides an easy way to define recipes that are rendered as follows:
 * <ul>
 * <li>Title bar with optional icon.</li>
 * <li>A grid of slots on the left side, representing the recipe inputs.</li>
 * <li>A grid of slots on the right side, representing the recipe outputs.</li>
 * <li>A big arrow pointing left to right between the two grids.</li>
 * </ul>
 * <p/>
 * Use the {@link #builder()} method to get started.
 */
public class LytStandardRecipeBox<T> extends LytVBox implements ExportableResourceProvider {
    private final T holder;

    @ApiStatus.Internal
    LytStandardRecipeBox(T holder) {
        this.holder = holder;
    }

    public T getRecipe() {
        return holder;
    }

    @Override
    public void render(RenderContext context) {
        context.renderPanel(getBounds());
        super.render(context);
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        if (this.holder instanceof IRecipe recipe) {
            exporter.referenceRecipe(recipe);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LytBlock icon;
        private final LytParagraph title = new LytParagraph();
        private final List<LytBlock> topDecoration = new ArrayList<>();
        private final List<LytBlock> bottomDecoration = new ArrayList<>();
        private final List<LytBlock> leftDecoration = new ArrayList<>();
        private final List<LytBlock> rightDecoration = new ArrayList<>();
        @Nullable
        private LytSlotGrid input;
        @Nullable
        private LytSlotGrid output;
        @Nullable
        private LytBlock customBody;

        private Builder() {
            this.title.setStyle(DefaultStyles.CRAFTING_RECIPE_TYPE);
        }

        public <T> LytStandardRecipeBox<T> build(T recipe) {
            var box = new LytStandardRecipeBox<>(recipe);
            build(box);
            return box;
        }

        public Builder icon(LytBlock block) {
            this.icon = block;
            return this;
        }

        public Builder icon(Block block) {
            return icon(new ItemStack(block));
        }

        public Builder icon(Item item) {
            return icon(item.getDefaultInstance());
        }

        public Builder icon(ItemStack workbench) {
            var itemImage = new LytItemImage();
            itemImage.setScale(0.5f);
            itemImage.setItem(workbench);
            this.icon = itemImage;
            return this;
        }

        public Builder title(String title) {
            this.title.appendText(title);
            return this;
        }

        public Builder input(Ingredient ingredient) {
            this.input = LytSlotGrid.rowFromIngredients(List.of(ingredient), false);
            return this;
        }

        public Builder input(LytSlotGrid grid) {
            this.input = grid;
            return this;
        }

        public Builder output(LytSlotGrid grid) {
            this.output = grid;
            return this;
        }

        public Builder output(ItemStack resultItem) {
            this.output = new LytSlotGrid(1, 1);
            this.output.setItem(0, 0, resultItem);
            return this;
        }

        public Builder outputFromResultOf(IRecipe recipe) {
            var resultItem = recipe.getRecipeOutput();
            if (!resultItem.isEmpty()) {
                output(resultItem);
            }
            return this;
        }

        /**
         * Insert a block to be placed vertically before the main recipe body.
         */
        public Builder addTop(LytBlock block) {
            this.topDecoration.add(block);
            return this;
        }

        /**
         * Insert a block to be placed vertically before the main recipe body.
         */
        public Builder addLeft(LytBlock block) {
            this.leftDecoration.add(block);
            return this;
        }

        /**
         * Insert a block to be placed vertically after the main recipe body.
         */
        public Builder addBottom(LytBlock block) {
            this.bottomDecoration.add(block);
            return this;
        }

        /**
         * Insert a block to be placed vertically before the main recipe body.
         */
        public Builder addRight(LytBlock block) {
            this.rightDecoration.add(block);
            return this;
        }

        /**
         * Use a completely custom recipe body. Using this is mutually exclusive with {@link #addLeft} and
         * {@link #addRight}, as well as any of the input/output setters.
         */
        public Builder customBody(@Nullable LytBlock customBody) {
            this.customBody = customBody;
            return this;
        }

        @ApiStatus.Internal
        void build(LytStandardRecipeBox<?> box) {
            if (this.customBody != null) {
                if (!this.leftDecoration.isEmpty()) {
                    throw new IllegalStateException("Cannot combine a custom recipe body with left decorations");
                }
                if (!this.rightDecoration.isEmpty()) {
                    throw new IllegalStateException("Cannot combine a custom recipe body with right decorations");
                }
                if (this.input != null) {
                    throw new IllegalStateException("Cannot set recipe inputs when a custom body is used.");
                }
                if (this.output != null) {
                    throw new IllegalStateException("Cannot set recipe outputs when a custom body is used.");
                }
            }

            box.setGap(2);
            box.setPadding(5);

            var titleRow = new LytHBox();
            titleRow.setAlignItems(AlignItems.CENTER);
            titleRow.setWrap(false);
            if (icon != null) {
                titleRow.append(icon);
            }
            if (!title.isEmpty()) {
                titleRow.append(title);
            }
            titleRow.setGap(2);

            box.append(titleRow);

            for (var block : topDecoration) {
                box.append(block);
            }

            if (this.customBody != null) {
                box.append(this.customBody);
            } else if (input != null || output != null || !leftDecoration.isEmpty() || !rightDecoration.isEmpty()) {
                var gridRow = new LytHBox();
                gridRow.setGap(2);
                gridRow.setAlignItems(AlignItems.CENTER);
                gridRow.setWrap(false);

                for (var block : leftDecoration) {
                    gridRow.append(block);
                }
                if (input != null) {
                    gridRow.append(input);
                }
                if (input != null || output != null) {
                    gridRow.append(new LytGuiSprite(GuiAssets.ARROW, new LytSize(24, 17)));
                }
                if (output != null) {
                    gridRow.append(output);
                }
                for (var block : rightDecoration) {
                    gridRow.append(block);
                }

                box.append(gridRow);
            }

            for (var block : bottomDecoration) {
                box.append(block);
            }
        }
    }
}
