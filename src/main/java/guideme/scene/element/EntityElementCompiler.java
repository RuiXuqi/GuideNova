package guideme.scene.element;

import guideme.compiler.PageCompiler;
import guideme.compiler.tags.MdxAttrs;
import guideme.document.LytErrorSink;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.scene.GuidebookScene;
import java.util.Set;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityElementCompiler implements SceneElementTagCompiler {
    private static final Logger LOG = LoggerFactory.getLogger(EntityElementCompiler.class);

    @Override
    public Set<String> getTagNames() {
        return Set.of("Entity");
    }

    @Override
    public void compile(GuidebookScene scene,
            PageCompiler compiler,
            LytErrorSink errorSink,
            MdxJsxElementFields el) {
        var entityId = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (entityId == null) {
            errorSink.appendError(compiler, "Missing attribute 'id'", el);
            return;
        }

        var data = MdxAttrs.getCompoundTag(compiler, errorSink, el, "data", new NBTTagCompound());
        data.setString("id", entityId);

        var entity = EntityList.createEntityFromNBT(data, scene.getLevel());
        if (entity == null) {
            errorSink.appendError(compiler, "Failed to load entity '" + entityId, el);
            return;
        }

        var pos = new Vector3f(0.5f, 0, 0.5f);
        MdxAttrs.getFloatPos(compiler, errorSink, el, pos);
        entity.setPosition(pos.x, pos.y, pos.z);

        var rotationY = MdxAttrs.getFloat(compiler, errorSink, el, "rotationY", -90);
        var rotationX = MdxAttrs.getFloat(compiler, errorSink, el, "rotationX", 0);
        entity.rotationYaw = rotationY;
        entity.rotationPitch = rotationX;
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.lastTickPosX = entity.posX;
        entity.lastTickPosY = entity.posY;
        entity.lastTickPosZ = entity.posZ;
        entity.setRotationYawHead(entity.rotationYaw);
        entity.setRenderYawOffset(entity.rotationYaw);

        scene.getLevel().addEntity(entity);
        entity.onUpdate();
    }
}
