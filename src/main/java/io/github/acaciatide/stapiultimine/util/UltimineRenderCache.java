package io.github.acaciatide.stapiultimine.util;

import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import net.minecraft.block.Block;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Set;

public class UltimineRenderCache {

    private static int lastX = Integer.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static int lastZ = Integer.MIN_VALUE;
    private static Set<VeinMinerUtil.BlockPos> cachedBlocks = Collections.emptySet();

    /**
     * 現在の視点ターゲットに基づき、一括破壊対象の座標リストを（キャッシュを考慮して）取得する
     */
    public static Set<VeinMinerUtil.BlockPos> getCachedBlocks(World world, HitResult hit) {
        // キーが押されていないならキャッシュをクリアして空を返す
        if (!ClientInitListener.isUltimineKeyPressed()) {
            cachedBlocks = Collections.emptySet();
            lastX = lastY = lastZ = Integer.MIN_VALUE;
            return cachedBlocks;
        }

        // ブロックを見ていないなら空を返す
        if (hit == null || hit.type != HitResultType.BLOCK) {
            cachedBlocks = Collections.emptySet();
            lastX = lastY = lastZ = Integer.MIN_VALUE;
            return cachedBlocks;
        }

        // 向いている座標が変わった場合のみ再計算する
        if (hit.blockX != lastX || hit.blockY != lastY || hit.blockZ != lastZ) {
            lastX = hit.blockX;
            lastY = hit.blockY;
            lastZ = hit.blockZ;
            
            int id = world.getBlockId(lastX, lastY, lastZ);
            if (id > 0) {
                Block block = Block.BLOCKS[id];
                int meta = world.getBlockMeta(lastX, lastY, lastZ);
                cachedBlocks = VeinMinerUtil.getVeinBlocks(world, lastX, lastY, lastZ, block, meta);
            } else {
                cachedBlocks = Collections.emptySet();
            }
        }

        return cachedBlocks;
    }
}
