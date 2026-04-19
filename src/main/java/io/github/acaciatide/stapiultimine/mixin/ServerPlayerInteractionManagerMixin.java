package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import io.github.acaciatide.stapiultimine.server.PlayerStateManager;
import io.github.acaciatide.stapiultimine.util.VeinMineMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * サーバーサイドのブロック破壊フックを担うMixin。
 * 各プレイヤー固有の ServerPlayerInteractionManager インスタンスに適用される。
 */
@Environment(EnvType.SERVER)
@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow private ServerWorld world;
    @Shadow public PlayerEntity player;

    /**
     * 再帰防止フラグ。
     * tryBreakBlock() → Mixin → tryBreakBlock() のループを断つために使用する。
     * @Unique でインスタンスフィールドとして注入されるため各プレイヤーが独立した値を持つ。
     */
    @Unique
    private boolean stapiultimine_isVeinMining = false;

    /**
     * 最後に受け取ったブロック破壊アクションの面情報を保持する。
     * onBlockBreakingAction で保存し、tryBreakBlock で参照する（3x3モード用）。
     */
    @Unique
    private int stapiultimine_lastFace = 0;

    //プレイヤーが叩いたブロックの面方向（direction）を記録しておく。
    @Inject(method = "onBlockBreakingAction", at = @At("HEAD"))
    private void captureDirection(int x, int y, int z, int direction, CallbackInfo ci) {
        stapiultimine_lastFace = direction;
    }

    //プレイヤーの Ultimine が有効な場合、追加ブロックを一括破壊する。
    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void onTryBreakBlock(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        // 再帰呼び出しを検出したら即座に返る
        if (stapiultimine_isVeinMining) return;
        // Ultimineキーが押されていないプレイヤーは対象外
        if (!PlayerStateManager.isUltimineActive(this.player)) return;

        int blockId = this.world.getBlockId(x, y, z);
        if (blockId <= 0) return;

        Block block = Block.BLOCKS[blockId];
        if (block == null) return;

        int meta = this.world.getBlockMeta(x, y, z);

        // ツール適正チェック（forceVeinMine=falseかつ非適正ツールなら中止する）
        if (!ConfigInit.CONFIG.forceVeinMine && !this.player.canHarvest(block)) return;

        // プレイヤーごとに管理されているモードを取得する（クライアントのstatic変数は参照しない）
        VeinMineMode mode = PlayerStateManager.getMode(this.player);

        // 現在のモードに応じたブロックセットを計算する
        Set<BlockPos> targets = mode.getShape().getBlocks(
                this.world, this.player, x, y, z, block, meta, stapiultimine_lastFace
        );

        stapiultimine_isVeinMining = true;
        try {
            // 自身を ServerPlayerInteractionManager としてキャストして tryBreakBlock() を呼ぶ
            ServerPlayerInteractionManager self = (ServerPlayerInteractionManager) (Object) this;
            int count = 0;

            for (BlockPos pos : targets) {
                // 設定の上限に達したら中断する（形状側でも制限されているが安全のため二重チェックする）
                if (count >= ConfigInit.CONFIG.maxBlocks) break;

                // 起点ブロックはバニラの tryBreakBlock がそのまま処理するためスキップする
                if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) continue;

                self.tryBreakBlock(pos.getX(), pos.getY(), pos.getZ());
                count++;

                // ツールが破壊されたら追加破壊を中断する
                if (this.player.getHand() == null) break;
            }
        } finally {
            // 例外が発生してもフラグを確実に解除する
            stapiultimine_isVeinMining = false;
        }
    }
}
