package io.github.acaciatide.blockexcavatorstapi.mixin;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import io.github.acaciatide.blockexcavatorstapi.events.init.ClientInitListener;
import io.github.acaciatide.blockexcavatorstapi.util.VeinMinerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InteractionManager.class)
public class InteractionManagerMixin {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Unique
    private boolean blockexcavatorstapi_isStartingVeinMine = false;

    @Inject(method = "breakBlock", at = @At("HEAD"))
    public void onBreakBlock(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
        // 次回の破壊検知時に、以前の残骸（起点ブロックの記憶）を完全にリセットする
        VeinMinerUtil.originBlockPos = null;
        if (!ConfigInit.ADVANCED.teleportDrops) {
            VeinMinerUtil.currentPlayer = null;
        }
        
        // 例外等でフラグが残っていた場合のフェイルセーフ
        VeinMinerUtil.isTeleportingDrops = false;
        blockexcavatorstapi_isStartingVeinMine = false;

        // キーが押されている場合のみ一括破壊を試行する
        if (ClientInitListener.isExcavatorKeyPressed()) {
            // マルチプレイ時はサーバーサイドのMixinが処理するためスキップする
            if (this.minecraft.isWorldRemote()) return;

            // シングルプレイ時は従来通りクライアント側で処理する
            World world = this.minecraft.world;
            int blockId = world.getBlockId(x, y, z);
            if (blockId > 0) {
                int meta = world.getBlockMeta(x, y, z);
                Block block = Block.BLOCKS[blockId];
                
                // バニラのブロック破壊処理（起点ブロック）向けに情報を記憶させる
                if (ConfigInit.ADVANCED.teleportDrops) {
                    VeinMinerUtil.originBlockPos = new BlockPos(x, y, z);
                    VeinMinerUtil.currentPlayer = this.minecraft.player;
                    VeinMinerUtil.isTeleportingDrops = true;
                }
                
                blockexcavatorstapi_isStartingVeinMine = true;

                // 実際にブロックが破壊される前に、その周囲の同種ブロックを破壊するロジックを呼び出す
                VeinMinerUtil.mineVein(world, this.minecraft.player, x, y, z, block, meta, direction);
            }
        } else {
            // 一括破壊キーが押されていない通常破壊時はリセット
            VeinMinerUtil.resetTeleportStatus();
        }
    }

    @Inject(method = "breakBlock", at = @At("RETURN"))
    public void afterBreakBlock(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
        if (blockexcavatorstapi_isStartingVeinMine) {
            blockexcavatorstapi_isStartingVeinMine = false;
            // シングルプレイ環境では、このメソッドから抜けた直後（派生クラス）で起点ブロックの 
            // afterBreak(アイテムドロップ)が呼ばれる為、originBlockPos と currentPlayer は保持しておく。
            // 別の通常破壊で誤爆しないよう、グローバルフラグだけをfalseに戻す。
            VeinMinerUtil.isTeleportingDrops = false;
        }
    }
}
