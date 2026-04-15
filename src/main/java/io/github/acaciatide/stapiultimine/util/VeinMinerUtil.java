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

    // アイテムのテレポート状態を管理するためのフラグとプレイヤー情報
    public static boolean isTeleportingDrops = false;
    public static PlayerEntity currentPlayer = null;
    public static BlockPos originBlockPos = null;
    public static VeinMineMode currentMode = VeinMineMode.SHAPELESS;

    /**
     * モードを次の値に循環させる。方向が正なら次へ、負なら前へ。
     * @param direction スクロールの方向
     */
    public static void cycleMode(int direction) {
        VeinMineMode[] modes = VeinMineMode.values();
        int nextIndex = currentMode.ordinal();
        if (direction > 0) {
            nextIndex = (nextIndex + 1) % modes.length;
        } else if (direction < 0) {
            nextIndex = (nextIndex - 1 + modes.length) % modes.length;
        }
        currentMode = modes[nextIndex];
        
        // モードが変わったらレンダリングキャッシュを強制的にリセットして再計算させる
        UltimineRenderCache.resetCache();
    }

    public static String getNextModeName() {
        VeinMineMode[] modes = VeinMineMode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        return modes[nextIndex].getName();
    }

    public static String getPrevModeName() {
        VeinMineMode[] modes = VeinMineMode.values();
        int prevIndex = (currentMode.ordinal() - 1 + modes.length) % modes.length;
        return modes[prevIndex].getName();
    }

    /**
     * 起点となるブロックから周囲を探索し、同種ブロックを一括破壊する。
     */
    public static void mineVein(World world, PlayerEntity player, int startX, int startY, int startZ, Block block, int meta) {
        if (isMining) return;
        
        // ツールの適正チェック
        boolean canHarvest = player.canHarvest(block);
        // 無条件破壊がオフ かつ 適正ツールでない場合は一括破壊を中止する
        if (!ConfigInit.CONFIG.forceVeinMine && !canHarvest) return;
        if (block == null) return;
        isMining = true;

        try {
            Set<BlockPos> blocksToMine = getVeinBlocks(world, startX, startY, startZ, block, meta);
            
            for (BlockPos pos : blocksToMine) {
                // 開始地点自体はInteractionManager側で壊されるためスキップする
                if (pos.x == startX && pos.y == startY && pos.z == startZ) continue;

                int currentId = world.getBlockId(pos.x, pos.y, pos.z);
                int currentMeta = world.getBlockMeta(pos.x, pos.y, pos.z);

                // ブロックがまだ存在し、種類が一致しているか再確認
                if (currentId == block.id && currentMeta == meta) {
                    // ブロックを空気(0)に置換
                    world.setBlock(pos.x, pos.y, pos.z, 0);
                    
                    // 適正ツールがある場合のみ、アイテムドロップや統計処理を呼び出す
                    if (canHarvest) {
                        try {
                            if (ConfigInit.CONFIG.teleportDrops) {
                                isTeleportingDrops = true;
                                currentPlayer = player;
                                block.afterBreak(world, player, (int) player.x, (int) player.y, (int) player.z, currentMeta);
                            } else {
                                block.afterBreak(world, player, pos.x, pos.y, pos.z, currentMeta);
                            }
                        } finally {
                            isTeleportingDrops = false;
                            currentPlayer = null;
                        }
                    }

                    // 手持ちアイテム（ツール）の耐久値消費
                    if (ConfigInit.CONFIG.consumeDurability) {
                        ItemStack heldItem = player.getHand();
                        if (heldItem != null && heldItem.isDamageable()) {
                            heldItem.damage(1, player);
                            if (heldItem.count <= 0) {
                                heldItem.onRemoved(player);
                                player.clearStackInHand();
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            isMining = false;
        }
    }

    /**
     * 一括破壊の対象となるブロックの座標セットを取得する
     */
    public static Set<BlockPos> getVeinBlocks(World world, int startX, int startY, int startZ, Block block, int meta) {
        Set<BlockPos> blocks = new HashSet<>();
        if (block == null) return blocks;

        int blockId = block.id;
        int maxBlocks = Math.max(1, Math.min(256, ConfigInit.CONFIG.maxBlocks));
        
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        BlockPos start = new BlockPos(startX, startY, startZ);
        visited.add(start);
        blocks.add(start);
        addNeighbors(queue, visited, startX, startY, startZ);

        while (!queue.isEmpty() && blocks.size() < maxBlocks) {
            BlockPos pos = queue.poll();

            int currentId = world.getBlockId(pos.x, pos.y, pos.z);
            int currentMeta = world.getBlockMeta(pos.x, pos.y, pos.z);

            if (currentId == blockId && currentMeta == meta) {
                blocks.add(pos);
                addNeighbors(queue, visited, pos.x, pos.y, pos.z);
            }
        }
        return blocks;
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

    public static class BlockPos {
        public final int x, y, z;
        public BlockPos(int x, int y, int z) {
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
