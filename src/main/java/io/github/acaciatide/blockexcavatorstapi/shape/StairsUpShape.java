package io.github.acaciatide.blockexcavatorstapi.shape;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class StairsUpShape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        
        // プレイヤーの向いている方向（前）を取得
        Direction dir = getPlayerFacing(player);
        
        int maxLength = Math.max(1, ConfigInit.GENERAL.tunnelMaxBlocks);

        for (int i = 0; i < maxLength; i++) {
            // 前に1ブロック、上に1ブロックずつ進む
            int bx = startX + (dir.getOffsetX() * i);
            int by = startY + i;
            int bz = startZ + (dir.getOffsetZ() * i);

            int currentId = world.getBlockId(bx, by, bz);
            int currentMeta = world.getBlockMeta(bx, by, bz);

            if (isInvalidBlock(currentId)) {
                if (shouldStopOnInvalidBlock(currentId)) {
                    break;
                }
                continue;
            }

            if (!canHarvest(player, currentId)) {
                continue;
            }

            blocks.add(new BlockPos(bx, by, bz));
        }

        return blocks;
    }
}
