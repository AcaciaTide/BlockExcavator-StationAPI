package io.github.acaciatide.stapiultimine.util;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class VeinMinerUtil {

    // 再帰呼び出しを防止するためのフラグ
    private static boolean isMining = false;

    /**
     * 起点となるブロックから周囲を探索し、同種ブロックを一括破壊する。
     */
    public static void mineVein(World world, PlayerEntity player, int startX, int startY, int startZ, Block block, int meta) {
        if (isMining) return;
        
        // ツールの適正チェック
        boolean canHarvest = player.canHarvest(block);
        // 無条件破壊がオフ かつ 適正ツールでない場合は一括破壊を中止する
        if (!ConfigInit.CONFIG.forceVeinMine && !canHarvest) return;
        isMining = true;

        try {
            int blockId = block.id;
            // 設定から最大ブロック数を取得。(念のため1〜256の範囲に収める)
            int maxBlocks = Math.max(1, Math.min(256, ConfigInit.CONFIG.maxBlocks));
            int minedCount = 0;

            Queue<BlockPos> queue = new LinkedList<>();
            Set<BlockPos> visited = new HashSet<>();

            // 最初の一歩（自分が壊したブロックの周囲）
            addNeighbors(queue, visited, startX, startY, startZ);

            while (!queue.isEmpty() && minedCount < maxBlocks) {
                BlockPos pos = queue.poll();

                int currentId = world.getBlockId(pos.x, pos.y, pos.z);
                int currentMeta = world.getBlockMeta(pos.x, pos.y, pos.z);

                // 同種ブロック（IDとメタデータが一致）か確認
                if (currentId == blockId && currentMeta == meta) {
                    // ブロックを空気(0)に置換
                    world.setBlock(pos.x, pos.y, pos.z, 0);
                    
                    // 適正ツールがある場合のみ、アイテムドロップや統計処理を呼び出す
                    // (無条件破壊で素手の時はアイテム化しない)
                    if (canHarvest) {
                        block.afterBreak(world, player, pos.x, pos.y, pos.z, currentMeta);
                    }

                    // 手持ちアイテム（ツール）の耐久値消費
                    if (ConfigInit.CONFIG.consumeDurability) {
                        ItemStack heldItem = player.getHand();
                        if (heldItem != null && heldItem.isDamageable()) {
                            heldItem.damage(1, player);
                            
                            // 耐久値がゼロになり破損した場合
                            if (heldItem.count <= 0) {
                                heldItem.onRemoved(player);
                                player.clearStackInHand();
                                // ツールが壊れたため、残りの連鎖を安全に終了させる
                                break;
                            }
                        }
                    }

                    minedCount++;
                    addNeighbors(queue, visited, pos.x, pos.y, pos.z);
                }
            }
        } finally {
            isMining = false;
        }
    }

    private static void addNeighbors(Queue<BlockPos> queue, Set<BlockPos> visited, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = new BlockPos(x + dx, y + dy, z + dz);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private static class BlockPos {
        final int x, y, z;
        BlockPos(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockPos blockPos = (BlockPos) o;
            return x == blockPos.x && y == blockPos.y && z == blockPos.z;
        }
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
