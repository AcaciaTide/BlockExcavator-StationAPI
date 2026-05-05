package io.github.acaciatide.blockexcavatorstapi.mixin;

import io.github.acaciatide.blockexcavatorstapi.events.init.ClientInitListener;
import io.github.acaciatide.blockexcavatorstapi.network.ModeSwitchPacket;
import io.github.acaciatide.blockexcavatorstapi.util.VeinMinerUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    /**
     * ホットバーのスクロール処理をフックし、一括破壊キー＋スニークキーが押されている場合は
     * モードの切り替えを行い、バニラのスクロールをキャンセルする。
     */
    @Inject(method = "scrollInHotbar(I)V", at = @At("HEAD"), cancellable = true)
    private void onScrollInHotbar(int direction, CallbackInfo ci) {
        // クライアント側のみの処理
        if (ClientInitListener.isExcavatorKeyPressed()) {
            Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
            if (minecraft != null && minecraft.options != null && Keyboard.isKeyDown(minecraft.options.sneakKey.code)) {
                // モードを循環させる（上スクロールなら次へ、下スクロールなら前へ）
                // directionは正負が逆転している可能性があるため、そのまま渡してcycleMode側でハンドルする
                VeinMinerUtil.cycleMode(direction);

                // マルチプレイ時はサーバーにもモード変更を通知する
                if (minecraft.isWorldRemote()) {
                    PacketHelper.send(new ModeSwitchPacket(VeinMinerUtil.currentMode.ordinal()));
                }

                // バニラのホットバースロット切り替えを防止する
                ci.cancel();
            }
        }
    }
}
