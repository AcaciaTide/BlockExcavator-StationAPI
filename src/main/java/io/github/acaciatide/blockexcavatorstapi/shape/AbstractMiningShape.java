package io.github.acaciatide.blockexcavatorstapi.shape;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;

public abstract class AbstractMiningShape implements MiningShape {

    /**
     * 空気、岩盤、液体などの「採掘不可能」なブロックであるかを判定します。
     */
    public static boolean isInvalidBlock(int blockId) {
        if (blockId <= 0) return true; // 空気や未定義

        Block block = Block.BLOCKS[blockId];
        if (block == null) return true;

        // 硬度がマイナス（岩盤、ポータルなど破壊不能なもの）
        if (block.getHardness() < 0.0F) return true;

        // 液体（バニラの水、溶岩に加え、Mod追加の液体も FluidMaterial クラスから判定）
        return block.material instanceof net.minecraft.block.material.FluidMaterial;
    }

    /**
     * 岩盤やポータルなど「破壊不能」な無効ブロックであるかを判定し、探索を停止すべきか返します。
     */
    protected boolean shouldStopOnInvalidBlock(int blockId) {
        if (blockId <= 0) return false;
        Block block = Block.BLOCKS[blockId];
        return block != null && block.getHardness() < 0.0F;
    }

    /**
     * プレイヤーがそのブロックを破壊可能か（ツール適正があるか）を判定します。
     * 設定によってスキップされる場合があります。
     */
    protected boolean canHarvest(PlayerEntity player, int blockId) {
        // 無条件破壊(forceVeinMine)がONの時はチェックをスキップ
        if (Boolean.TRUE.equals(ConfigInit.ADVANCED.forceVeinMine)) {
            return true;
        }

        // Strict Tool Check がOFFの時はチェックをスキップ
        if (Boolean.FALSE.equals(ConfigInit.ADVANCED.strictToolCheck)) {
            return true;
        }

        if (player == null) {
            return true;
        }

        Block block = Block.BLOCKS[blockId];
        return block != null && player.canHarvest(block);
    }

    /**
     * プレイヤーの現在の向いている方向（水平方向）を取得します。
     */
    protected net.modificationstation.stationapi.api.util.math.Direction getPlayerFacing(PlayerEntity player) {
        if (player == null) {
            return net.modificationstation.stationapi.api.util.math.Direction.SOUTH;
        }
        return net.modificationstation.stationapi.api.util.math.Direction.fromRotation(player.yaw);
    }
}
