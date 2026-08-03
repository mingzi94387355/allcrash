package com.chapple.allcrash;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

public class AckPacket {
    public AckPacket() {}

    public static void encode(AckPacket packet, FriendlyByteBuf buf) {

    }

    public static AckPacket decode(FriendlyByteBuf buf) {
        return new AckPacket();
    }

    public static void handle(AckPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                UUID uuid = player.getUUID();
                ScheduledFuture<?> future = AllCrash.pendingPlayers.remove(uuid);
                if (future != null) {
                    future.cancel(false);
                    AllCrash.LOGGER.info("Player {} verified successfully.", player.getName().getString());
                }
            }
        });
        context.setPacketHandled(true);
    }
}