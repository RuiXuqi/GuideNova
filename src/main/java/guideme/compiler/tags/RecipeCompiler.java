package guideme.compiler.tags;

import guideme.compiler.PageCompiler;
import guideme.document.block.LytBlock;
import guideme.document.block.LytBlockContainer;
import guideme.document.block.LytParagraph;
import guideme.internal.GuideMEClient;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.libs.mdast.model.MdAstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows a Recipe-Book-Like representation of the recipe needed to craft a given item.
 */
public class RecipeCompiler extends BlockTagCompiler {
    private static final Logger LOG = LoggerFactory.getLogger(RecipeCompiler.class);

    @Nullable
    private List<RecipeDisplayMapping<?>> sharedMappings;

    @Override
    public Set<String> getTagNames() {
        return Set.of("Recipe", "RecipeFor", "RecipesFor");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var fallbackText = el.getAttributeString("fallbackText", null);
        var mappings = getMappings(compiler, parent, el);
        if (mappings == null) {
            return;
        }

        if ("RecipesFor".equals(el.name())) {
            var itemAndId = MdxAttrs.getRequiredItemStackAndId(compiler, parent, el, OreDictionary.WILDCARD_VALUE);
            if (itemAndId == null) {
                return;
            }

            boolean anyAdded = false;
            var target = itemAndId.getRight();
            for (var mapping : mappings) {
                for (var block : createAllForOutput(mapping, target)) {
                    block.setSourceNode((MdAstNode) el);
                    parent.append(block);
                    anyAdded = true;
                }
            }

            if (!anyAdded && fallbackText != null && !fallbackText.isEmpty()) {
                parent.append(LytParagraph.of(fallbackText));
            }
        } else if ("RecipeFor".equals(el.name())) {
            var itemAndId = MdxAttrs.getRequiredItemStackAndId(compiler, parent, el, OreDictionary.WILDCARD_VALUE);
            if (itemAndId == null) {
                return;
            }

            var id = itemAndId.getLeft();
            var target = itemAndId.getRight();

            for (var mapping : mappings) {
                var block = mapping.createFirstForOutput(target);
                if (block != null) {
                    block.setSourceNode((MdAstNode) el);
                    parent.append(block);
                    return;
                }
            }

            if (fallbackText == null) {
                if (!GuideMEClient.isHideMissingRecipeErrors()) {
                    parent.appendError(compiler, "Couldn't find recipe for " + id, el);
                }
            } else if (!fallbackText.isEmpty()) {
                parent.append(LytParagraph.of(fallbackText));
            }
        } else {
            var recipeId = MdxAttrs.getRequiredId(compiler, parent, el, "id");
            if (recipeId == null) {
                return;
            }

            boolean foundType = false;
            for (var mapping : mappings) {
                foundType = true;
                var block = mapping.createById(recipeId);
                if (block != null) {
                    block.setSourceNode((MdAstNode) el);
                    parent.append(block);
                    return;
                }
            }

            if (fallbackText == null) {
                if (!GuideMEClient.isHideMissingRecipeErrors()) {
                    var message = foundType
                            ? "Couldn't find recipe " + recipeId
                            : "Couldn't find recipe type " + MdxAttrs.getString(compiler, parent, el, "type", null);
                    parent.appendError(compiler, message, el);
                }
            } else if (!fallbackText.isEmpty()) {
                parent.append(LytParagraph.of(fallbackText));
            }
        }
    }

    @Nullable
    private List<RecipeDisplayMapping<?>> getMappings(PageCompiler compiler, LytBlockContainer parent,
            MdxJsxElementFields el) {
        List<RecipeDisplayMapping<?>> result = new ArrayList<>();
        var mappings = new RecipeTypeMappingSupplier.RecipeTypeMappings() {
            @Override
            public <T> void add(RecipeDisplayMapping<T> mapping) {
                Objects.requireNonNull(mapping, "mapping");
                result.add(mapping);
            }
        };
        for (var extension : compiler.getExtensions(RecipeTypeMappingSupplier.EXTENSION_POINT)) {
            extension.collect(mappings);
        }

        result.addAll(getSharedMappings());

        var requestedType = MdxAttrs.getString(compiler, parent, el, "type", null);
        if (requestedType != null) {
            ResourceLocation resolvedType;
            try {
                resolvedType = compiler.resolveId(requestedType.trim());
            } catch (Exception e) {
                parent.appendError(compiler, "Malformed recipe type " + requestedType + ": " + e.getMessage(), el);
                return null;
            }
            result.removeIf(mapping -> !resolvedType.equals(mapping.id()));
        }

        return result;
    }

    private List<? extends RecipeDisplayMapping<?>> getSharedMappings() {
        if (sharedMappings != null) {
            return sharedMappings;
        }

        Set<ResourceLocation> recipeTypes = new HashSet<>();
        List<RecipeDisplayMapping<?>> result = new ArrayList<>();
        var mappings = new RecipeTypeMappingSupplier.RecipeTypeMappings() {
            @Override
            public <T> void add(RecipeDisplayMapping<T> mapping) {
                Objects.requireNonNull(mapping, "mapping");

                recipeTypes.add(mapping.id());
                result.add(mapping);
            }
        };

        var it = ServiceLoader.load(RecipeTypeMappingSupplier.class).stream().iterator();
        while (it.hasNext()) {
            var provider = it.next();
            try {
                provider.get().collect(mappings);
            } catch (Exception e) {
                LOG.error("Failed to collect shared recipe type mappings from {}", provider.type(), e);
            }
        }

        var recipeTypesSorted = new ArrayList<>(recipeTypes);
        Collections.sort(recipeTypesSorted);
        LOG.info("Discovered shared recipe type mappings: {}", recipeTypesSorted);

        return sharedMappings = List.copyOf(result);
    }

    private static <T> List<LytBlock> createAllForOutput(RecipeDisplayMapping<T> mapping, ItemStack target) {
        var result = new ArrayList<LytBlock>();
        for (T recipe : mapping.recipes()) {
            if (mapping.outputMatches(recipe, target)) {
                result.add(mapping.create(recipe));
            }
        }
        return result;
    }
}
