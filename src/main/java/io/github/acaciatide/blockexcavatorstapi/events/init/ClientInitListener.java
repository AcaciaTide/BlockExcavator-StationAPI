package io.github.acaciatide.blockexcavatorstapi.events.init;

import io.github.acaciatide.blockexcavatorstapi.network.KeyStatePacket;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import org.lwjgl.input.Keyboard;

public class ClientInitListener {

    // 一括破壊モードのON/OFFを切り替えるキーバインド
    public static KeyBinding excavatorKey;

    @EventListener
    public void registerKeyBindings(KeyBindingRegisterEvent event) {
        // キーバインドの登録 ('LeftCTRL'キーをデフォルトに設定)
        excavatorKey = new KeyBinding("key.blockexcavatorstapi.excavator", Keyboard.KEY_LCONTROL       );
        event.keyBindings.add(excavatorKey);
    }
    
    /**
     * Excavatorキーの押下/解放イベントを検知し、マルチプレイ時にサーバーへ通知する。
     * StationAPIがキー状態変化の瞬間にこのイベントを発火する。
     */
    @EventListener
    public void onKeyStateChanged(KeyStateChangedEvent event) {
        if (excavatorKey == null) return;
        // 変化したキーがExcavatorキーでなければ無視する
        if (Keyboard.getEventKey() != excavatorKey.code) return;
        // GUI(チャット画面等）を開いている時はゲーム内の操作とみなさないため無視する
        if (event.environment != KeyStateChangedEvent.Environment.IN_GAME) return;

        // Keyboard.getEventKeyState()でイベントの状態を取得する（true=押下, false=解放）
        boolean pressed = Keyboard.getEventKeyState();

        // マルチプレイ時のみサーバーにキー状態を通知する
        Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
        if (minecraft != null && minecraft.isWorldRemote()) {
            PacketHelper.send(new KeyStatePacket(pressed));
        }
    }

    // 一括破壊キーが押されているか判定するメソッド
    public static boolean isExcavatorKeyPressed() {
        if (excavatorKey == null) {
            return false;
        }
        return Keyboard.isKeyDown(excavatorKey.code);
    }
}
