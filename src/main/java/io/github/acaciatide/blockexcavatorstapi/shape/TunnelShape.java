package io.github.acaciatide.blockexcavatorstapi.shape;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class TunnelShape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        
        // 叩いた面の「反対側」を掘る方向として決定する
        Direction dir = Direction.byId(face).getOpposite();
        
        int maxLength = Math.max(1, ConfigInit.GENERAL.tunnelMaxBlocks);

        for (int i = 0; i < maxLength; i++) {
            int bx = startX + (dir.getOffsetX() * i);
            int by = startY + (dir.getOffsetY() * i);
            int bz = startZ + (dir.getOffsetZ() * i);

            int currentId = world.getBlockId(bx, by, bz);
            int currentMeta = world.getBlockMeta(bx, by, bz);

            // 有効ブロックチェック (岩盤やポータルなどをスキップ)
            if (isInvalidBlock(currentId)) {
                if (shouldStopOnInvalidBlock(currentId)) {
                    break;
                }
                continue;
            }

            // ツール適正チェック
            if (!canHarvest(player, currentId)) {
                continue;
            }

            blocks.add(new BlockPos(bx, by, bz));
        }

        return blocks;
    }
}
