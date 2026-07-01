package io.github.acaciatide.blockexcavatorstapi.shape;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.util.math.StationBlockPos;

import java.util.HashSet;
import java.util.Set;

public class ShapelessShape extends AbstractMiningShape {

    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        int maxBlocksVal = Math.max(1, Math.min(256, ConfigInit.GENERAL.maxBlocks));
        
        // 最終的に返すBlockPosのセット
        Set<BlockPos> blocks = new HashSet<>(maxBlocksVal * 2);
        
        // オブジェクトのボクシングを防ぐため、fastutil のプリミティブコレクションを使用する
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue(maxBlocksVal * 4);
        LongSet visited = new LongOpenHashSet(maxBlocksVal * 4);

        long startPacked = StationBlockPos.asLong(startX, startY, startZ);
        visited.add(startPacked);
        
        // 開始ブロックを結果に追加する
        blocks.add(new BlockPos(startX, startY, startZ));
        
        addNeighbors(queue, visited, startX, startY, startZ);

        while (!queue.isEmpty() && blocks.size() < maxBlocksVal) {
            long packed = queue.dequeueLong();
            int px = StationBlockPos.unpackLongX(packed);
            int py = StationBlockPos.unpackLongY(packed);
            int pz = StationBlockPos.unpackLongZ(packed);

            int currentId = world.getBlockId(px, py, pz);
            int currentMeta = world.getBlockMeta(px, py, pz);

            if (currentId == startBlock.id && (ConfigInit.ADVANCED.ignoreMetadataInShapeless || currentMeta == startMeta)) {
                // ツール適正チェック
                if (!canHarvest(player, currentId)) {
                    continue;
                }
                
                // 一括破壊対象ブロックとして確定した時点でのみ、BlockPosを生成して追加する
                blocks.add(new BlockPos(px, py, pz));
                addNeighbors(queue, visited, px, py, pz);
            }
        }
        return blocks;
    }

    // 周囲26近傍を探索し、未訪問の座標をキューと訪問済みに登録する
    private void addNeighbors(LongArrayFIFOQueue queue, LongSet visited, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;
                    long packed = StationBlockPos.asLong(nx, ny, nz);
                    // visitedへの追加に成功した場合（未訪問の場合）のみキューに登録する
                    if (visited.add(packed)) {
                        queue.enqueue(packed);
                    }
                }
            }
        }
    }
}
