package io.github.acaciatide.blockexcavatorstapi.util;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public class VeinMinerUtil {

    private VeinMinerUtil() {}

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
        ExcavatorRenderCache.resetCache();
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
     * テレポート関連のフラグをリセットする。
     */
    public static void resetTeleportStatus() {
        isTeleportingDrops = false;
        currentPlayer = null;
        originBlockPos = null;
    }

    /**
     * 起点となるブロックから周囲を探索し、同種ブロックを一括破壊する。
     */
    public static void mineVein(World world, PlayerEntity player, int startX, int startY, int startZ, Block block, int meta, int face) {
        if (isMining) return;
        
        // ツールの適正チェック
        boolean canHarvest = player.canHarvest(block);
        // 無条件破壊がオフ かつ 適正ツールでない場合は一括破壊を中止する
        if (!ConfigInit.ADVANCED.forceVeinMine && !canHarvest) return;
        if (block == null) return;
        isMining = true;

        try {
            Set<BlockPos> blocksToMine = getVeinBlocks(world, player, startX, startY, startZ, block, meta, face);
            
            for (BlockPos pos : blocksToMine) {
                // 開始地点自体はInteractionManager側で壊されるためスキップする
                if (pos.getX() == startX && pos.getY() == startY && pos.getZ() == startZ) continue;

                int currentId = world.getBlockId(pos.getX(), pos.getY(), pos.getZ());
                int currentMeta = world.getBlockMeta(pos.getX(), pos.getY(), pos.getZ());

                // 耐久値ストップの事前チェック
                if (ConfigInit.GENERAL.consumeDurability && ConfigInit.GENERAL.stopAtDurability > 0) {
                    ItemStack held = player.getHand();
                    if (held != null && held.isDamageable()) {
                        int remaining = held.getMaxDamage() - held.getDamage();
                        if (remaining <= ConfigInit.GENERAL.stopAtDurability) {
                            break; // これ以上破壊しない
                        }
                    }
                }

                // ブロックがまだ存在するかのみをチェック（詳細な条件はgetVeinBlocksで検証済み）
                if (currentId > 0) {
                    Block currentBlock = Block.BLOCKS[currentId];
                    // ブロックを空気(0)に置換
                    world.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0);
                    
                    // 個別のブロックに対するツール適正判定
                    boolean currentCanHarvest = player.canHarvest(currentBlock);
                    
                    // 適正ツールがある場合のみ、アイテムドロップや統計処理を呼び出す
                    if (currentCanHarvest && currentBlock != null) {
                        // teleportDropsがONの場合はフラグを立てておき、ItemEntityMixinにフックさせる
                        if (ConfigInit.GENERAL.teleportDrops) {
                            isTeleportingDrops = true;
                            currentPlayer = player;
                        }
                        // 常に実際のブロックの座標を渡す（爆発などの他Modのギミックが正しく動くようにするため）
                        currentBlock.afterBreak(world, player, pos.getX(), pos.getY(), pos.getZ(), currentMeta);
                    }

                    // 手持ちアイテム（ツール）の耐久値消費
                    if (ConfigInit.GENERAL.consumeDurability) {
                        ItemStack heldItem = player.getHand();
                        if (heldItem != null) {
                            heldItem.postMine(currentId, pos.getX(), pos.getY(), pos.getZ(), player);
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
        if (block == null) return new java.util.HashSet<>();
        
        // 現在のモードに紐付けられた独自ロジックを呼び出す
        return currentMode.getShape().getBlocks(world, player, startX, startY, startZ, block, meta, face);
    }
}
