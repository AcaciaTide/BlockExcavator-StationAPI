package io.github.acaciatide.stapiultimine.shape;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public interface MiningShape {
    /**
     * 指定された条件に基づいて、一括破壊の対象となるブロックのセットを返します。
     */
    Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face);
}
