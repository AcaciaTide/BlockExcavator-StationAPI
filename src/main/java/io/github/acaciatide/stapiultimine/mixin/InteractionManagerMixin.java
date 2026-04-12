package io.github.acaciatide.stapiultimine.mixin;

import io.github.acaciatide.stapiultimine.events.init.ClientInitListener;
import io.github.acaciatide.stapiultimine.util.VeinMinerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InteractionManager.class)
public class InteractionManagerMixin {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Inject(method = "breakBlock", at = @At("HEAD"))
    public void onBreakBlock(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
        // キーが押されている場合のみ一括破壊を試行する
        if (ClientInitListener.isUltimineKeyPressed()) {
            World world = this.minecraft.world;
            int blockId = world.getBlockId(x, y, z);
            if (blockId > 0) {
                int meta = world.getBlockMeta(x, y, z);
                Block block = Block.BLOCKS[blockId];
                // 実際にブロックが破壊される前に、その周囲の同種ブロックを破壊するロジックを呼び出す
                VeinMinerUtil.mineVein(world, this.minecraft.player, x, y, z, block, meta);
            }
        }
    }
}
