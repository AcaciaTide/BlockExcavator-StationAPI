package io.github.acaciatide.blockexcavatorstapi.shape;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ShapelessShape extends AbstractMiningShape {

    /**
     * ブロックの3次元座標（x, y, z）を1つの 64ビット long 値にパックする。
     * ビット割り当て：
     * - X 座標: 26 ビット (ビット 38 〜 63) -> 表現範囲: -33,554,432 〜 33,554,431
     * - Y 座標: 12 ビット (ビット 26 〜 37) -> 表現範囲: -2,048 〜 2,047
     * - Z 座標: 26 ビット (ビット 0 〜 25)  -> 表現範囲: -33,554,432 〜 33,554,431
     * 
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @return パックされた long 値
     */
    private static long encodePos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) |
               ((long) (y & 0xFFF) << 26) |
               (z & 0x3FFFFFF);
    }

    /**
     * パックされた long 値から X 座標を抽出・復元する。
     * 26ビットの符号ビット（ビット 25）を検出し、負の座標の場合は上位ビットを 1 で埋める符号拡張を行う。
     * 
     * @param packed パックされた座標値
     * @return 復元された X 座標
     */
    private static int getX(long packed) {
        int x = (int) ((packed >> 38) & 0x3FFFFFF);
        if ((x & 0x2000000) != 0) x |= 0xFC000000;
        return x;
    }

    /**
     * パックされた long 値から Y 座標を抽出・復元する。
     * 12ビットの符号ビット（ビット 11）を検出し、負の座標の場合は上位ビットを 1 で埋める符号拡張を行う。
     * 
     * @param packed パックされた座標値
     * @return 復元された Y 座標
     */
    private static int getY(long packed) {
        int y = (int) ((packed >> 26) & 0xFFF);
        if ((y & 0x800) != 0) y |= 0xFFFFF000;
        return y;
    }

    /**
     * パックされた long 値から Z 座標を抽出・復元する。
     * 26ビットの符号ビット（ビット 25）を検出し、負の座標の場合は上位ビットを 1 で埋める符号拡張を行う。
     * 
     * @param packed パックされた座標値
     * @return 復元された Z 座標
     */
    private static int getZ(long packed) {
        int z = (int) (packed & 0x3FFFFFF);
        if ((z & 0x2000000) != 0) z |= 0xFC000000;
        return z;
    }

    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        int maxBlocksVal = Math.max(1, Math.min(256, ConfigInit.GENERAL.maxBlocks));
        
        // 最終的に返すBlockPosのセット
        Set<BlockPos> blocks = new HashSet<>(maxBlocksVal * 2);
        
        // オブジェクトの新規生成を防ぐため、ArrayDequeとlong値で探索を管理する
        Queue<Long> queue = new ArrayDeque<>(maxBlocksVal * 4);
        Set<Long> visited = new HashSet<>(maxBlocksVal * 4);

        long startPacked = encodePos(startX, startY, startZ);
        visited.add(startPacked);
        
        // 開始ブロックを結果に追加する
        blocks.add(new BlockPos(startX, startY, startZ));
        
        addNeighbors(queue, visited, startX, startY, startZ);

        while (!queue.isEmpty() && blocks.size() < maxBlocksVal) {
            long packed = queue.poll();
            int px = getX(packed);
            int py = getY(packed);
            int pz = getZ(packed);

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
    private void addNeighbors(Queue<Long> queue, Set<Long> visited, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;
                    long packed = encodePos(nx, ny, nz);
                    // visitedへの追加に成功した場合（未訪問の場合）のみキューに登録する
                    if (visited.add(packed)) {
                        queue.add(packed);
                    }
                }
            }
        }
    }
}
