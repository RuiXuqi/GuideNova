package guideme.scene.level;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

class GuidebookChunk extends Chunk {
    public GuidebookChunk(GuidebookLevel level, int chunkX, int chunkZ) {
        super(level, chunkX, chunkZ);
    }

    private GuidebookLevel getGuidebookLevel() {
        return (GuidebookLevel) getWorld();
    }

    @Nullable
    @Override
    public IBlockState setBlockState(BlockPos pos, IBlockState state) {
        var result = super.setBlockState(pos, state);
        if (state.getBlock().isAir(state, getGuidebookLevel(), BlockPos.ORIGIN)) {
            getGuidebookLevel().removeFilledBlock(pos);
        } else {
            getGuidebookLevel().addFilledBlock(pos);
        }
        return result;
    }

}
