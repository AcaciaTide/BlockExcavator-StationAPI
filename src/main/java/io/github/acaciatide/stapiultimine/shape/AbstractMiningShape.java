package io.github.acaciatide.stapiultimine.shape;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public abstract class AbstractMiningShape implements MiningShape {

    /**
     * 空気、岩盤、液体などの「採掘不可能」なブロックであるかを判定します。
     */
    protected boolean isInvalidBlock(int blockId) {
        if (blockId <= 0) return true; // 空気や未定義

        Block block = Block.BLOCKS[blockId];
        if (block == null) return true;

        // 硬度がマイナス（岩盤、ポータルなど破壊不能なもの）
        if (block.getHardness() < 0.0F) return true;

        // 液体（バニラの水、溶岩に加え、Mod追加の液体も FluidMaterial クラスから判定）
        if (block.material instanceof net.minecraft.block.material.FluidMaterial) return true;
        
        return false;
    }

    /**
     * プレイヤーがそのブロックを破壊可能か（ツール適正があるか）を判定します。
     * 設定によってスキップされる場合があります。
     */
    protected boolean canHarvest(PlayerEntity player, int blockId) {
        // 無条件破壊(forceVeinMine)がONの時はチェックをスキップ
        if (ConfigInit.CONFIG.forceVeinMine) {
            return true;
        }

        // Strict Tool Check がOFFの時はチェックをスキップ
        if (!ConfigInit.CONFIG.strictToolCheck) {
            return true;
        }

        if (player == null) {
            return true;
        }

        Block block = Block.BLOCKS[blockId];
        return block != null && player.canHarvest(block);
    }
}
