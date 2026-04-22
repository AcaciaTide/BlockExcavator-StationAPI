package io.github.acaciatide.stapiultimine.shape;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class Square3x3Shape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
        Direction dir = Direction.byId(face);
        Direction.Axis axis = dir.getAxis();
        
        for (int d1 = -1; d1 <= 1; d1++) {
            for (int d2 = -1; d2 <= 1; d2++) {
                int bx = startX;
                int by = startY;
                int bz = startZ;

                if (axis == Direction.Axis.Y) {
                    bx += d1;
                    bz += d2;
                } else if (axis == Direction.Axis.Z) {
                    bx += d1;
                    by += d2;
                } else if (axis == Direction.Axis.X) {
                    by += d1;
                    bz += d2;
                }

                int currentId = world.getBlockId(bx, by, bz);
                int currentMeta = world.getBlockMeta(bx, by, bz);

                // 基本的な無効ブロックチェック
                if (isInvalidBlock(currentId)) {
                    continue;
                }

                // ツール適正チェック
                if (!canHarvest(player, currentId)) {
                    continue;
                }

                if (ConfigInit.GENERAL.hammerMode3x3) {
                    // ハンマーモード：壊せるブロックなら何でも追加
                    blocks.add(new BlockPos(bx, by, bz));
                } else {
                    // 安全モード：最初にクリックしたものと同じブロックのみ追加
                    if (currentId == startBlock.id && currentMeta == startMeta) {
                        blocks.add(new BlockPos(bx, by, bz));
                    }
                }
            }
        }
        return blocks;
    }
}
