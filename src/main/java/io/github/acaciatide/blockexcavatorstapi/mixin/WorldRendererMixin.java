package io.github.acaciatide.blockexcavatorstapi.mixin;

import io.github.acaciatide.blockexcavatorstapi.util.ExcavatorRenderCache;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    private World world;

    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    public void onRenderBlockOutline(PlayerEntity player, HitResult hitResult, int i, ItemStack handStack, float tickDelta, CallbackInfo ci) {
        
        // まず現在の視点に合わせてキャッシュを更新させる
        ExcavatorRenderCache.updateCache(this.world, player, hitResult);

        // 事前に計算済みの「シルエットのアウトライン（不要な内部線を省いたもの）」を取得
        List<ExcavatorRenderCache.LineSegment> lines = ExcavatorRenderCache.cachedLines;

        if (lines.isEmpty()) return;

        // OpenGLの設定: 透視状態にして線を描画するための準備
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(io.github.acaciatide.blockexcavatorstapi.config.ConfigInit.ADVANCED.outlineColorRed / 255.0f, io.github.acaciatide.blockexcavatorstapi.config.ConfigInit.ADVANCED.outlineColorGreen / 255.0f, io.github.acaciatide.blockexcavatorstapi.config.ConfigInit.ADVANCED.outlineColorBlue / 255.0f, io.github.acaciatide.blockexcavatorstapi.config.ConfigInit.ADVANCED.outlineColorAlpha / 255.0f);
        GL11.glLineWidth(io.github.acaciatide.blockexcavatorstapi.config.ConfigInit.ADVANCED.outlineThickness);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // 壁越しでも見えるように深度テストを無効化

        // プレイヤーの移動幅（1フレーム前からのズレ）を基にしたカメラの補正値
        double offsetX = player.lastTickX + (player.x - player.lastTickX) * (double)tickDelta;
        double offsetY = player.lastTickY + (player.y - player.lastTickY) * (double)tickDelta;
        double offsetZ = player.lastTickZ + (player.z - player.lastTickZ) * (double)tickDelta;

        // すべての線を「GL_LINES」モードで一括処理する
        Tessellator tessellator = Tessellator.INSTANCE;
        tessellator.start(1); 
        
        // Iteratorオブジェクトの新規生成を防ぐため、インデックスループで描画データを追加する
        int size = lines.size();
        for (int k = 0; k < size; k++) {
            ExcavatorRenderCache.LineSegment line = lines.get(k);
            tessellator.vertex(line.x1 - offsetX, line.y1 - offsetY, line.z1 - offsetZ);
            tessellator.vertex(line.x2 - offsetX, line.y2 - offsetY, line.z2 - offsetZ);
        }
        
        tessellator.draw();

        // OpenGLの設定を元に戻す
        GL11.glEnable(GL11.GL_DEPTH_TEST); // 深度テストを再度有効化
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
