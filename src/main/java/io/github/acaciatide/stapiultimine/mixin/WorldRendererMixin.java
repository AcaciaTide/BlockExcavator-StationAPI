package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.util.UltimineRenderCache;
import io.github.acaciatide.stapiultimine.util.VeinMinerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    private World world;

    @Shadow
    protected abstract void renderOutline(Box box);

    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    public void onRenderBlockOutline(PlayerEntity player, HitResult hitResult, int i, ItemStack handStack, float tickDelta, CallbackInfo ci) {
        // 一括破壊対象の座標セットをキャッシュ経由で取得
        Set<VeinMinerUtil.BlockPos> targets = UltimineRenderCache.getCachedBlocks(this.world, hitResult);

        if (targets.isEmpty()) return;

        // OpenGLの設定: 白色・半透明のアウトライン描画に向けた設定を行う
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.4F); // 白色 (RGBA)
        GL11.glLineWidth(2.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        // プレイヤーの移動に合わせたカメラ位置の補正計算
        double offsetX = player.lastTickX + (player.x - player.lastTickX) * (double)tickDelta;
        double offsetY = player.lastTickY + (player.y - player.lastTickY) * (double)tickDelta;
        double offsetZ = player.lastTickZ + (player.z - player.lastTickZ) * (double)tickDelta;

        float expansion = 0.002F;

        for (VeinMinerUtil.BlockPos pos : targets) {
            int blockId = this.world.getBlockId(pos.x, pos.y, pos.z);
            if (blockId > 0) {
                Block block = Block.BLOCKS[blockId];
                // ブロックごとの形状（当たり判定）に合わせて枠を更新
                block.updateBoundingBox(this.world, pos.x, pos.y, pos.z);
                
                // 描画用のボックスを構築し、カメラ位置分だけオフセットさせる
                Box box = block.getBoundingBox(this.world, pos.x, pos.y, pos.z)
                        .expand(expansion, expansion, expansion)
                        .offset(-offsetX, -offsetY, -offsetZ);
                
                // バニラの描画メソッドを呼び出して白い線を描く
                this.renderOutline(box);
            }
        }

        // OpenGLの設定を元に戻す
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
