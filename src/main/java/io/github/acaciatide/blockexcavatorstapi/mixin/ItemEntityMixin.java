package io.github.acaciatide.blockexcavatorstapi.mixin;

import io.github.acaciatide.blockexcavatorstapi.util.VeinMinerUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void onInit(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        // 一括破壊の処理中でない場合は即時リターンし、平常時のオーバーヘッドをゼロにする
        if (!VeinMinerUtil.isTeleportingDrops && VeinMinerUtil.originBlockPos == null) {
            return;
        }

        boolean isOrigin = false;
        if (VeinMinerUtil.originBlockPos != null) {
            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            if (ix == VeinMinerUtil.originBlockPos.getX() && iy == VeinMinerUtil.originBlockPos.getY() && iz == VeinMinerUtil.originBlockPos.getZ()) {
                isOrigin = true;
            }
        }

        if ((VeinMinerUtil.isTeleportingDrops || isOrigin) && VeinMinerUtil.currentPlayer != null) {
            ItemEntity entity = (ItemEntity) (Object) this;
            
            // 速度（散らばり）をリセットする
            entity.velocityX = 0.0D;
            entity.velocityY = 0.0D;
            entity.velocityZ = 0.0D;
            
            // 座標をプレイヤーの正確な位置にリセットする（ランダムなズレを上書きする）
            // プレイヤーの当たり判定の底辺（厳密な足元の床座標）を取得し、ItemEntityの沈み込みオフセット（+0.125D）を加味して床に密着させる
            double exactGroundY = VeinMinerUtil.currentPlayer.boundingBox.minY + 0.125D;
            entity.setPosition(VeinMinerUtil.currentPlayer.x, exactGroundY, VeinMinerUtil.currentPlayer.z);
        }
    }
}
