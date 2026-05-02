package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.config.ConfigInit;
import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import io.github.acaciatide.stapiultimine.util.UltimineRenderCache;
import io.github.acaciatide.stapiultimine.util.VeinMinerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow
    private Minecraft minecraft;

    @Inject(method = "render(FZII)V", at = @At("TAIL"))
    private void renderHUD(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        if (ClientInitListener.isUltimineKeyPressed() && ConfigInit.GENERAL.displayHudStatus) {
            int offsetX = (ConfigInit.GENERAL.hudOffsetX != null) ? ConfigInit.GENERAL.hudOffsetX : 0;
            int offsetY = (ConfigInit.GENERAL.hudOffsetY != null) ? ConfigInit.GENERAL.hudOffsetY : 0;
            
            int baseX = 5 + offsetX;
            int baseY = 15 + offsetY;

            if (UltimineRenderCache.cachedBlockCount > 0) {
                // 有効時: 白色でステータスとモードを表示
                this.minecraft.textRenderer.drawWithShadow("StAPIUltimine: Enabled", baseX, baseY, 0xFFFFFF);
                this.minecraft.textRenderer.drawWithShadow("Mode: " + VeinMinerUtil.currentMode.getName() + " (" + UltimineRenderCache.cachedBlockCount + ")", baseX, baseY + 10, 0xFFFFFF);
            } else {
                // 無効時: 灰色でDisabledのみ表示
                this.minecraft.textRenderer.drawWithShadow("StAPIUltimine: Disabled", baseX, baseY, 0xAAAAAA);
            }
            
            // スニーク（シフト等）を押している時にだけ操作案内を追加表示
            if (this.minecraft.options != null && org.lwjgl.input.Keyboard.isKeyDown(this.minecraft.options.sneakKey.code)) {
                this.minecraft.textRenderer.drawWithShadow("Mouse wheel up: " + VeinMinerUtil.getNextModeName(), baseX, baseY + 20, 0xFFFFAA);
                this.minecraft.textRenderer.drawWithShadow("Mouse wheel down: " + VeinMinerUtil.getPrevModeName(), baseX, baseY + 30, 0xFFFFAA);
            }
        }
    }
}
