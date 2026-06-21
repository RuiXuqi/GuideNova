package guideme.scene.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.jetbrains.annotations.Nullable;

class GuidebookChunkSource implements IChunkProvider {
    private final GuidebookLevel level;

    private final Long2ObjectMap<GuidebookChunk> chunks = new Long2ObjectOpenHashMap<>();

    public GuidebookChunkSource(GuidebookLevel level) {
        this.level = level;
    }

    @Nullable
    @Override
    public Chunk getLoadedChunk(int x, int z) {
        return chunks.get(ChunkPos.asLong(x, z));
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new GuidebookChunk(level, x, z));
    }

    @Override
    public boolean tick() {
        return false;
    }

    @Override
    public String makeString() {
        return "GuidebookChunkSource: " + chunks.size();
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return chunks.containsKey(ChunkPos.asLong(x, z));
    }
}
