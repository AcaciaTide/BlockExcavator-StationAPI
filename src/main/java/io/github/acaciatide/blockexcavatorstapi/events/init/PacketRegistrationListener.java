package io.github.acaciatide.blockexcavatorstapi.events.init;

import io.github.acaciatide.blockexcavatorstapi.network.KeyStatePacket;
import io.github.acaciatide.blockexcavatorstapi.network.ModeSwitchPacket;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.network.packet.PacketRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.registry.PacketTypeRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Namespace;

import java.lang.invoke.MethodHandles;

/**
 * パケットタイプをPacketTypeRegistryに登録するリスナー。
 * fabric.mod.json の stationapi:event_bus に登録して使用する。
 */
public class PacketRegistrationListener {

    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Namespace NAMESPACE = Namespace.resolve();

    @EventListener
    public void registerPackets(PacketRegisterEvent event) {
        // キー状態パケットをIDで登録する (C→S)
        Registry.register(PacketTypeRegistry.INSTANCE, NAMESPACE.id("excavator_key_state"), KeyStatePacket.TYPE);
        // モード切替パケットをIDで登録する (C→S)
        Registry.register(PacketTypeRegistry.INSTANCE, NAMESPACE.id("excavator_mode_switch"), ModeSwitchPacket.TYPE);
    }
}
