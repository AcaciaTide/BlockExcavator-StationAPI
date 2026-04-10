package io.github.acaciatide.stapiultimine.events.init;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

public class ClientInitListener {

    // 一括破壊モードのON/OFFを切り替えるキーバインド
    public static KeyBinding ultimineKey;

    @EventListener
    public void registerKeyBindings(KeyBindingRegisterEvent event) {
        // キーバインドの登録 ('V'キーをデフォルトに設定)
        ultimineKey = new KeyBinding("key.stapiultimine.ultimine", Keyboard.KEY_V);
        event.keyBindings.add(ultimineKey);
    }
    
    // 一括破壊キーが押されているか判定するメソッド
    public static boolean isUltimineKeyPressed() {
        if (ultimineKey == null) {
            return false;
        }
        return Keyboard.isKeyDown(ultimineKey.code);
    }
}
