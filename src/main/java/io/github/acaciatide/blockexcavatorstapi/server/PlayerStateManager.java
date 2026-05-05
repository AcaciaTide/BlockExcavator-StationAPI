package io.github.acaciatide.blockexcavatorstapi.server;

import io.github.acaciatide.blockexcavatorstapi.util.VeinMineMode;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * サーバーサイドでプレイヤーごとのExcavator状態を管理するシングルトンクラス。
 * プレイヤー名をキーに使用する。
 */
public class PlayerStateManager {

    // プレイヤー名 → Excavatorキー押下状態 のマップ
    private static final Map<String, Boolean> keyStates = new HashMap<>();

    // プレイヤー名 → 現在のVeinMineMode のマップ
    private static final Map<String, VeinMineMode> modes = new HashMap<>();

    /**
     * プレイヤーのExcavatorキーが押されているかを返す。
     * 未登録のプレイヤーはfalseとして扱う。
     */
    public static boolean isExcavatorActive(PlayerEntity player) {
        if (player == null) return false;
        return keyStates.getOrDefault(player.name, false);
    }

    /**
     * プレイヤーの現在のVeinMineModeを返す。
     * 未登録のプレイヤーはデフォルト値(SHAPELESS)として扱う。
     */
    public static VeinMineMode getMode(PlayerEntity player) {
        if (player == null) return VeinMineMode.SHAPELESS;
        return modes.getOrDefault(player.name, VeinMineMode.SHAPELESS);
    }

    /** プレイヤーのキー状態を更新する */
    public static void setKeyState(PlayerEntity player, boolean pressed) {
        if (player == null) return;
        keyStates.put(player.name, pressed);
    }

    /** プレイヤーのモードを更新する */
    public static void setMode(PlayerEntity player, VeinMineMode mode) {
        if (player == null) return;
        modes.put(player.name, mode);
    }

    /**
     * プレイヤーが切断した際に状態をクリアする。
     * メモリリークを防ぐために必ず呼び出す必要がある。
     */
    public static void onPlayerDisconnect(PlayerEntity player) {
        if (player == null) return;
        keyStates.remove(player.name);
        modes.remove(player.name);
    }
}
