package io.github.acaciatide.stapiultimine.util;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.MutableBlockPos;

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
                if (pos.getX() == startX && pos.getY() == startY && pos.getZ() == startZ) continue;

                int currentId = world.getBlockId(pos.getX(), pos.getY(), pos.getZ());
                int currentMeta = world.getBlockMeta(pos.getX(), pos.getY(), pos.getZ());

                // ブロックがまだ存在し、種類が一致しているか再確認
                if (currentId == block.id && currentMeta == meta) {
                    // ブロックを空気(0)に置換
                    world.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0);
                    
                    // 適正ツールがある場合のみ、アイテムドロップや統計処理を呼び出す
                    if (canHarvest) {
                        try {
                            if (ConfigInit.CONFIG.teleportDrops) {
                                isTeleportingDrops = true;
                                currentPlayer = player;
                                block.afterBreak(world, player, (int) player.x, (int) player.y, (int) player.z, currentMeta);
                            } else {
                                block.afterBreak(world, player, pos.getX(), pos.getY(), pos.getZ(), currentMeta);
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
        MutableBlockPos mutablePos = new MutableBlockPos();

        BlockPos start = new BlockPos(startX, startY, startZ);
        visited.add(start);
        blocks.add(start);
        addNeighbors(queue, visited, startX, startY, startZ, mutablePos);

        while (!queue.isEmpty() && blocks.size() < maxBlocks) {
            BlockPos pos = queue.poll();

            int currentId = world.getBlockId(pos.getX(), pos.getY(), pos.getZ());
            int currentMeta = world.getBlockMeta(pos.getX(), pos.getY(), pos.getZ());

            if (currentId == blockId && currentMeta == meta) {
                blocks.add(pos);
                addNeighbors(queue, visited, pos.getX(), pos.getY(), pos.getZ(), mutablePos);
            }
        }
        return blocks;
    }

    private static void addNeighbors(Queue<BlockPos> queue, Set<BlockPos> visited, int x, int y, int z, MutableBlockPos mutablePos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    mutablePos.set(x + dx, y + dy, z + dz);
                    if (!visited.contains(mutablePos)) {
                        BlockPos immutablePos = mutablePos.toImmutable();
                        visited.add(immutablePos);
                        queue.add(immutablePos);
                    }
                }
            }
        }
    }
}
