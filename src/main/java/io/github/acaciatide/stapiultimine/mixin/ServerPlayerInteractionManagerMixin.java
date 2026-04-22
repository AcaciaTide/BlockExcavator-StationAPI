package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import io.github.acaciatide.stapiultimine.server.PlayerStateManager;
import io.github.acaciatide.stapiultimine.util.VeinMineMode;
import io.github.acaciatide.stapiultimine.util.VeinMinerUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    /** tryBreakBlock() の内部処理のうちブロック消去を行うメソッド（public のため @Shadow 直接取得可能） */
    @Shadow public boolean finishMining(int x, int y, int z) { throw new AssertionError(); }

    /**
     * 再帰防止フラグ。
     * tryBreakBlock() → Mixin → stapiultimine_breakBlock() のループを断つために使用する。
     * @Unique でインスタンスフィールドとして注入されるため各プレイヤーが独立した値を持つ。
     */
    @Unique
    private boolean stapiultimine_isVeinMining = false;

    /**
     * トップレベルの tryBreakBlock 呼び出しであることを追跡するフラグ。
     */
    @Unique
    private boolean stapiultimine_isStartingVeinMine = false;

    /**
     * 最後に受け取ったブロック破壊アクションの面情報を保持する。
     * onBlockBreakingAction で保存し、tryBreakBlock で参照する（3x3モード用）。
     */
    @Unique
    private int stapiultimine_lastFace = 0;

    /**
     * tryBreakBlock() の内部処理を分解し、Config に従った制御を行う。
     * - consumeDurability=false の場合、postMine() をスキップして耐久を消費しない
     * - teleportDrops は呼び出し元ループ側でフラグ制御するため、ここでは afterBreak() を呼ぶのみ
     */
    @Unique
    private void stapiultimine_breakBlock(int x, int y, int z) {
        int blockId = this.world.getBlockId(x, y, z);
        if (blockId <= 0) return;
        Block block = Block.BLOCKS[blockId];
        if (block == null) return;
        int meta = this.world.getBlockMeta(x, y, z);

        // 破壊パーティクルと効果音をクライアントに送信する
        this.world.worldEvent(this.player, 2001, x, y, z, blockId + meta * 256);

        // ブロックを消去してメタデータ変更通知を送る（BlockUpdateS2CPacketも内部で送信される）
        boolean removed = this.finishMining(x, y, z);

        if (removed) {
            // ドロップを生成する（teleportDropsフラグはItemEntityMixin側で参照される）
            if (ConfigInit.ADVANCED.teleportDrops) {
                // シングルプレイと同じくプレイヤーの足元に直接生成することで、テレポートの同期ズレを解消する
                block.afterBreak(this.world, this.player, (int) Math.floor(this.player.x), (int) Math.floor(this.player.y), (int) Math.floor(this.player.z), meta);
            } else {
                block.afterBreak(this.world, this.player, x, y, z, meta);
            }
        }

        // consumeDurability 設定に従ってツール耐久消費を制御する
        if (ConfigInit.GENERAL.consumeDurability) {
            ItemStack held = this.player.getHand();
            if (held != null) {
                held.postMine(blockId, x, y, z, this.player);
                if (held.count <= 0) {
                    held.onRemoved(this.player);
                    this.player.clearStackInHand();
                }
            }
        }
    }

    // プレイヤーが叩いたブロックの面方向（direction）を記録しておく
    @Inject(method = "onBlockBreakingAction", at = @At("HEAD"))
    private void captureDirection(int x, int y, int z, int direction, CallbackInfo ci) {
        stapiultimine_lastFace = direction;
    }

    // プレイヤーの Ultimine が有効な場合、追加ブロックを一括破壊する
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
        if (!ConfigInit.ADVANCED.forceVeinMine && !this.player.canHarvest(block)) return;

        // プレイヤーごとに管理されているモードを取得する（クライアントのstatic変数は参照しない）
        VeinMineMode mode = PlayerStateManager.getMode(this.player);

        // 現在のモードに応じたブロックセットを計算する
        Set<BlockPos> targets = mode.getShape().getBlocks(
                this.world, this.player, x, y, z, block, meta, stapiultimine_lastFace
        );

        stapiultimine_isVeinMining = true;
        stapiultimine_isStartingVeinMine = true;

        // teleportDrops が有効な場合は ItemEntityMixin が参照するフラグを立てる
        if (ConfigInit.ADVANCED.teleportDrops) {
            VeinMinerUtil.isTeleportingDrops = true;
            VeinMinerUtil.currentPlayer = this.player;
            VeinMinerUtil.originBlockPos = new BlockPos(x, y, z);
        }

        try {
            int count = 0;

            for (BlockPos pos : targets) {
                // 設定の上限に達したら中断する（形状側でも制限されているが安全のため二重チェックする）
                if (count >= ConfigInit.GENERAL.maxBlocks) break;

                // 起点ブロックはバニラの tryBreakBlock がそのまま処理するためスキップする
                if (pos.getX() == x && pos.getY() == y && pos.getZ() == z) continue;

                stapiultimine_breakBlock(pos.getX(), pos.getY(), pos.getZ());
                count++;

                // ツールが破壊されたら追加破壊を中断する
                if (this.player.getHand() == null) break;
            }
        } catch (Exception e) {
            // 例外が発生した場合はここでフラグをリセットし、上位に投げる
            stapiultimine_isVeinMining = false;
            stapiultimine_isStartingVeinMine = false;
            VeinMinerUtil.resetTeleportStatus();
            throw e;
        }
    }

    // ブロック破壊処理が終わった後にフラグをリセットする
    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void afterTryBreakBlock(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (stapiultimine_isStartingVeinMine) {
            stapiultimine_isVeinMining = false;
            stapiultimine_isStartingVeinMine = false;
            VeinMinerUtil.resetTeleportStatus();
        }
    }
}
