package io.github.acaciatide.stapiultimine.shape;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.modificationstation.stationapi.api.util.math.MutableBlockPos;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ShapelessShape extends AbstractMiningShape {
    @Override
    public Set<BlockPos> getBlocks(World world, PlayerEntity player, int startX, int startY, int startZ, Block startBlock, int startMeta, int face) {
        Set<BlockPos> blocks = new HashSet<>();
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

            if (currentId == startBlock.id && currentMeta == startMeta) {
                // ツール適正チェック
                if (!canHarvest(player, currentId)) {
                    continue;
                }
                blocks.add(pos);
                addNeighbors(queue, visited, pos.getX(), pos.getY(), pos.getZ(), mutablePos);
            }
        }
        return blocks;
    }

    private void addNeighbors(Queue<BlockPos> queue, Set<BlockPos> visited, int x, int y, int z, MutableBlockPos mutablePos) {
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
