package io.github.acaciatide.blockexcavatorstapi.mixin;

import io.github.acaciatide.blockexcavatorstapi.config.ConfigInit;
import io.github.acaciatide.blockexcavatorstapi.events.init.ClientInitListener;
import io.github.acaciatide.blockexcavatorstapi.util.ExcavatorRenderCache;
import io.github.acaciatide.blockexcavatorstapi.util.VeinMinerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow
    private Minecraft minecraft;

    // 文字列のキャッシュと状態監視用
    @Unique private String blockexcavatorstapi_cachedModeText = "";
    @Unique private String blockexcavatorstapi_cachedUpText = "";
    @Unique private String blockexcavatorstapi_cachedDownText = "";
    @Unique private io.github.acaciatide.blockexcavatorstapi.util.VeinMineMode blockexcavatorstapi_lastMode = null;
    @Unique private int blockexcavatorstapi_lastBlockCount = -1;

    @Inject(method = "render(FZII)V", at = @At("TAIL"))
    private void renderHUD(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        if (ClientInitListener.isExcavatorKeyPressed() && ConfigInit.GENERAL.displayHudStatus) {
            int offsetX = (ConfigInit.ADVANCED.hudOffsetX != null) ? ConfigInit.ADVANCED.hudOffsetX : 0;
            int offsetY = (ConfigInit.ADVANCED.hudOffsetY != null) ? ConfigInit.ADVANCED.hudOffsetY : 0;
            
            int baseX = 5 + offsetX;
            int baseY = 15 + offsetY;

            int cachedCount = ExcavatorRenderCache.cachedBlockCount;
            io.github.acaciatide.blockexcavatorstapi.util.VeinMineMode currentMode = VeinMinerUtil.currentMode;

            // キャッシュの更新判定
            if (currentMode != blockexcavatorstapi_lastMode) {
                blockexcavatorstapi_lastMode = currentMode;
                blockexcavatorstapi_cachedUpText = "Mouse wheel up: " + VeinMinerUtil.getNextModeName();
                blockexcavatorstapi_cachedDownText = "Mouse wheel down: " + VeinMinerUtil.getPrevModeName();
                // 有効（描画される）状態のときのみ、モード用テキストも生成する
                if (cachedCount > 0) {
                    blockexcavatorstapi_cachedModeText = "Mode: " + currentMode.getName() + " (" + cachedCount + ")";
                    blockexcavatorstapi_lastBlockCount = cachedCount;
                }
            } else if (cachedCount > 0 && cachedCount != blockexcavatorstapi_lastBlockCount) {
                // 有効状態かつブロック数が変わったときのみモード用テキストを生成
                blockexcavatorstapi_lastBlockCount = cachedCount;
                blockexcavatorstapi_cachedModeText = "Mode: " + currentMode.getName() + " (" + cachedCount + ")";
            }

            if (cachedCount > 0) {
                // 有効時: 白色でステータスとモードを表示
                this.minecraft.textRenderer.drawWithShadow("BlockExcavatorStAPI: Enabled", baseX, baseY, 0xFFFFFF);
                this.minecraft.textRenderer.drawWithShadow(blockexcavatorstapi_cachedModeText, baseX, baseY + 10, 0xFFFFFF);
            } else {
                // 無効時: 灰色でDisabledのみ表示
                this.minecraft.textRenderer.drawWithShadow("BlockExcavatorStAPI: Disabled", baseX, baseY, 0xAAAAAA);
            }
            
            // スニーク（シフト等）を押している時にだけ操作案内を追加表示
            if (this.minecraft.options != null && org.lwjgl.input.Keyboard.isKeyDown(this.minecraft.options.sneakKey.code)) {
                this.minecraft.textRenderer.drawWithShadow(blockexcavatorstapi_cachedUpText, baseX, baseY + 20, 0xFFFFAA);
                this.minecraft.textRenderer.drawWithShadow(blockexcavatorstapi_cachedDownText, baseX, baseY + 30, 0xFFFFAA);
            }
        }
    }
}
