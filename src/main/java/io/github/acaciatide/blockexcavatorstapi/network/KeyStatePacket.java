package io.github.acaciatide.blockexcavatorstapi.network;

import io.github.acaciatide.blockexcavatorstapi.server.PlayerStateManager;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.packet.Packet;
import net.modificationstation.stationapi.api.entity.player.PlayerHelper;
import net.modificationstation.stationapi.api.network.packet.ManagedPacket;
import net.modificationstation.stationapi.api.network.packet.PacketType;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * クライアント→サーバーへExcavatorキーの押下/解放状態を通知するパケット。
 * サーバーバウンドのみ (clientBound=false, serverBound=true)。
 * サイズ: 1byte (boolean)
 */
public class KeyStatePacket extends Packet implements ManagedPacket<KeyStatePacket> {

    // クライアント→サーバーへの一方向送信パケットとして定義する
    public static final PacketType<KeyStatePacket> TYPE =
            PacketType.builder(false, true, KeyStatePacket::new).build();

    /** true=押下, false=解放 */
    public boolean pressed;

    /** デシリアライズ用の引数なしコンストラクタ */
    public KeyStatePacket() {}

    /** 送信用コンストラクタ */
    public KeyStatePacket(boolean pressed) {
        this.pressed = pressed;
    }

    @Override
    public void read(DataInputStream stream) {
        try {
            pressed = stream.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(DataOutputStream stream) {
        try {
            stream.writeBoolean(pressed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(NetworkHandler networkHandler) {
        // サーバー側でプレイヤーのキー状態を更新する
        PlayerStateManager.setKeyState(
                PlayerHelper.getPlayerFromPacketHandler(networkHandler),
                pressed
        );
    }

    @Override
    public int size() {
        // boolean は 1byte
        return 1;
    }

    @Override
    public @NotNull PacketType<KeyStatePacket> getType() {
        return TYPE;
    }
}
