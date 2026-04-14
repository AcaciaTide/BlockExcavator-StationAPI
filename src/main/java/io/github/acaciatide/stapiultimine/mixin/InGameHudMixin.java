package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import io.github.acaciatide.stapiultimine.util.UltimineRenderCache;
import io.github.acaciatide.stapiultimine.config.ConfigInit;
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
        if (ClientInitListener.isUltimineKeyPressed() && ConfigInit.CONFIG.displayHudStatus) {
            if (UltimineRenderCache.cachedBlockCount > 0) {
                // 有効時: 白色でステータスとモードを表示
                this.minecraft.textRenderer.drawWithShadow("StAPIUltimine: Enabled", 5, 5, 0xFFFFFF);
                this.minecraft.textRenderer.drawWithShadow("Mode: Shapeless (" + UltimineRenderCache.cachedBlockCount + ")", 5, 15, 0xFFFFFF);
            } else {
                // 無効時: 灰色でDisabledのみ表示
                this.minecraft.textRenderer.drawWithShadow("StAPIUltimine: Disabled", 5, 5, 0xAAAAAA);
            }
        }
    }
}
