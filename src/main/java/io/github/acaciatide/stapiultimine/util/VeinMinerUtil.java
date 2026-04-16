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
    public static void mineVein(World world, PlayerEntity player, int startX, int startY, int startZ, Block block, int meta, int face) {
        if (isMining) return;
        
        // ツールの適正チェック
        boolean canHarvest = player.canHarvest(block);
        // 無条件破壊がオフ かつ 適正ツールでない場合は一括破壊を中止する
        if (!ConfigInit.CONFIG.forceVeinMine && !canHarvest) return;
        if (block == null) return;
        isMining = true;

        try {
            Set<BlockPos> blocksToMine = getVeinBlocks(world, player, startX, startY, startZ, block, meta, face);
            
            for (BlockPos pos : blocksToMine) {
                // 開始地点自体はInteractionManager側で壊されるためスキップする
                if (pos.getX() == startX && pos.getY() == startY && pos.getZ() == startZ) continue;

                int currentId = world.getBlockId(pos.getX(), pos.getY(), pos.getZ());
                int currentMeta = world.getBlockMeta(pos.getX(), pos.getY(), pos.getZ());

                // ブロックがまだ存在し、モードごとの条件に合致しているか再確認
                boolean shouldBreak = false;
                if (currentMode == VeinMineMode.SQUARE_3X3 && ConfigInit.CONFIG.hammerMode3x3) {
                    if (currentId > 0 && currentId != 7 && currentId != 8 && currentId != 9 && currentId != 10 && currentId != 11 && currentId != 51 && currentId != 90) {
                        shouldBreak = true;
                    }
                } else {
                    if (currentId == block.id && currentMeta == meta) {
                        shouldBreak = true;
                    }
                }

                if (shouldBreak) {
                    Block currentBlock = Block.BLOCKS[currentId];
                    // ブロックを空気(0)に置換
                    world.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0);
                    
                    // 個別のブロックに対するツール適正判定
                    boolean currentCanHarvest = player.canHarvest(currentBlock);
                    
                    // 適正ツールがある場合のみ、アイテムドロップや統計処理を呼び出す
                    if (currentCanHarvest && currentBlock != null) {
                        try {
                            if (ConfigInit.CONFIG.teleportDrops) {
                                isTeleportingDrops = true;
                                currentPlayer = player;
                                currentBlock.afterBreak(world, player, (int) player.x, (int) player.y, (int) player.z, currentMeta);
                            } else {
                                currentBlock.afterBreak(world, player, pos.getX(), pos.getY(), pos.getZ(), currentMeta);
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
    public static Set<BlockPos> getVeinBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block block, int meta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        if (block == null) return blocks;

        int blockId = block.id;

        if (currentMode == VeinMineMode.SQUARE_3X3) {
            net.modificationstation.stationapi.api.util.math.Direction dir = net.modificationstation.stationapi.api.util.math.Direction.byId(face);
            net.modificationstation.stationapi.api.util.math.Direction.Axis axis = dir.getAxis();
            
            for (int d1 = -1; d1 <= 1; d1++) {
                for (int d2 = -1; d2 <= 1; d2++) {
                    int bx = startX;
                    int by = startY;
                    int bz = startZ;

                    if (axis == net.modificationstation.stationapi.api.util.math.Direction.Axis.Y) {
                        bx += d1;
                        bz += d2;
                    } else if (axis == net.modificationstation.stationapi.api.util.math.Direction.Axis.Z) {
                        bx += d1;
                        by += d2;
                    } else if (axis == net.modificationstation.stationapi.api.util.math.Direction.Axis.X) {
                        by += d1;
                        bz += d2;
                    }

                    int currentId = world.getBlockId(bx, by, bz);
                    int currentMeta = world.getBlockMeta(bx, by, bz);

                    // 空気、岩盤、水、溶岩、炎、ポータルなどの削れないブロックを安全に除外
                    if (currentId <= 0 || currentId == 7 || currentId == 8 || currentId == 9 || currentId == 10 || currentId == 11 || currentId == 51 || currentId == 90) {
                        continue;
                    }

                    // ツール適正チェック (無条件破壊がONの時はリミッター解除)
                    if (ConfigInit.CONFIG.strictToolCheck && !ConfigInit.CONFIG.forceVeinMine && player != null) {
                        Block currentBlock = Block.BLOCKS[currentId];
                        if (currentBlock != null && !player.canHarvest(currentBlock)) {
                            continue;
                        }
                    }

                    if (ConfigInit.CONFIG.hammerMode3x3) {
                        // ハンマーモード：壊せるブロックなら何でも追加
                        blocks.add(new BlockPos(bx, by, bz));
                    } else {
                        // 安全モード：最初にクリックしたものと同じブロックのみ追加
                        if (currentId == blockId && currentMeta == meta) {
                            blocks.add(new BlockPos(bx, by, bz));
                        }
                    }
                }
            }
            return blocks;
        }

        // --- 以下は SHAPELESS モードの処理 ---
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
                // ツール適正チェック (無条件破壊がONの時はリミッター解除)
                if (ConfigInit.CONFIG.strictToolCheck && !ConfigInit.CONFIG.forceVeinMine && player != null) {
                    Block currentBlock = Block.BLOCKS[currentId];
                    if (currentBlock != null && !player.canHarvest(currentBlock)) {
                        continue;
                    }
                }
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
