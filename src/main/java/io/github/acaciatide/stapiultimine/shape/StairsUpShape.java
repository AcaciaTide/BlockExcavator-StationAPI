package io.github.acaciatide.stapiultimine.shape;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class StairsUpShape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        
        // プレイヤーの向いている方向（前）を取得
        Direction dir = getPlayerFacing(player);
        
        int maxLength = Math.max(1, ConfigInit.CONFIG.tunnelMaxBlocks);

        for (int i = 0; i < maxLength; i++) {
            // 前に1ブロック、上に1ブロックずつ進む
            int bx = startX + (dir.getOffsetX() * i);
            int by = startY + i;
            int bz = startZ + (dir.getOffsetZ() * i);

            int currentId = world.getBlockId(bx, by, bz);
            int currentMeta = world.getBlockMeta(bx, by, bz);

            if (isInvalidBlock(currentId)) {
                Block currentBlock = currentId > 0 ? Block.BLOCKS[currentId] : null;
                if (currentBlock != null && currentBlock.getHardness() < 0.0F) {
                    break; // 岩盤などにぶつかったら停止
                }
                continue; // 空気や液体はスキップ
            }

            if (!canHarvest(player, currentId)) {
                continue;
            }

            if (!ConfigInit.CONFIG.hammerMode3x3) {
                if (currentId != startBlock.id || currentMeta != startMeta) {
                    continue;
                }
            }

            blocks.add(new BlockPos(bx, by, bz));
        }

        return blocks;
    }
}
