package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import io.github.acaciatide.stapiultimine.util.VeinMinerUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "afterBreak", at = @At("HEAD"))
    public void onAfterBreak(World world, PlayerEntity playerEntity, int x, int y, int z, int meta, CallbackInfo ci) {
        // シングルプレイMVP前提: 直接クライアントのキー押下状態を参照する
        if (ClientInitListener.isUltimineKeyPressed()) {
            VeinMinerUtil.mineVein(world, playerEntity, x, y, z, (Block) (Object) this, meta);
        }
    }
}
