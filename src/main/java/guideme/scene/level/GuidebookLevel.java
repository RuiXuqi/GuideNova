package guideme.scene.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Timer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveDataMemoryStorage;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;

public class GuidebookLevel extends World {
    private final LongSet filledBlocks = new LongOpenHashSet();

    private final Timer tracker = new Timer(20.0F);
    private float partialTick;

    public GuidebookLevel() {
        super(new SaveHandlerMP(), createLevelData(), new WorldProviderSurface(), new Profiler(), true);
        this.provider.setWorld(this);
        this.chunkProvider = createChunkProvider();
        this.mapStorage = new SaveDataMemoryStorage();
        this.initCapabilities();
    }

    public Bounds getBounds() {
        if (filledBlocks.isEmpty()) {
            return new Bounds(BlockPos.ORIGIN, BlockPos.ORIGIN);
        }

        var min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        var max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        var cur = new BlockPos.MutableBlockPos();

        filledBlocks.forEach(packedPos -> {
            cur.setPos(BlockPos.fromLong(packedPos));
            min.setPos(
                    Math.min(min.getX(), cur.getX()),
                    Math.min(min.getY(), cur.getY()),
                    Math.min(min.getZ(), cur.getZ()));

            max.setPos(
                    Math.max(max.getX(), cur.getX() + 1),
                    Math.max(max.getY(), cur.getY() + 1),
                    Math.max(max.getZ(), cur.getZ() + 1));
        });

        for (var entity : getEntitiesForRendering()) {
            var bounds = entity.getEntityBoundingBox();

            min.setPos(
                    Math.min(min.getX(), (int) bounds.minX),
                    Math.min(min.getY(), (int) bounds.minY),
                    Math.min(min.getZ(), (int) bounds.minZ));

            max.setPos(
                    Math.max(max.getX(), (int) Math.ceil(bounds.maxX)),
                    Math.max(max.getY(), (int) Math.ceil(bounds.maxY)),
                    Math.max(max.getZ(), (int) Math.ceil(bounds.maxZ)));
        }

        return new Bounds(min, max);
    }

    public boolean isFilledBlock(BlockPos blockPos) {
        return filledBlocks.contains(blockPos.toLong());
    }

    void removeFilledBlock(BlockPos pos) {
        filledBlocks.remove(pos.toLong());
    }

    void addFilledBlock(BlockPos pos) {
        filledBlocks.add(pos.toLong());
    }

    public record Bounds(BlockPos min, BlockPos max) {
    }

    private static WorldInfo createLevelData() {
        var levelData = new WorldInfo(new WorldSettings(0, GameType.CREATIVE,
                false, false, WorldType.DEFAULT),
                "Guidebook");
        levelData.setDifficulty(EnumDifficulty.PEACEFUL);

        // set time of day to noon (from TimeCommand noon)
        levelData.setWorldTime(6000);

        return levelData;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public void onRenderFrame() {
        tracker.updateTimer();
        var ticksElapsed = tracker.elapsedTicks;
        if (ticksElapsed > 0) {
            worldInfo.setWorldTotalTime(worldInfo.getWorldTotalTime() + ticksElapsed);
        }

        partialTick = tracker.renderPartialTicks;
    }

    public boolean hasFilledBlocks() {
        return !filledBlocks.isEmpty();
    }

    public Stream<BlockPos> getFilledBlocks() {
        return filledBlocks.longStream().sequential().mapToObj(BlockPos::fromLong);
    }

    /**
     * @return All block entities in the level.
     */
    public Set<TileEntity> getBlockEntities() {
        return getFilledBlocks()
                .map(this::getTileEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    public Iterable<Entity> getEntitiesForRendering() {
        return loadedEntityList;
    }

    public void addEntity(Entity entity) {
        this.removeEntity(entity.getEntityId());
        loadedEntityList.add(entity);
        entitiesById.addKey(entity.getEntityId(), entity);
        getChunk(entity.getPosition()).addEntity(entity);
        onEntityAdded(entity);
    }

    public void removeEntity(int entityId) {
        var entity = entitiesById.removeObject(entityId);
        if (entity != null) {
            if (entity.isBeingRidden()) {
                entity.removePassengers();
            }
            if (entity.isRiding()) {
                entity.dismountRidingEntity();
            }
            if (entity.addedToChunk && isChunkLoaded(entity.chunkCoordX, entity.chunkCoordZ, true)) {
                getChunk(entity.chunkCoordX, entity.chunkCoordZ).removeEntity(entity);
            }
            loadedEntityList.remove(entity);
            entity.setDead();
            onEntityRemoved(entity);
        }
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new GuidebookChunkSource(this);
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return allowEmpty || !getChunkProvider().provideChunk(x, z).isEmpty();
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 0xF000F0;
    }

    @Override
    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
        return 15;
    }

    @Override
    public float getLightBrightness(BlockPos pos) {
        return 1.0f;
    }
}
