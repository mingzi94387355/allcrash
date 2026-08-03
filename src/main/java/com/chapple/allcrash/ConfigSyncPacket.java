package com.chapple.allcrash;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigSyncPacket {
    private final List<String> crashModList;

    public ConfigSyncPacket(List<String> crashModList) {
        this.crashModList = crashModList;
    }

    public static void encode(ConfigSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.crashModList.size());
        for (String modId : packet.crashModList) {
            buf.writeUtf(modId);
        }
    }

    public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return new ConfigSyncPacket(list);
    }

    public static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        //注意：此操作在 Netty 线程执行，但 ModList.get() 和日志是线程安全的
        AllCrash.LOGGER.info("Received ConfigSyncPacket on client side!");

        List<String> serverList = packet.crashModList;
        for (String modId : serverList) {
            if (ModList.get().isLoaded(modId)) {
                throw new RuntimeException("Never gonna give you up, never gonna let you down, Never gonna run around and desert you.(" + modId + " is loaded)");
            }
        }

        AllCrash.LOGGER.info("Client verification passed, sending ack.");
        //确认
        AllCrash.CHANNEL.sendToServer(new AckPacket());

        context.setPacketHandled(true);
    }
}