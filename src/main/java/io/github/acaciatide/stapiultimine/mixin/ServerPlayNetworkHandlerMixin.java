package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.server.PlayerStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    private ServerPlayerEntity player;

    @Unique
    private boolean stapiultimine_disconnected = false;

    @Inject(method = "disconnect(Ljava/lang/String;)V", at = @At("HEAD"))
    private void onDisconnectKick(String reason, CallbackInfo ci) {
        if (!stapiultimine_disconnected && this.player != null) {
            PlayerStateManager.onPlayerDisconnect(this.player);
            stapiultimine_disconnected = true;
        }
    }

    @Inject(method = "onDisconnected(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"))
    private void onDisconnected(String reason, Object[] objects, CallbackInfo ci) {
        if (!stapiultimine_disconnected && this.player != null) {
            PlayerStateManager.onPlayerDisconnect(this.player);
            stapiultimine_disconnected = true;
        }
    }
}
