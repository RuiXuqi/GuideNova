package guideme.compiler;

import guideme.libs.mdast.model.MdAstRoot;
import java.util.Objects;
import net.minecraft.util.ResourceLocation;

public class ParsedGuidePage {
    final String sourcePack;
    final ResourceLocation id;
    final String source;
    final MdAstRoot astRoot;
    final Frontmatter frontmatter;
    final String language;

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
            Frontmatter frontmatter, String language) {
        this.sourcePack = sourcePack;
        this.id = id;
        this.source = source;
        this.astRoot = astRoot;
        this.frontmatter = frontmatter;
        this.language = Objects.requireNonNull(language, "language");
    }

    public String getSourcePack() {
        return sourcePack;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Frontmatter getFrontmatter() {
        return frontmatter;
    }

    public MdAstRoot getAstRoot() {
        return astRoot;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        if (id.getNamespace().equals(sourcePack)) {
            return id.toString();
        } else {
            return id + " (from " + sourcePack + ")";
        }
    }
}
