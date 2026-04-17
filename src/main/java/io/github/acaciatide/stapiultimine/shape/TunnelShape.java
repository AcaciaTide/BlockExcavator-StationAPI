package io.github.acaciatide.stapiultimine.shape;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class TunnelShape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        
        // 叩いた面の「反対側」を掘る方向として決定する
        Direction dir = Direction.byId(face).getOpposite();
        
        int maxLength = Math.max(1, ConfigInit.CONFIG.tunnelMaxBlocks);

        for (int i = 0; i < maxLength; i++) {
            int bx = startX + (dir.getOffsetX() * i);
            int by = startY + (dir.getOffsetY() * i);
            int bz = startZ + (dir.getOffsetZ() * i);

            int currentId = world.getBlockId(bx, by, bz);
            int currentMeta = world.getBlockMeta(bx, by, bz);

            // 有効ブロックチェック (岩盤やポータルなどをスキップ)
            if (isInvalidBlock(currentId)) {
                // 岩盤やポータルなど「破壊不能」なブロックにぶつかったらそこでトンネル探索を停止
                Block currentBlock = currentId > 0 ? Block.BLOCKS[currentId] : null;
                if (currentBlock != null && currentBlock.getHardness() < 0.0F) {
                    break;
                }
                // 空気や液体などの場合はスキップして奥の探索を継続
                continue;
            }

            // ツール適正チェック
            if (!canHarvest(player, currentId)) {
                continue;
            }

            // ハンマーモードでなければ起点ブロックと同じ種類のみ掘る
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
