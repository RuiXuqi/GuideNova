package guideme.scene.element;

import guideme.compiler.IdUtils;
import guideme.compiler.PageCompiler;
import guideme.compiler.tags.MdxAttrs;
import guideme.document.LytErrorSink;
import guideme.internal.util.SNBTUtil;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.scene.GuidebookScene;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

/**
 * Imports a structure into the scene.
 */
public class ImportStructureElementCompiler implements SceneElementTagCompiler {
    @Override
    public Set<String> getTagNames() {
        return Set.of("ImportStructure");
    }

    @Override
    public void compile(GuidebookScene scene,
            PageCompiler compiler,
            LytErrorSink errorSink,
            MdxJsxElementFields el) {
        var structureSrc = el.getAttributeString("src", null);
        if (structureSrc == null) {
            errorSink.appendError(compiler, "Missing src attribute", el);
            return;
        }

        var pos = MdxAttrs.getBlockPos(compiler, errorSink, el, "pos", BlockPos.ORIGIN);

        ResourceLocation absStructureSrc;
        try {
            absStructureSrc = IdUtils.resolveLink(structureSrc, compiler.getPageId());
        } catch (IdUtils.ResourceLocationException e) {
            errorSink.appendError(compiler, "Invalid structure path: " + structureSrc, el);
            return;
        }

        var structureNbtData = compiler.loadAsset(absStructureSrc);
        if (structureNbtData == null) {
            errorSink.appendError(compiler, "Missing structure file", el);
            return;
        }

        NBTTagCompound compoundTag;
        try {
            if (absStructureSrc.getPath().toLowerCase(Locale.ROOT).endsWith(".snbt")) {
                compoundTag = SNBTUtil.snbtToStructure(new String(structureNbtData, StandardCharsets.UTF_8));
            } else {
                compoundTag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(structureNbtData));
            }
        } catch (Exception e) {
            errorSink.appendError(compiler, "Couldn't read structure: " + e.getMessage(), el);
            return;
        }

        var template = new Template();
        template.read(compoundTag);
        var settings = new PlacementSettings();
        settings.setIgnoreEntities(true); // Entities need a server level in structures

        template.addBlocksToWorld(scene.getLevel(), pos, settings, 0);
    }
}
