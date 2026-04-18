package io.github.acaciatide.stapiultimine.network;

import io.github.acaciatide.stapiultimine.server.PlayerStateManager;
import io.github.acaciatide.stapiultimine.util.VeinMineMode;
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
 * クライアント→サーバーへUltimineのモード変更を通知するパケット。
 * サーバーバウンドのみ (clientBound=false, serverBound=true)。
 * サイズ: 4bytes (int)
 */
public class ModeSwitchPacket extends Packet implements ManagedPacket<ModeSwitchPacket> {

    // クライアント→サーバーへの一方向送信パケットとして定義する
    public static final PacketType<ModeSwitchPacket> TYPE =
            PacketType.builder(false, true, ModeSwitchPacket::new).build();

    /** VeinMineMode.ordinal() の値 */
    public int modeOrdinal;

    /** デシリアライズ用の引数なしコンストラクタ */
    public ModeSwitchPacket() {}

    /** 送信用コンストラクタ */
    public ModeSwitchPacket(int modeOrdinal) {
        this.modeOrdinal = modeOrdinal;
    }

    @Override
    public void read(DataInputStream stream) {
        try {
            modeOrdinal = stream.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(DataOutputStream stream) {
        try {
            stream.writeInt(modeOrdinal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(NetworkHandler networkHandler) {
        // ordinalが有効範囲内の場合のみサーバー側でモードを更新する
        VeinMineMode[] modes = VeinMineMode.values();
        if (modeOrdinal >= 0 && modeOrdinal < modes.length) {
            PlayerStateManager.setMode(
                    PlayerHelper.getPlayerFromPacketHandler(networkHandler),
                    modes[modeOrdinal]
            );
        }
    }

    @Override
    public int size() {
        // int は 4bytes
        return 4;
    }

    @Override
    public @NotNull PacketType<ModeSwitchPacket> getType() {
        return TYPE;
    }
}
