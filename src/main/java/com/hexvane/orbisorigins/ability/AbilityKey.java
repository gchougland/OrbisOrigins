package com.hexvane.orbisorigins.ability;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crouch + the game mode key (O) toggles racial abilities. The client sends that key as the chat
 * command "/gm c" or "/gm a"; while the player crouches it is caught here, otherwise it passes on
 * untouched so operators still switch game mode with O.
 */
public final class AbilityKey implements AutoCloseable {
    private static final Set<String> KEY_COMMANDS = Set.of("/gm c", "/gm a");
    private final Set<UUID> crouching = ConcurrentHashMap.newKeySet();
    private final PacketFilter filter;

    public AbilityKey() {
        filter = PacketAdapters.registerInbound((PlayerPacketFilter) this::intercept);
    }

    /** Runs on the network thread, where the store cannot be read, so crouching is followed from movement packets. */
    private boolean intercept(PlayerRef player, Packet packet) {
        if (packet instanceof ClientMovement movement) {
            if (movement.movementStates != null) {
                if (movement.movementStates.crouching) crouching.add(player.getUuid());
                else crouching.remove(player.getUuid());
            }
            return false;
        }
        if (!(packet instanceof ChatMessage chat) || chat.message == null || !KEY_COMMANDS.contains(chat.message)) return false;
        if (!crouching.contains(player.getUuid())) return false;
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) return true;
        var store = ref.getStore();
        var world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (ref.isValid()) player.sendMessage(AbilityToggle.set(ref, store, player, world, null));
        });
        return true;
    }

    @Override
    public void close() {
        PacketAdapters.deregisterInbound(filter);
    }
}
